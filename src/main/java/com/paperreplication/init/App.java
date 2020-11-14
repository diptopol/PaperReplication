package com.paperreplication.init;

import com.paperreplication.entity.DataSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Urbashi
 * @since 11/13/2020 7:08 PM
 */
public class App {
    public static void main(String[] args) {
        String fileName = "dataset/GooglePresCleanData.out";

        List<DataSet> dataSetList = getDataSetList(fileName);
    }

    private static List<DataSet> getDataSetList(String fileName) {
        List<DataSet> dataSetList = new ArrayList<>();

        try {
            Reader in = new FileReader(fileName);

            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .parse(in);

            dataSetList = StreamSupport.stream(records.spliterator(), false)
                    .map(DataSet::new)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataSetList;
    }
}
