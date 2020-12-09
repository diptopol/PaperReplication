package com.paperreplication.init;

import com.paperreplication.entity.DataSet;
import com.paperreplication.utils.PerformanceEvaluationUtils;
import com.paperreplication.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

        /*
         * InitialDataLoading method load the GooglePostCleanData.out file split the dataset into training and test data.
         * We have considered the first 7 days data as training data and considered the remaining data as test data.
         * We have stored the dataset in separate files(e.g. trainingDataSet.csv, testDataSet.csv).
         *
         * Since now we are no longer required to split the data, we have commented out the method.
         */
        //initialDataLoading();

        /*
         * saveDistinctTestSuiteList method will save the unique test suites id in a file which will be used later to
         * create the co-failure distribution matrix.
         */
        //saveDistinctTestSuiteList(trainingDataSet, "distinctTestSuiteList.csv");

        /*
         * Below these two consecutive methods generates the co-failure distribution matrix from the trainingData. First
         * method will generate matrix for scenario where t1 test failed and second method will generate matrix for scenario
         * where t1 test passed. Both generated matrix were saved using java serialization in files.
         *
         * We have commented out generation of matrix and fetch the saved files to fetch the matrix.
         */
        //createProbabilityMatrix("t1FailedMatrix.dat", "FAILED");
        //createProbabilityMatrix("t1PassedMatrix.dat", "PASSED");

        /*
         * writeFifoBaselineOutput method will order the tests according to FIFOBaseline. Then the testSuites will be
         * grouped by Change Request ID. The result map will be stored in the file using Java Serialization.
         *
         * Commented out the method after the output is saved in the file. TO measure the performance read the output
         * from the file
         */
        //writeFifoBaselineOutput();

        /*
         * writeCodyNaqSingleOutput method will perform the CodynaQSingle algorithm and re-prioritize the testSuites. Then
         * the testSuites will be saved as grouped by Change Request ID using Java Serialization.
         *
         * Commented out the method after the output is saved in the file. TO measure the performance read the output
         * from the file
         */
        //writeCodyNaqSingleOutput();

        /*
         * evaluatePerformance method will measure the performance of the experiment
         */
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

    /**
     * This method ordered the test suites based on launch time to simulate FIFOBASELINE. The sorted list then grouped
     * into multiple groups based on changeRequestID and saved using java serialization.
     *
     * Also See {@link com.paperreplication.init.App#performCodynaqSingleParallel(List)}
     */
    private static void writeFifoBaselineOutput(){
        List<DataSet> testDataSet = Utils.getDataSetList("testDataSet.csv", true);

        testDataSet.sort(Comparator.comparing(DataSet::getLaunchTimeDate));
        serializeOutput(testDataSet, "FifoBaselineTestRequestInfo.dat");
    }

    /**
     * This method performs codynaqSingle algorithm and then saved the group of list grouped by change request ID. This
     * method first load the co-failure distribution matrix and then perform the algorithm and saved the result.
     */
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

    /**
     * This method create a map from list using grouped by param and saved the map in file using Java Serialization
     *
     * @param dataSetList the list on which the group by operation will be performed
     * @param fileName name of the file on which the data will be stored
     */
    private static void serializeOutput(List<DataSet> dataSetList, String fileName) {
        Map<Integer, List<DataSet>> dataSetGroupedByChangeRequest =
                dataSetList.stream()
                        .collect(Collectors.groupingBy(DataSet::getChangeRequestId));

        Utils.writeTestRequestInfo(dataSetGroupedByChangeRequest, fileName);
    }

    /**
     * This method will perform codynaqSingle using parallel strem in order to improve execution performance
     *
     * @param testDataSet the dataset for performing codynaqSingle algorithm
     * @return {@code Map} which will contain change request ID as key and list of corresponding dataset as value
     */
    private static Map<Integer, List<DataSet>> performCodynaqSingleParallel(List<DataSet> testDataSet) {
        Map<Integer, List<DataSet>> testDataSetGroupByChangeRequestId = testDataSet.stream()
                .collect(Collectors.groupingBy(DataSet::getChangeRequestId));

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

    /**
     * This method calculates reprioritization of dataset based on priority score
     *
     * @param dispatchQueue
     * @param finishedTestSuiteList
     * @return prioritizedList
     */
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

    private static void saveDistinctTestSuiteList(List<DataSet> dataSetList, String fileName) {
        List<String> distinctTestSuiteList = dataSetList.stream()
                .map(DataSet::getTestSuite)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Utils.writeDistinctTestSuiteList(distinctTestSuiteList, fileName);
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
