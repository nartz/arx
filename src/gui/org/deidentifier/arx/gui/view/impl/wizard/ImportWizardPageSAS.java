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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.epam.parso.SasFileReader;
import com.epam.parso.impl.SasFileReaderImpl;
import org.deidentifier.arx.DataType;
import org.deidentifier.arx.gui.resources.Charsets;
import org.deidentifier.arx.gui.resources.Resources;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.deidentifier.arx.io.CSVSyntax;
import org.deidentifier.arx.io.ImportAdapter;
import org.deidentifier.arx.io.ImportColumn;
import org.deidentifier.arx.io.ImportColumnCSV;
import org.deidentifier.arx.io.ImportConfigurationCSV;
import org.deidentifier.arx.io.ImportConfigurationSAS;
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

import com.carrotsearch.hppc.CharIntOpenHashMap;
import com.carrotsearch.hppc.IntIntOpenHashMap;
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
     * Label provider for CSV columns
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

    /** Button. */
    private Button                             btnContainsHeader;

    /** Combo. */
    private Combo                              comboDelimiter;

    /** Combo. */
    private Combo                              comboLinebreak;

    /** Combo. */
    private Combo                              comboQuote;

    /** Combo. */
    private Combo                              comboEscape;

    /** Combo. */
    private Combo                              comboCharset;

    /** Label. */
    private Label                              lblDelimiter;

    /** Label. */
    private Label                              lblQuote;

    /** Label. */
    private Label                              lblLinebreak;

    /** Label. */
    private Label                              lblEscape;

    /** Label. */
    private Label                              lblCharset;

    /** Table. */
    private Table                              tablePreview;

    /** TableViewer. */
    private TableViewer                        tableViewerPreview;

    /**
     * Currently selected separator (index).
     *
     * @see {@link #delimiters}
     */
    private int                                selectedDelimiter = 0;

    /**
     * Currently selected delimiter (index).
     *
     * @see {@link #quotes}
     */
    private int                                selectedQuote = 0;

    /**
     * Currently selected escape (index).
     *
     * @see {@link #quotes}
     */
    private int                                selectedEscape    = 0;

    /**
     * Currently selected line break (index).
     */
    private int                                selectedLinebreak    = 0;

    /**
     * Currently selected charset (index).
     */
    private int                                selectedCharset    = 0;


    /**
     * Supported escape characters.
     *
     * @see {@link #labels}
     * @note This are the escape characters.
     */
    private final char[]                       escapes           = { '\"', '\\' };

    /**
     * Supported delimiters.
     *
     * @see {@link #labels}
     * @note This are the delimiters.
     */
    private final char[]                       quotes        = { '\"', '\'' };

    /**
     * Supported separators.
     *
     * @see {@link #labels}
     * @note This are the separators itself. The appropriate combo box will
     *       display the {@link #labels} instead.
     */
    private final char[]                       delimiters        = { ';', ',', '|', '\t' };

    /**
     * Labels for separators defined in {@link #delimiters}.
     *
     * @see {@link #delimiters}
     */
    private final String[]                     labels            = { ";", ",", "|", "Tab" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    /**
     * Indicates whether separator was detected automatically or by the user
     *
     * The separator will usually be detected automatically {@link #detectDelimiter()}. In case the user selected another separator
     * by hand, this flag will be set to true, making sure the rest of the logic
     * knows about it.
     */
    private boolean                            customDelimiter;

    /**
     * Indicates whether line break was detected automatically or by the user
     *
     * The line break will usually be detected automatically {@link #detectLinebreak()}. In case the user selected another line break
     * by hand, this flag will be set to true, making sure the rest of the logic
     * knows about it.
     */
    private boolean                            customLinebreak;

    /** Data for preview. */
    private final ArrayList<String[]>          previewData       = new ArrayList<String[]>();

    /**
     * Creates a new instance of this page and sets its title and description.
     *
     * @param wizardImport Reference to wizard containing this page
     */
    public ImportWizardPageSAS(ImportWizard wizardImport)
    {

        super("WizardImportSasPage"); //$NON-NLS-1$
        setTitle("SAS"); //$NON-NLS-1$
        setDescription(Resources.getMessage("ImportWizardPageCSV.6")); //$NON-NLS-1$
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
        lblLocation.setText(Resources.getMessage("ImportWizardPageCSV.7")); //$NON-NLS-1$

        /* Combo box for selection of file */
        comboLocation = new Combo(container, SWT.READ_ONLY);
        comboLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboLocation.addSelectionListener(new SelectionAdapter() {
            /**
             * Resets {@link customSeparator} and evaluates page
             */
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                /* Make widgets visible */
                //lblDelimiter.setVisible(true);
                //comboDelimiter.setVisible(true);
                //lblQuote.setVisible(true);
                //comboQuote.setVisible(true);
                //lblLinebreak.setVisible(true);
                //comboLinebreak.setVisible(true);
                //lblEscape.setVisible(true);
                lblCharset.setVisible(true);
                comboCharset.setVisible(true);
                //comboEscape.setVisible(true);
                btnContainsHeader.setVisible(true);
                customDelimiter = false;
                customLinebreak = false;
                evaluatePage();
            }
        });

        /* Button to open file selection dialog */
        btnChoose = new Button(container, SWT.NONE);
        btnChoose.setText(Resources.getMessage("ImportWizardPageCSV.8")); //$NON-NLS-1$
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
        lblCharset.setText(Resources.getMessage("ImportWizardPageCSV.20")); //$NON-NLS-1$

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

        /* Contains header button */
        btnContainsHeader = new Button(container, SWT.CHECK);
        btnContainsHeader.setVisible(false);
        btnContainsHeader.setText(Resources.getMessage("ImportWizardPageCSV.14")); //$NON-NLS-1$
        btnContainsHeader.setSelection(true);
        btnContainsHeader.addSelectionListener(new SelectionAdapter() {

            /**
             * (Re-)Evaluate page
             */
            @Override
            public void widgetSelected(SelectionEvent arg0) {
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
            setErrorMessage(Resources.getMessage("ImportWizardPageCSV.16")); //$NON-NLS-1$
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
        data.setFirstRowContainsHeader(btnContainsHeader.getSelection());
        data.setFileLocation(comboLocation.getText());
        data.setCsvDelimiter(delimiters[selectedDelimiter]);
        data.setCsvQuote(quotes[selectedQuote]);
        data.setCsvEscape(escapes[selectedEscape]);
        data.setCharset(Charsets.getCharsetForName(Charsets.getNamesOfAvailableCharsets()[selectedCharset]));
        data.setCsvLinebreak(CSVSyntax.getLinebreakForLabel(CSVSyntax.getAvailableLinebreaks()[selectedLinebreak]));

        /* Mark page as completed */
        setPageComplete(true);
    }

    private Charset getCharset() {
        // TODO: get charset from user
        return Charset.defaultCharset();
    }

    private Iterator<String[]> getIteratorForSasFile(SasFileReader sasFileReader) {
        Object[][] rawData = sasFileReader.readAll();
        List<String[]> dataAsStrings = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < rawData.length; rowIndex++) {
            String[] row = new String[rawData[rowIndex].length];
            for (int cellIndex = 0; cellIndex < rawData[rowIndex].length; cellIndex++) {
                row[cellIndex] = rawData[rowIndex][cellIndex].toString();
            }
            dataAsStrings.add(row);
        }
        return dataAsStrings.iterator();
    }

    /**
     * Reads in preview data
     *
     * This goes through up to {@link ImportWizardModel#PREVIEW_MAX_LINES} lines
     * within the appropriate file and reads them in. It uses {@link ImportAdapter} in combination with {@link ImportConfigurationCSV} to actually read in the data.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void readPreview() throws IOException {

        /* Reset preview data */
        previewData.clear();

        /* Parameters from the user interface */
        final String location = comboLocation.getText();
        final boolean containsHeader = btnContainsHeader.getSelection();
        final Charset charset = Charsets.getCharsetForName(Charsets.getNamesOfAvailableCharsets()[selectedCharset]);

        /* Variables needed for processing */
        InputStream inputStream = new FileInputStream(location);
        SasFileReader sasFileReader = new SasFileReaderImpl(inputStream);
        final Iterator<String[]> it = getIteratorForSasFile(sasFileReader);

        final String[] firstLine;
        wizardColumns = new ArrayList<ImportWizardModelColumn>();
        ImportConfigurationSAS config = new ImportConfigurationSAS(location, charset, containsHeader);

        /* Check whether there is at least one line in file and retrieve it */
        if (it.hasNext()) {
            firstLine = it.next();
        } else {
            inputStream.close();
            throw new IOException(Resources.getMessage("ImportWizardPageCSV.17")); //$NON-NLS-1$
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

        inputStream.close();

        /* Remove first entry as it always contains name of columns */
        previewData.remove(0);

        /* Check whether there is actual any data */
        if (previewData.size() == 0) {
            throw new IOException(Resources.getMessage("ImportWizardPageCSV.18")); //$NON-NLS-1$
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

            if (btnContainsHeader.getSelection()) {
                tableColumn.setText(column.getColumn().getAliasName());
                tableColumn.setToolTipText(Resources.getMessage("ImportWizardPageCSV.19") + ((ImportColumnCSV) column.getColumn()).getIndex()); //$NON-NLS-1$
            }
        }

        ColumnViewerToolTipSupport.enableFor(tableViewerPreview, ToolTip.NO_RECREATE);

        /* Setup preview table */
        tableViewerPreview.setInput(previewData);
        tablePreview.setHeaderVisible(btnContainsHeader.getSelection());
        tablePreview.setVisible(true);
        tablePreview.layout();
        tablePreview.setRedraw(true);
    }
}
