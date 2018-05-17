package org.deidentifier.arx.io;

import com.epam.parso.Column;
import com.epam.parso.SasFileReader;
import com.epam.parso.impl.SasFileReaderImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kenedy on 5/16/18.
 */
public class SASDataInput {
    private InputStream inputStream;
    private SasFileReader sasFileReader;

    public SASDataInput(String fileName) throws IOException{
        this.inputStream = new FileInputStream(fileName);
        this.sasFileReader = new SasFileReaderImpl(inputStream);
    }

    public Iterator<String[]> iterator() {
        return new Iterator<String[]>() {

            private int currentIndex = 0;
            private int numRows = (int) sasFileReader.getSasFileProperties().getRowCount();

            @Override
            public boolean hasNext() {
                return  (currentIndex < numRows);
            }

            @Override
            public String[] next() {
                try {
                    Object[] rawRow = sasFileReader.readNext();
                    String[] row = new String[rawRow.length];
                    for (int cellIndex = 0; cellIndex < rawRow.length; cellIndex++) {
                        row[cellIndex] = rawRow[cellIndex].toString();
                    }
                    currentIndex++;
                    return row;
                } catch (IOException e) {
                    return null;
                }
            }
        };
    }

    public List<Column> getColumns() {
        return sasFileReader.getColumns();
    }

    public void close() throws IOException {
        inputStream.close();
    }
}
