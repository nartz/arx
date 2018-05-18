/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2018 Fabian Prasser and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.gui.view.impl.wizard;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

import org.deidentifier.arx.DataType;
import org.deidentifier.arx.gui.resources.Charsets;
import org.deidentifier.arx.gui.resources.Resources;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.deidentifier.arx.io.ImportAdapter;
import org.deidentifier.arx.io.ImportColumn;
import org.deidentifier.arx.io.ImportColumnCSV;
import org.deidentifier.arx.io.ImportConfigurationCSV;
import org.deidentifier.arx.io.ImportConfigurationSAS;
import org.deidentifier.arx.io.SASDataInput;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import com.univocity.parsers.common.TextParsingException;


/**
 * SAS page
 *
 * This page offers a means to import data from a SAS database. This is modeled off of
 * {@link ImportWizardPageCSV}
 *
 */
public class ImportWizardPageSAS extends WizardPage {

    /**
     * Label provider for SAS columns
     *
     * A new instance of this object will be initiated for each column of
     * {@link tableViewerPreview}. This class holds the index of the
     * appropriate column {@link #index}, making sure they will return the
     * correct value for each column.
     */
    class SASColumnLabelProvider extends ColumnLabelProvider {

        /** Index of the column this instance is representing. */
        private int index;


        /**
         * Creates new instance of this class for the given index.
         *
         * @param index Index the instance should be created for
         */
        public SASColumnLabelProvider(int index) {
            this.index = index;
        }

        /**
         * Returns the string value for the given column.
         *
         * @param element the element
         * @return the text
         */
        @Override
        public String getText(Object element) {
            return ((String[]) element)[index];
        }
    }

    /** Reference to the wizard containing this page. */
    private ImportWizard                       wizardImport;

    /**
     * Columns detected by this page and passed on to {@link ImportWizardModel}.
     */
    private ArrayList<ImportWizardModelColumn> wizardColumns;
    /* Widgets */
    /** Label. */
    private Label                              lblLocation;

    /** Combo. */
    private Combo                              comboLocation;

    /** Button. */
    private Button                             btnChoose;

    /** Combo. */
    private Combo                              comboCharset;

    /** Label. */
    private Label                              lblCharset;

    /** Table. */
    private Table                              tablePreview;

    /** TableViewer. */
    private TableViewer                        tableViewerPreview;

    /**
     * Currently selected charset (index).
     */
    private int                                selectedCharset    = 0;

    /** Data for preview. */
    private final ArrayList<String[]>          previewData       = new ArrayList<>();

    /**
     * Creates a new instance of this page and sets its title and description.
     *
     * @param wizardImport Reference to wizard containing this page
     */
    public ImportWizardPageSAS(ImportWizard wizardImport)
    {

        super("WizardImportSasPage"); //$NON-NLS-1$
        setTitle("SAS"); //$NON-NLS-1$
        setDescription(Resources.getMessage("ImportWizardPageSAS.6")); //$NON-NLS-1$
        this.wizardImport = wizardImport;

    }

    /**
     * Creates the design of this page
     *
     * This adds all the controls to the page along with their listeners.
     *
     * @param parent the parent
     * @note {@link #tablePreview} is not visible until a file is loaded.
     */
    public void createControl(Composite parent)
    {

        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new GridLayout(3, false));

        /* Location label */
        lblLocation = new Label(container, SWT.NONE);
        lblLocation.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblLocation.setText(Resources.getMessage("ImportWizardPageSAS.7")); //$NON-NLS-1$

        /* Combo box for selection of file */
        comboLocation = new Combo(container, SWT.READ_ONLY);
        comboLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboLocation.addSelectionListener(new SelectionAdapter() {
            /**
             * Resets {@link customSeparator} and evaluates page
             */
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                lblCharset.setVisible(true);
                comboCharset.setVisible(true);
                evaluatePage();
            }
        });

        /* Button to open file selection dialog */
        btnChoose = new Button(container, SWT.NONE);
        btnChoose.setText(Resources.getMessage("ImportWizardPageSAS.8")); //$NON-NLS-1$
        btnChoose.addSelectionListener(new SelectionAdapter() {

            /**
             * Opens a file selection dialog for CSV files
             *
             * If a valid CSV file was selected, it is added to
             * {@link #comboLocation} when it wasn't already there. It is then
             * preselected within {@link #comboLocation} and the page is
             * evaluated {@see #evaluatePage}.
             */
            @Override
            public void widgetSelected(SelectionEvent arg0) {

                /* Open file dialog */
                final String path = wizardImport.getController().actionShowOpenFileDialog(getShell(),
                    "*.sas7bdat"); //$NON-NLS-1$
                if (path == null) {
                    return;
                }

                /* Check whether path was already added */
                if (comboLocation.indexOf(path) == -1) {
                    comboLocation.add(path, 0);
                }

                /* Select path and notify comboLocation about change */
                comboLocation.select(comboLocation.indexOf(path));
                comboLocation.notifyListeners(SWT.Selection, null);
            }
        });

        /* Charset label */
        lblCharset = new Label(container, SWT.NONE);
        lblCharset.setVisible(false);
        lblCharset.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblCharset.setText(Resources.getMessage("ImportWizardPageSAS.20")); //$NON-NLS-1$

        /* Charset combobox */
        comboCharset = new Combo(container, SWT.READ_ONLY);
        comboCharset.setVisible(false);

        /* Add labels */
        int index = 0;
        for (final String s : Charsets.getNamesOfAvailableCharsets()) {
            comboCharset.add(s);
            if (s.equals(Charsets.getNameOfDefaultCharset())) {
                selectedCharset = index;
            }
            index++;
        }

        comboCharset.select(selectedCharset);
        comboCharset.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboCharset.addSelectionListener(new SelectionAdapter() {

            /**
             * Set selection index and customDelimiter and (re-)evaluates page
             */
            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                selectedCharset = comboCharset.getSelectionIndex();
                evaluatePage();
            }
        });

        /* Place holder */
        new Label(container, SWT.NONE);

        /* Place holders */
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);

        /* Preview table viewer */
        tableViewerPreview = SWTUtil.createTableViewer(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        tableViewerPreview.setContentProvider(new ArrayContentProvider());

        /* Actual table for {@link #tableViewerPreview} */
        tablePreview = tableViewerPreview.getTable();
        GridData gd_tablePreview = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
        gd_tablePreview.heightHint = 150;
        tablePreview.setLayoutData(gd_tablePreview);
        tablePreview.setLinesVisible(true);
        tablePreview.setVisible(false);

        /* Set page to incomplete by default */
        setPageComplete(false);
    }


    /**
     * Evaluates the page
     *
     * This checks whether the current settings on the page make any sense.
     * If everything is fine, the settings are being put into the appropriate
     * data container {@link ImportWizardModel} and the  current page is marked as
     * complete by invoking {@link #setPageComplete(boolean)}. Otherwise an
     * error message is set, which will make sure the user is informed about
     * the reason for the error.
     */
    private void evaluatePage() {

        setPageComplete(false);
        setErrorMessage(null);
        tablePreview.setVisible(false);

        if (comboLocation.getText().equals("")) { //$NON-NLS-1$
            return;
        }

        try {
            readPreview();

        } catch (IOException | IllegalArgumentException e) {
            setErrorMessage(e.getMessage());
            return;
        } catch (TextParsingException e) {
            setErrorMessage(Resources.getMessage("ImportWizardPageSAS.16")); //$NON-NLS-1$
            return;
        } catch (RuntimeException e) {
            if (e.getCause()!=null) {
                setErrorMessage(e.getCause().getMessage());
            } else {
                setErrorMessage(e.getMessage());
            }
            return;
        }

        /* Put data into container */
        ImportWizardModel data = wizardImport.getData();

        data.setWizardColumns(wizardColumns);
        data.setPreviewData(previewData);
        data.setFileLocation(comboLocation.getText());

        data.setCharset(Charsets.getCharsetForName(Charsets.getNamesOfAvailableCharsets()[selectedCharset]));

        /* Mark page as completed */
        setPageComplete(true);
    }

    /**
     * Reads in preview data
     *
     * This goes through up to {@link ImportWizardModel#PREVIEW_MAX_LINES} lines
     * within the appropriate file and reads them in. It uses {@link ImportAdapter} in combination with {@link ImportConfigurationSAS} to actually read in the data.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void readPreview() throws IOException {

        /* Reset preview data */
        previewData.clear();

        /* Parameters from the user interface */
        final String location = comboLocation.getText();
        final Charset charset = Charsets.getCharsetForName(Charsets.getNamesOfAvailableCharsets()[selectedCharset]);

        /* Variables needed for processing */
        SASDataInput sasDataInput = new SASDataInput(location);
        final Iterator<String[]> it = sasDataInput.iterator();

        final String[] firstLine;
        wizardColumns = new ArrayList<ImportWizardModelColumn>();
        ImportConfigurationSAS config = new ImportConfigurationSAS(location, charset);

        /* Check whether there is at least one line in file and retrieve it */
        if (it.hasNext()) {
            firstLine = it.next();
        } else {
            sasDataInput.close();
            throw new IOException(Resources.getMessage("ImportWizardPageSAS.17")); //$NON-NLS-1$
        }

        /* Iterate over columns and add it to {@link #allColumns} */
        for (int i = 0; i < firstLine.length; i++) {

            ImportColumn column = new ImportColumnCSV(i, DataType.STRING);
            ImportWizardModelColumn wizardColumn = new ImportWizardModelColumn(column);

            wizardColumns.add(wizardColumn);
            config.addColumn(column);
        }

        /* Create adapter to import data with given configuration */
        ImportAdapter importAdapter = ImportAdapter.create(config);

        /* Get up to {ImportData#previewDataMaxLines} lines for previewing */
        int count = 0;
        while (importAdapter.hasNext() && (count <= ImportWizardModel.PREVIEW_MAX_LINES)) {
            previewData.add(importAdapter.next());
            count++;
        }

        sasDataInput.close();

        /* Remove first entry as it always contains name of columns */
        previewData.remove(0);

        /* Check whether there is actual any data */
        if (previewData.size() == 0) {
            throw new IOException(Resources.getMessage("ImportWizardPageSAS.18")); //$NON-NLS-1$
        }

        /*
         * Show preview in appropriate table
         */

        /* Disable redrawing once redesign is finished */
        tablePreview.setRedraw(false);

        /* Remove all of the old columns */
        while (tablePreview.getColumnCount() > 0) {
            tablePreview.getColumns()[0].dispose();
        }

        /* Add new columns */
        for (ImportWizardModelColumn column : wizardColumns) {

            TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewerPreview, SWT.NONE);
            tableViewerColumn.setLabelProvider(new SASColumnLabelProvider(((ImportColumnCSV) column.getColumn()).getIndex()));

            TableColumn tableColumn = tableViewerColumn.getColumn();
            tableColumn.setWidth(100);

            tableColumn.setText(column.getColumn().getAliasName());
            tableColumn.setToolTipText(Resources.getMessage("ImportWizardPageSAS.19") + ((ImportColumnCSV) column.getColumn()).getIndex()); //$NON-NLS-1$
        }

        ColumnViewerToolTipSupport.enableFor(tableViewerPreview, ToolTip.NO_RECREATE);

        /* Setup preview table */
        tableViewerPreview.setInput(previewData);
        tablePreview.setVisible(true);
        tablePreview.layout();
        tablePreview.setRedraw(true);
    }
}
