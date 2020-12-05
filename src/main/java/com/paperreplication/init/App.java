package com.paperreplication.init;

import com.paperreplication.utils.PerformanceEvaluationUtils;
import com.paperreplication.utils.Utils;
import com.paperreplication.entity.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Urbashi
 * @since 11/13/2020 7:08 PM
 */
public class App {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static List<String> distinctTestSuiteList;
    private static List<DataSet> finishedTestSuiteList;
    private static double[][] t1FailedMatrix;
    private static double[][] t1PassedMatrix;

    public static void main(String[] args) {
        logger.info("Start Time: {}", new Date());

        //initialDataLoading();
        //createProbabilityMatrix("t1FailedMatrix.dat", "FAILED");
        //createProbabilityMatrix("t1PassedMatrix.dat", "PASSED");

        //writeFifoBaselineOutput();
        //writeCodyNaqSingleOutput();

        evaluatePerformance();
    }

    private static void evaluatePerformance() {
        Date start = new Date();
        Map<Integer, List<DataSet>> fifoResult =
                Utils.readTestRequestInfo("FifoBaselineTestRequestInfo.dat");

        Map<Integer, List<DataSet>> codynaqSingleResult =
                Utils.readTestRequestInfo("CodynaqSingleTestRequestInfo.dat");
        Date end = new Date();
        logger.info("Data Read Duration : {}", (end.getTime() - start.getTime()));

        double medianGainFirstFail = PerformanceEvaluationUtils.getMedianGainFirstFailure(fifoResult, codynaqSingleResult);
        logger.info("Median Gain First Fail: {}", medianGainFirstFail);

        double medianGainFirstFailAtLeastOneFailure = PerformanceEvaluationUtils.getMedianGainFirstFailAtLeastOneFailure(fifoResult, codynaqSingleResult);
        logger.info("Median Gain First Fail At Least One Failure: {}", medianGainFirstFailAtLeastOneFailure);

        double medianGainAllFail = PerformanceEvaluationUtils.getMedianGainAllFail(fifoResult, codynaqSingleResult);
        logger.info("Median Gain All Fail: {}", medianGainAllFail);

        double medianGainAllFailAtLeastOneFailure = PerformanceEvaluationUtils.getMedianGainAllFailWithAtLeastOneFailure(fifoResult, codynaqSingleResult);
        logger.info("Median Gain All Fail  At Least One Failure: {}", medianGainAllFailAtLeastOneFailure);

        double medianPercentageDelay = PerformanceEvaluationUtils.getPercentageDelay(fifoResult, codynaqSingleResult);
        logger.info("Percentage Delay: {}", medianPercentageDelay);

        double medianPercentageDelayAtLeastOneFailure = PerformanceEvaluationUtils.getPercentageDelayWithAtLeastOneFailure(fifoResult, codynaqSingleResult);
        logger.info("Percentage Delay  At Least One Failure: {}", medianPercentageDelayAtLeastOneFailure);
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private static List<Integer> getAllPassedChangeRequestIdList() {
        String preDataSetFileName = "GooglePostCleanData.out";
        List<DataSet> preDataSetList = Utils.getDataSetList(preDataSetFileName, false);

        System.out.println(preDataSetList.stream().filter(distinctByKey(DataSet::getChangeRequestId)).count());

        Map<Integer, List<DataSet>> map = preDataSetList.stream().collect(Collectors.groupingBy(DataSet::getChangeRequestId));

        List<Integer> allPassedChangeRequestIdList = new ArrayList<>();
        for (Map.Entry<Integer, List<DataSet>> entry: map.entrySet()) {
            List<DataSet> dataSetList = entry.getValue();

            if (dataSetList.stream().allMatch(dataSet -> dataSet.getStatus().equals("PASSED"))) {
                allPassedChangeRequestIdList.add(entry.getKey());
            }
        }

        return allPassedChangeRequestIdList;
    }

    private static void writeFifoBaselineOutput(){
        List<DataSet> testDataSet = Utils.getDataSetList("testDataSet.csv", true);

        testDataSet.sort(Comparator.comparing(DataSet::getLaunchTimeDate));
        serializeOutput(testDataSet, "FifoBaselineTestRequestInfo.dat");
    }

    private static void writeCodyNaqSingleOutput() {
        Date startInit = new Date();
        List<DataSet> testDataSet = Utils.getDataSetList("testDataSet.csv", true);

        getDistinctTestSuiteList();
        getT1PassedProbabilityMatrix();
        getT1FailedProbabilityMatrix();

        Date endInit = new Date();
        logger.info("Initialization Duration : {}", (endInit.getTime() - startInit.getTime()));

        //sort the data
        testDataSet.sort(Comparator.comparing(DataSet::getLaunchTimeDate));

        Map<Integer, List<DataSet>> finishedTestSetGroupByChangeRequestId = performCodynaqSingleParallel(testDataSet);
        Utils.writeTestRequestInfo(finishedTestSetGroupByChangeRequestId, "CodynaqSingleTestRequestInfo.dat");
    }

    private static void serializeOutput(List<DataSet> dataSetList, String fileName) {
        Map<Integer, List<DataSet>> dataSetGroupedByChangeRequest =
                dataSetList.stream()
                        .collect(Collectors.groupingBy(DataSet::getChangeRequestId));

        Utils.writeTestRequestInfo(dataSetGroupedByChangeRequest, fileName);
    }

    private static Map<Integer, List<DataSet>> performCodynaqSingleParallel(List<DataSet> testDataSet) {
        Map<Integer, List<DataSet>> testDataSetGroupByChangeRequestId = testDataSet.stream()
                .collect(Collectors.groupingBy(DataSet::getChangeRequestId));

        //Map<Integer, List<DataSet>> testDataSetGroupByChangeRequestId2 = Collections.singletonMap(4097, testDataSetGroupByChangeRequestId.get(4097));

        Map<Integer, List<DataSet>> finishedTestSetGroupByChangeRequestId = new ConcurrentHashMap<>();

        Date start = new Date();
        testDataSetGroupByChangeRequestId.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    List<DataSet> dispatchCandidateList = entry.getValue();
                    List<DataSet> finishedTestList = new ArrayList<>();

                    while (dispatchCandidateList.size() > 0) {
                        dispatchCandidateList = getRePrioritizedList(dispatchCandidateList, finishedTestList);
                        DataSet finishedTest = dispatchCandidateList.remove(0);
                        finishedTestList.add(finishedTest);
                    }

                    finishedTestSetGroupByChangeRequestId.put(entry.getKey(), finishedTestList);
                    logger.info("Finished Change Request ID: {}, Finished Test Size: {}", entry.getKey(),
                            finishedTestSetGroupByChangeRequestId.size());
                });

        Date end = new Date();
        logger.info("Duration : " + (end.getTime() - start.getTime()));

        return finishedTestSetGroupByChangeRequestId;
    }

    private static List<DataSet> getRePrioritizedList(List<DataSet> dispatchQueue, List<DataSet> finishedTestSuiteList) {
        List<DataSet> prioritizedList = new ArrayList<>(dispatchQueue);

        // Here 1 test executors
        if (finishedTestSuiteList.size() > 0) {
            DataSet finishedTest = finishedTestSuiteList.get(finishedTestSuiteList.size() - 1);
            List<DataSet> otherTestList = getOtherTestList(finishedTest, prioritizedList);

            for (DataSet dataSet : otherTestList) {
                setPriorities(dataSet, finishedTest);
            }

            prioritizedList.sort(Comparator.comparingDouble(DataSet::getPriority).reversed());
        }

        return prioritizedList;
    }

    private static void setPriorities(DataSet newTest, DataSet finishedTest) {
        int t1Index = getDistinctTestSuiteList().indexOf(finishedTest.getTestSuite());
        int t2Index = getDistinctTestSuiteList().indexOf(newTest.getTestSuite());

        if (finishedTest.getStatus().equals("FAILED")) {
            double probability = t1Index != -1 && t2Index != -1 ? getT1FailedProbabilityMatrix()[t1Index][t2Index] : 0.5;
            newTest.setPriority(newTest.getPriority() + (probability - 0.5));
        } else {
            double probability = t1Index != -1 && t2Index != -1 ? getT1PassedProbabilityMatrix()[t1Index][t2Index] : 0.5;
            newTest.setPriority(newTest.getPriority() + probability - 0.5);
        }
    }

    private static List<DataSet> getOtherTestList(DataSet finishedTest, List<DataSet> dispatchQueue) {
        return dispatchQueue.stream()
                .filter(dataSet -> dataSet.getChangeRequestId() == finishedTest.getChangeRequestId()
                        && !dataSet.getTestSuite().equals(finishedTest.getTestSuite()))
                .collect(Collectors.toList());
    }

    private static List<DataSet> getFinishedTestSuiteList() {
        if (Objects.isNull(finishedTestSuiteList)) {
            finishedTestSuiteList = new ArrayList<>();
        }

        return finishedTestSuiteList;
    }

    private static List<String> getDistinctTestSuiteList() {
        if (Objects.isNull(distinctTestSuiteList)) {
            distinctTestSuiteList = Utils.getDistinctTestSuiteList("distinctTestSuiteList.csv");
        }

        return distinctTestSuiteList;
    }

    private static double[][] getT1PassedProbabilityMatrix() {
        if (Objects.isNull(t1PassedMatrix)) {
            t1PassedMatrix = Utils.readMatrix("t1PassedMatrix.dat");
        }

        return t1PassedMatrix;
    }

    private static double[][] getT1FailedProbabilityMatrix() {
        if (Objects.isNull(t1FailedMatrix)) {
            t1FailedMatrix = Utils.readMatrix("t1FailedMatrix.dat");
        }

        return t1FailedMatrix;
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
        IntStream.range(0, size).parallel().forEach(i -> IntStream.range(0, size).parallel().forEach(j -> {
            String testSuite1 = distinctTestSuiteList.get(i);
            String testSuite2 = distinctTestSuiteList.get(j);

            if (!testSuite1.equals(testSuite2)) {
                probabilityMatrixBasedOnT1[i][j] = Utils.getProbability(testSuite1, testSuite2, failedDataSetTestSuiteMap, passedDataSetTestSuiteMap, status);
            }
        }));

        Date end = new Date();
        logger.info("Duration : " + (end.getTime() - start.getTime()));

        Utils.writeMatrix(probabilityMatrixBasedOnT1, fileName);
        logger.info("Write completed");
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
