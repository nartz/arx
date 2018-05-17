/*
 * ARX: Powerful Data Anonymization
 * Copyright 2014 - 2015 Karol Babioch, Fabian Prasser, Florian Kohlmayer
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

package org.deidentifier.arx.io;

import java.nio.charset.Charset;

/**
 * Configuration describing a SAS file.
 *
 * @author Karol Babioch
 * @author Fabian Prasser
 */
public class ImportConfigurationSAS extends ImportConfigurationFile { // NO_UCD

    /** The charset of the CSV file */
    private final Charset charset;

    /**
     * Creates a new instance of this object.
     *
     * @param fileLocation {@link #setFileLocation(String)}
     * @param containsHeader {@link #containsHeader}
     */
    public ImportConfigurationSAS(String fileLocation,
                                  Charset charset) {
        setFileLocation(fileLocation);
        this.charset = charset;
    }

    /**
     * Adds a single column to import from
     *
     * This makes sure that only {@link ImportColumnCSV} can be added, otherwise
     * an {@link IllegalArgumentException} will be thrown.
     *
     * @param column
     *            A single column to import from, {@link ImportColumnCSV}
     */
    @Override
    public void addColumn(ImportColumn column) {

        if (!(column instanceof ImportColumnCSV)) {
            throw new IllegalArgumentException("Column needs to be of type CSVColumn");
        }

        if (!((ImportColumnCSV) column).isIndexSpecified()) {
            final String ERROR = "Adressing columns by name is only possible if the source contains a header";
            throw new IllegalArgumentException(ERROR);
        }

        for (ImportColumn c : columns) {
            if (((ImportColumnCSV) column).isIndexSpecified() &&
                (((ImportColumnCSV) column).getIndex() == ((ImportColumnCSV) c).getIndex())) {
                throw new IllegalArgumentException("Column for this index already assigned");
            }

            if (!((ImportColumnCSV) column).isIndexSpecified() &&
                ((ImportColumnCSV) column).getName().equals(((ImportColumnCSV) c).getName())) {
                throw new IllegalArgumentException("Column for this name already assigned");
            }

            if ((column.getAliasName() != null) && (c.getAliasName() != null) &&
                c.getAliasName().equals(column.getAliasName())) {
                throw new IllegalArgumentException("Column names need to be unique");
            }
        }

        columns.add(column);
    }

    /**
     * Returns the charset of the CSV file.
     * @return
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Sets the indexes based on the header.
     *
     * @param row the row
     */
    public void prepare(String[] row) {

        for (ImportColumn c : super.getColumns()) {
            ImportColumnCSV column = (ImportColumnCSV) c;
            if (!column.isIndexSpecified()) {
                boolean found = false;
                for (int i = 0; i < row.length; i++) {
                    if (row[i].equals(column.getName())) {
                        found = true;
                        column.setIndex(i);
                    }
                }
                if (!found) {
                    throw new IllegalArgumentException("Index for column '" + column.getName() + "' couldn't be found");
                }
            }
        }
    }
}
