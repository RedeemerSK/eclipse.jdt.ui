/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
// AW
package org.eclipse.jdt.internal.ui.preferences;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
	
/*
 * The page for defaults for classpath entries in new java projects.
 * See PreferenceConstants to access or change these values through public API.
 */
public class NewJavaProjectPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String SRCBIN_FOLDERS_IN_NEWPROJ= PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ;
	private static final String SRCBIN_SRCNAME= PreferenceConstants.SRCBIN_SRCNAME;
	private static final String SRCBIN_BINNAME= PreferenceConstants.SRCBIN_BINNAME;

	private static final String CLASSPATH_JRELIBRARY_INDEX= PreferenceConstants.NEWPROJECT_JRELIBRARY_INDEX;
	private static final String CLASSPATH_JRELIBRARY_LIST= PreferenceConstants.NEWPROJECT_JRELIBRARY_LIST;


	/**
	 * @deprecated Inline to avoid reference to preference page
	 */
	public static boolean useSrcAndBinFolders() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ);
	}

	/**
	 * @deprecated Inline to avoid reference to preference page
	 */	
	public static String getSourceFolderName() {
		return PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_SRCNAME);
	}

	/**
	 * @deprecated Inline to avoid reference to preference page
	 */	
	public static String getOutputLocationName() {
		return PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME);
	}

	public static IClasspathEntry[] getDefaultJRELibrary() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		
		String str= store.getString(CLASSPATH_JRELIBRARY_LIST);
		int index= store.getInt(CLASSPATH_JRELIBRARY_INDEX);
		
		StringTokenizer tok= new StringTokenizer(str, ";"); //$NON-NLS-1$
		while (tok.hasMoreTokens() && index > 0) {
			tok.nextToken();
			index--;
		}
		
		if (tok.hasMoreTokens()) {
			IClasspathEntry[] res= decodeJRELibraryClasspathEntries(tok.nextToken());
			if (res.length > 0) {
				return res;
			}
		}
		return new IClasspathEntry[] { JavaRuntime.getJREVariableEntry() };	
	}			
	
	// JRE Entry
	
	public static String decodeJRELibraryDescription(String encoded) {
		int end= encoded.indexOf(' ');
		if (end != -1) {
			return URLDecoder.decode(encoded.substring(0, end));
		}
		return ""; //$NON-NLS-1$
	}
	
	
	public static IClasspathEntry[] decodeJRELibraryClasspathEntries(String encoded) {
		StringTokenizer tok= new StringTokenizer(encoded, " "); //$NON-NLS-1$
		ArrayList res= new ArrayList();
		while (tok.hasMoreTokens()) {
			try {
				tok.nextToken(); // desc: ignore
				int kind= Integer.parseInt(tok.nextToken());
				IPath path= decodePath(tok.nextToken());
				IPath attachPath= decodePath(tok.nextToken());
				IPath attachRoot= decodePath(tok.nextToken());
				boolean isExported= Boolean.valueOf(tok.nextToken()).booleanValue();
				switch (kind) {
					case IClasspathEntry.CPE_SOURCE:
						res.add(JavaCore.newSourceEntry(path));
						break;
					case IClasspathEntry.CPE_LIBRARY:
						res.add(JavaCore.newLibraryEntry(path, attachPath, attachRoot, isExported));
						break;
					case IClasspathEntry.CPE_VARIABLE:
						res.add(JavaCore.newVariableEntry(path, attachPath, attachRoot, isExported));
						break;
					case IClasspathEntry.CPE_PROJECT:
						res.add(JavaCore.newProjectEntry(path, isExported));
						break;
					case IClasspathEntry.CPE_CONTAINER:
						res.add(JavaCore.newContainerEntry(path, isExported));
						break;
				}								
			} catch (NumberFormatException e) {
				String message= JavaUIMessages.getString("NewJavaProjectPreferencePage.error.decode"); //$NON-NLS-1$
				JavaPlugin.log(new Status(Status.ERROR, JavaUI.ID_PLUGIN, Status.ERROR, message, e));
			} catch (NoSuchElementException e) {
				String message= JavaUIMessages.getString("NewJavaProjectPreferencePage.error.decode"); //$NON-NLS-1$
				JavaPlugin.log(new Status(Status.ERROR, JavaUI.ID_PLUGIN, Status.ERROR, message, e));
			}
		}
		return (IClasspathEntry[]) res.toArray(new IClasspathEntry[res.size()]);	
	}
	
	
	public static String encodeJRELibrary(String desc, IClasspathEntry[] cpentries) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < cpentries.length; i++) {
			IClasspathEntry entry= cpentries[i];
			buf.append(URLEncoder.encode(desc));
			buf.append(' ');
			buf.append(entry.getEntryKind());
			buf.append(' ');
			buf.append(encodePath(entry.getPath()));
			buf.append(' ');
			buf.append(encodePath(entry.getSourceAttachmentPath()));
			buf.append(' ');
			buf.append(encodePath(entry.getSourceAttachmentRootPath()));
			buf.append(' ');
			buf.append(entry.isExported());
			buf.append(' ');
		}
		return buf.toString();
	}
	
	private static String encodePath(IPath path) {
		if (path == null) {
			return "#"; //$NON-NLS-1$
		} else if (path.isEmpty()) {
			return "&"; //$NON-NLS-1$
		} else {
			return URLEncoder.encode(path.toString());
		}
	}
	
	private static IPath decodePath(String str) {
		if ("#".equals(str)) { //$NON-NLS-1$
			return null;
		} else if ("&".equals(str)) { //$NON-NLS-1$
			return Path.EMPTY;
		} else {
			return new Path(URLDecoder.decode(str));
		}
	}
	
	
	private ArrayList fCheckBoxes;
	private ArrayList fRadioButtons;
	private ArrayList fTextControls;
	
	private SelectionListener fSelectionListener;
	private ModifyListener fModifyListener;
	
	private Text fBinFolderNameText;
	private Text fSrcFolderNameText;

	private Combo fJRECombo;

	private Button fProjectAsSourceFolder;
	private Button fFoldersAsSourceFolder;

	private Label fSrcFolderNameLabel;
	private Label fBinFolderNameLabel;

	public NewJavaProjectPreferencePage() {
		super();
		setPreferenceStore(JavaPlugin.getDefault().getPreferenceStore());
		setDescription(JavaUIMessages.getString("NewJavaProjectPreferencePage.description")); //$NON-NLS-1$
	
		fRadioButtons= new ArrayList();
		fCheckBoxes= new ArrayList();
		fTextControls= new ArrayList();
		
		fSelectionListener= new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				controlChanged(e.widget);
			}
		};
		
		fModifyListener= new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				controlModified(e.widget);
			}
		};
		
	}

	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(SRCBIN_FOLDERS_IN_NEWPROJ, false);
		store.setDefault(SRCBIN_SRCNAME, "src"); //$NON-NLS-1$
		store.setDefault(SRCBIN_BINNAME, "bin"); //$NON-NLS-1$
		
		store.setDefault(CLASSPATH_JRELIBRARY_LIST, getDefaultJRELibraries());
		store.setDefault(CLASSPATH_JRELIBRARY_INDEX, "carbon".equals(SWT.getPlatform()) ? 1 : 0); //$NON-NLS-1$
	}
	
	private static String getDefaultJRELibraries() {
		StringBuffer buf= new StringBuffer();
		IClasspathEntry varentry= JavaRuntime.getJREVariableEntry();
		buf.append(encodeJRELibrary(JavaUIMessages.getString("NewJavaProjectPreferencePage.jre_variable.description"), new IClasspathEntry[] { varentry })); //$NON-NLS-1$
		buf.append(';');
		IClasspathEntry cntentry= JavaRuntime.getDefaultJREContainerEntry();
		buf.append(encodeJRELibrary(JavaUIMessages.getString("NewJavaProjectPreferencePage.jre_container.description"), new IClasspathEntry[] { cntentry} )); //$NON-NLS-1$
		buf.append(';');
		return buf.toString();
	}
	

	/*
	 * @see IWorkbenchPreferencePage#init(IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}		
	
	/**
	 * @see PreferencePage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.NEW_JAVA_PROJECT_PREFERENCE_PAGE);
	}	


	private Button addRadioButton(Composite parent, String label, String key, String value, int indent) { 
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		gd.horizontalIndent= indent;
		
		Button button= new Button(parent, SWT.RADIO);
		button.setText(label);
		button.setData(new String[] { key, value });
		button.setLayoutData(gd);

		button.setSelection(value.equals(getPreferenceStore().getString(key)));
		
		fRadioButtons.add(button);
		return button;
	}
	
	private Text addTextControl(Composite parent, Label labelControl, String key, int indent) {
		GridData gd= new GridData();
		gd.horizontalIndent= indent;
		
		labelControl.setLayoutData(gd);
		
		gd= new GridData();
		gd.widthHint= convertWidthInCharsToPixels(40);
		
		Text text= new Text(parent, SWT.SINGLE | SWT.BORDER);
		text.setText(getPreferenceStore().getString(key));
		text.setData(key);
		text.setLayoutData(gd);
		
		fTextControls.add(text);
		return text;
	}	
	
	
	protected Control createContents(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.numColumns= 2;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		
		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan= 2;
		
		Label label= new Label(composite, SWT.WRAP);
		label.setText(JavaUIMessages.getString("NewJavaProjectPreferencePage.sourcefolder.label")); //$NON-NLS-1$
		label.setLayoutData(gd);
		
		int indent= convertWidthInCharsToPixels(1);
		
		fProjectAsSourceFolder= addRadioButton(composite, JavaUIMessages.getString("NewJavaProjectPreferencePage.sourcefolder.project"), SRCBIN_FOLDERS_IN_NEWPROJ, IPreferenceStore.FALSE, indent); //$NON-NLS-1$
		fProjectAsSourceFolder.addSelectionListener(fSelectionListener);

		fFoldersAsSourceFolder= addRadioButton(composite, JavaUIMessages.getString("NewJavaProjectPreferencePage.sourcefolder.folder"), SRCBIN_FOLDERS_IN_NEWPROJ, IPreferenceStore.TRUE, indent); //$NON-NLS-1$
		fFoldersAsSourceFolder.addSelectionListener(fSelectionListener);
		
		indent= convertWidthInCharsToPixels(4);

		fSrcFolderNameLabel= new Label(composite, SWT.NONE);
		fSrcFolderNameLabel.setText(JavaUIMessages.getString("NewJavaProjectPreferencePage.folders.src")); //$NON-NLS-1$
		fSrcFolderNameText= addTextControl(composite, fSrcFolderNameLabel, SRCBIN_SRCNAME, indent); //$NON-NLS-1$
		fSrcFolderNameText.addModifyListener(fModifyListener);

		fBinFolderNameLabel= new Label(composite, SWT.NONE);
		fBinFolderNameLabel.setText(JavaUIMessages.getString("NewJavaProjectPreferencePage.folders.bin")); //$NON-NLS-1$
		fBinFolderNameText= addTextControl(composite, fBinFolderNameLabel, SRCBIN_BINNAME, indent); //$NON-NLS-1$
		fBinFolderNameText.addModifyListener(fModifyListener);

		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		
		String[] jreNames= getJRENames();
		if (jreNames.length > 0) {
			Label jreSelectionLabel= new Label(composite, SWT.NONE);
			jreSelectionLabel.setText(JavaUIMessages.getString("NewJavaProjectPreferencePage.jrelibrary.label")); //$NON-NLS-1$
			jreSelectionLabel.setLayoutData(new GridData());
		
			int index= getPreferenceStore().getInt(CLASSPATH_JRELIBRARY_INDEX);
			fJRECombo= new Combo(composite, SWT.READ_ONLY);
			fJRECombo.setItems(jreNames);
			fJRECombo.select(index);
			fJRECombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		}
					
		validateFolders();
	
		return composite;
	}
	
	private void validateFolders() {
		boolean useFolders= fFoldersAsSourceFolder.getSelection();
		
		fSrcFolderNameText.setEnabled(useFolders);
		fBinFolderNameText.setEnabled(useFolders);
		fSrcFolderNameLabel.setEnabled(useFolders);
		fBinFolderNameLabel.setEnabled(useFolders);		
		if (useFolders) {
			String srcName= fSrcFolderNameText.getText();
			String binName= fBinFolderNameText.getText();
			if (srcName.length() + binName.length() == 0) {
				updateStatus(new StatusInfo(IStatus.ERROR,  JavaUIMessages.getString("NewJavaProjectPreferencePage.folders.error.namesempty"))); //$NON-NLS-1$
				return;
			}
			IWorkspace workspace= JavaPlugin.getWorkspace();
			IStatus status;
			if (srcName.length() != 0) {
				status= workspace.validateName(srcName, IResource.FOLDER);
				if (!status.isOK()) {
					String message= JavaUIMessages.getFormattedString("NewJavaProjectPreferencePage.folders.error.invalidsrcname", status.getMessage()); //$NON-NLS-1$
					updateStatus(new StatusInfo(IStatus.ERROR, message));
					return;
				}
			}
			status= workspace.validateName(binName, IResource.FOLDER);
			if (!status.isOK()) {
				String message= JavaUIMessages.getFormattedString("NewJavaProjectPreferencePage.folders.error.invalidbinname", status.getMessage()); //$NON-NLS-1$
				updateStatus(new StatusInfo(IStatus.ERROR, message));
				return;
			}
			IProject dmy= workspace.getRoot().getProject("dmy"); //$NON-NLS-1$
			IClasspathEntry entry= JavaCore.newSourceEntry(dmy.getFullPath().append(srcName));
			IPath outputLocation= dmy.getFullPath().append(binName);
			status= JavaConventions.validateClasspath(JavaCore.create(dmy), new IClasspathEntry[] { entry }, outputLocation);
			if (!status.isOK()) {
				updateStatus(status);
				return;
			}
		}
		updateStatus(new StatusInfo()); // set to OK
	}
		
	private void updateStatus(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}		
	
	private void controlChanged(Widget widget) {
		if (widget == fFoldersAsSourceFolder || widget == fProjectAsSourceFolder) {
			validateFolders();
		}
	}
	
	private void controlModified(Widget widget) {
		if (widget == fSrcFolderNameText || widget == fBinFolderNameText) {
			validateFolders();
		}
	}	
	
	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		IPreferenceStore store= getPreferenceStore();
		for (int i= 0; i < fCheckBoxes.size(); i++) {
			Button button= (Button) fCheckBoxes.get(i);
			String key= (String) button.getData();
			button.setSelection(store.getDefaultBoolean(key));
		}
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			String[] info= (String[]) button.getData();
			button.setSelection(info[1].equals(store.getDefaultString(info[0])));
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			text.setText(store.getDefaultString(key));
		}
		if (fJRECombo != null) {
			fJRECombo.select(store.getDefaultInt(CLASSPATH_JRELIBRARY_INDEX));
		}
		
		validateFolders();
		super.performDefaults();
	}

	/*
	 * @see IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IPreferenceStore store= getPreferenceStore();
		for (int i= 0; i < fCheckBoxes.size(); i++) {
			Button button= (Button) fCheckBoxes.get(i);
			String key= (String) button.getData();
			store.setValue(key, button.getSelection());
		}
		for (int i= 0; i < fRadioButtons.size(); i++) {
			Button button= (Button) fRadioButtons.get(i);
			if (button.getSelection()) {
				String[] info= (String[]) button.getData();
				store.setValue(info[0], info[1]);
			}
		}
		for (int i= 0; i < fTextControls.size(); i++) {
			Text text= (Text) fTextControls.get(i);
			String key= (String) text.getData();
			store.setValue(key, text.getText());
		}
		
		if (fJRECombo != null) {
			store.setValue(CLASSPATH_JRELIBRARY_INDEX, fJRECombo.getSelectionIndex());
		}
		
		JavaPlugin.getDefault().savePluginPreferences();
		return super.performOk();
	}
	
	private String[] getJRENames() {
		String prefString= getPreferenceStore().getString(CLASSPATH_JRELIBRARY_LIST);
		ArrayList list= new ArrayList();
		StringTokenizer tok= new StringTokenizer(prefString, ";"); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			list.add(decodeJRELibraryDescription(tok.nextToken()));
		}
		return (String[]) list.toArray(new String[list.size()]);
	}

}


