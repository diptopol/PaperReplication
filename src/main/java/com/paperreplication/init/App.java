package com.paperreplication.init;

import com.paperreplication.Utils;
import com.paperreplication.entity.DataSet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Urbashi
 * @since 11/13/2020 7:08 PM
 */
public class App {
    public static void main(String[] args) {
        //initialDataLoading();

        //createProbabilityMatrix("t1FailedMatrix.dat", "FAILED");
        //createProbabilityMatrix("t1PassedMatrix.dat", "PASSED");

        //readAndPrintMatrix("t1FailedMatrix.dat");
        //readAndPrintMatrix("t1PassedMatrix.dat");
    }

    private static void createProbabilityMatrix(String fileName, String status) {
        List<DataSet> trainingDataSet = Utils.getDataSetList("trainingDataSet.csv", true);

        //List<String> distinctTestSuiteList = getDistinctTestSuiteListAfterSerialization(trainingDataSet, "distinctTestSuiteList.csv");
        List<String> distinctTestSuiteList = Utils.getDistinctTestSuiteList("distinctTestSuiteList.csv");

        int size = distinctTestSuiteList.size();
        double[][] probabilityMatrixBasedOnT1 = new double[size][size];

        Map<String, List<Integer>> failedDataSetTestSuiteMap = trainingDataSet.stream()
                .filter(dataSet -> dataSet.getStatus().equals("FAILED"))
                .collect(Collectors.groupingBy(DataSet::getTestSuite, Collectors.mapping(DataSet::getChangeRequestId, Collectors.toList())));

        Map<String, List<Integer>> passedDataSetTestSuiteMap = trainingDataSet.stream()
                .filter(dataSet -> dataSet.getStatus().equals("PASSED"))
                .collect(Collectors.groupingBy(DataSet::getTestSuite, Collectors.mapping(DataSet::getChangeRequestId, Collectors.toList())));

        Date start = new Date();
        IntStream.range(0, size).parallel().forEach(i -> IntStream.range(0, size).parallel().forEach(j-> {
            String testSuite1 = distinctTestSuiteList.get(i);
            String testSuite2 = distinctTestSuiteList.get(j);

            if (!testSuite1.equals(testSuite2)) {
                probabilityMatrixBasedOnT1[i][j] = Utils.getProbability(testSuite1, testSuite2, failedDataSetTestSuiteMap, passedDataSetTestSuiteMap, status);
            }
        }));

        Date end = new Date();
        System.out.println("Duration : " + (end.getTime() - start.getTime()));

        Utils.writeMatrix(probabilityMatrixBasedOnT1, fileName);
        System.out.println("Write completed");
    }

    private static void readAndPrintMatrix(String fileName) {
        List<String> distinctTestSuiteList = Utils.getDistinctTestSuiteList("distinctTestSuiteList.csv");
        int size = distinctTestSuiteList.size();
        double[][] probabilityMatrixBasedOnT1Fail = Utils.readMatrix(fileName);

        //System.out.println(distinctTestSuiteList.get(5433));
        //System.out.println(distinctTestSuiteList.get(4420));

        /*for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (probabilityMatrixBasedOnT1Fail[i][j] > 0) {
                    System.out.println(probabilityMatrixBasedOnT1Fail[i][j] + " [i] : " + i + " [j] : " + j);
                }
            }
        }*/
    }

    private static List<String> getDistinctTestSuiteListAfterSerialization(List<DataSet> dataSetList, String fileName) {
        List<String> distinctTestSuiteList = dataSetList.stream()
                .map(DataSet::getTestSuite)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Utils.writeDistinctTestSuiteList(distinctTestSuiteList, fileName);

        return distinctTestSuiteList;
    }

    private static void initialDataLoading() {
        String postDataSetFileName = "GooglePostCleanData.out";
        List<DataSet> postDataSetList = Utils.getDataSetList(postDataSetFileName, false);

        // sorted by arrival time
        postDataSetList.sort(Comparator.comparing(DataSet::getLaunchTime));

        LocalDateTime splitPoint = LocalDateTime.parse("2014-01-07 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        writeTrainingDataSet(postDataSetList, splitPoint);
        writeTestDataSet(postDataSetList, splitPoint);
    }

    private static void writeTrainingDataSet(List<DataSet> dataSetList, LocalDateTime splitPoint) {
        List<DataSet> trainingDataSet = dataSetList.stream()
                .filter(dataSet -> dataSet.getLaunchTimeDate().isBefore(splitPoint))
                .collect(Collectors.toList());

        Utils.writeDataSet(trainingDataSet, "trainingDataSet.csv");
    }

    private static void writeTestDataSet(List<DataSet> dataSetList, LocalDateTime splitPoint) {
        List<DataSet> testDataSet = dataSetList.stream()
                .filter(dataSet -> !dataSet.getLaunchTimeDate().isBefore(splitPoint))
                .collect(Collectors.toList());

        Utils.writeDataSet(testDataSet, "testDataSet.csv");
    }

}
