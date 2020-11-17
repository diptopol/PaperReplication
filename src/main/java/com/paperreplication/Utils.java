package com.paperreplication;

import com.paperreplication.entity.ChangeRequestStatus;
import com.paperreplication.entity.DataSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Diptopol
 * @since 11/14/2020 11:29 AM
 */
public class Utils {

    public static long getNumberOfChangeRequestBasedOnT1TestStatus(String testSuite, Map<Integer, List<DataSet>> dataGroupedByChangeRequest, String status) {

        int count = 0;
        for (Map.Entry<Integer, List<DataSet>> entry : dataGroupedByChangeRequest.entrySet()) {
            List<DataSet> dataSetListPerChangeRequest = entry.getValue();

            List<DataSet> filteredT1List = dataSetListPerChangeRequest.stream()
                    .filter(dataSet -> dataSet.getTestSuite().equals(testSuite) && status.equals(dataSet.getStatus()))
                    .collect(Collectors.toList());

            if (filteredT1List.size() > 0) {
                count++;
            }
        }

        return count;
    }

    public static double getProbability(String testSuite1, String testSuite2,
                                        Map<String, List<Integer>> failedDataSetTestSuiteMap,
                                        Map<String, List<Integer>> passedChangeRequestIdTestSuiteMap, String t1Status) {

        long t1Andt2Count = 0;
        long t1Count = 0;

        List<Integer> failedListForTestSuite2 = failedDataSetTestSuiteMap.get(testSuite2);
        List<Integer> failedListForTestSuite1 = failedDataSetTestSuiteMap.get(testSuite1);
        List<Integer> passedListForTestSuite1 = passedChangeRequestIdTestSuiteMap.get(testSuite1);

        if (t1Status.equals("FAILED")) {
            t1Andt2Count = Optional.ofNullable(failedListForTestSuite2).orElse(Collections.emptyList())
                    .stream()
                    .filter(t2 -> Objects.nonNull(failedListForTestSuite1) && failedListForTestSuite1.contains(t2)).count();
            t1Count = Objects.nonNull(failedListForTestSuite1) ? failedListForTestSuite1.size() : 0;

        } else {
            t1Andt2Count = Optional.ofNullable(failedListForTestSuite2).orElse(Collections.emptyList())
                    .stream().filter(t2 -> (Objects.nonNull(passedListForTestSuite1) && passedListForTestSuite1.contains(t2))).count();
            t1Count = Objects.nonNull(passedListForTestSuite1)
                    ? passedListForTestSuite1.size()
                    : 0;
        }

        if (t1Count == 0 || t1Andt2Count == 0) {
            return 0;
        } else {
            return ((double) t1Andt2Count) / t1Count;
        }
    }

    public static double getProbabilityGivenT1Status(String testSuite1, String testSuite2,
                                                     Map<Integer, List<DataSet>> dataGroupedByChangeRequest, String t1Status) {
        long t1Count = 0;
        long t1Andt2Count = 0;

        for (Map.Entry<Integer, List<DataSet>> entry : dataGroupedByChangeRequest.entrySet()) {
            List<DataSet> dataSetListPerChangeRequest = entry.getValue();

            List<DataSet> filteredT1List = dataSetListPerChangeRequest.stream()
                    .filter(dataSet -> dataSet.getTestSuite().equals(testSuite1) && t1Status.equals(dataSet.getStatus()))
                    .collect(Collectors.toList());

            List<DataSet> filteredT2List = dataSetListPerChangeRequest.stream()
                    .filter(dataSet -> dataSet.getTestSuite().equals(testSuite2) && "FAILED".equals(dataSet.getStatus()))
                    .collect(Collectors.toList());

            if (filteredT1List.size() > 0) {
                t1Count++;
            }

            if (filteredT1List.size() > 0 && filteredT2List.size() > 0) {
                t1Andt2Count++;
            }
        }

        if (t1Count == 0 && t1Andt2Count == 0) {
            return 0;
        } else {
            return ((double) t1Andt2Count) / t1Count;
        }
    }

    public static void writeDataSet(List<DataSet> dataSetList, String fileName) {
        try (FileWriter out = new FileWriter("dataset/" + fileName);
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(DataSet.HEADER))) {
            Consumer<DataSet> dataSetConsumer = dataSet -> {
                try {
                    printer.printRecord(dataSet.getTestSuite(), dataSet.getChangeRequestId(), dataSet.getStage(),
                            dataSet.getStatus(), dataSet.getLaunchTime(), dataSet.getExecutionTimeInMilliSeconds(),
                            dataSet.getSize(), dataSet.getShardNumber(), dataSet.getRunNumber(), dataSet.getLanguage());

                } catch (IOException ex) {
                    ex.printStackTrace();
                }

            };

            dataSetList.forEach(dataSetConsumer);
            printer.flush();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void writeChangeRequestStatus(List<ChangeRequestStatus> changeRequestStatusList, String fileName) {
        changeRequestStatusList.sort(Comparator.comparing(ChangeRequestStatus::getChangeRequestId));

        try {
            FileWriter out = new FileWriter("dataset/" + fileName);

            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
                    .withHeader(ChangeRequestStatus.HEADERS))) {
                Consumer<ChangeRequestStatus> changeRequestStatusConsumer = changeRequestStatus -> {
                    try {
                        printer.printRecord(changeRequestStatus.getChangeRequestId(), changeRequestStatus.getPassCount(),
                                changeRequestStatus.getFailCount());

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                };
                changeRequestStatusList.forEach(changeRequestStatusConsumer);

                printer.flush();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static List<DataSet> getDataSetList(String fileName, boolean headerIncluded) {
        List<DataSet> dataSetList = new ArrayList<>();

        try {
            Reader in = new FileReader("dataset/" + fileName);

            CSVFormat csvFormat = CSVFormat.DEFAULT;

            if (headerIncluded) {
                csvFormat = csvFormat.withFirstRecordAsHeader();
            }

            Iterable<CSVRecord> records = csvFormat.parse(in);

            dataSetList = StreamSupport.stream(records.spliterator(), false)
                    .map(DataSet::new)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataSetList;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public static List<String> getDistinctTestSuiteList(String fileName) {
        List<String> distinctTestSuiteList = new ArrayList<>();

        try {
            Reader in = new FileReader("dataset/" + fileName);

            CSVFormat csvFormat = CSVFormat.DEFAULT;

            Iterable<CSVRecord> records = csvFormat.parse(in);

            distinctTestSuiteList = StreamSupport.stream(records.spliterator(), false)
                    .map(record -> record.get(0))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return distinctTestSuiteList;

    }

    public static void writeDistinctTestSuiteList(List<String> distinctTestSuiteList, String fileName) {
        try (FileWriter out = new FileWriter("dataset/" + fileName);
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT)) {

            distinctTestSuiteList.forEach(testSuite -> {
                try {
                    printer.printRecord(testSuite);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            printer.flush();

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public static void writeMatrix(double[][] matrix, String fileName) {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("dataset/" + fileName))) {

            objectOutputStream.writeObject(matrix);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static double[][] readMatrix(String fileName) {
        double[][] matrix = null;

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("dataset/" + fileName))) {

            matrix = (double[][]) objectInputStream.readObject();

        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        return matrix;
    }
}
