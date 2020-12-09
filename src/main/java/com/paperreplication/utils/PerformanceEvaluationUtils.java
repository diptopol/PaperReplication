package com.paperreplication.utils;

import com.paperreplication.entity.DataSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author Diptopol
 * @since 12/4/2020 6:58 PM
 */
public class PerformanceEvaluationUtils {
    private static Logger logger = LoggerFactory.getLogger(PerformanceEvaluationUtils.class);

    public static double getPercentageDelay(Map<Integer, List<DataSet>> fifoResult,
                                             Map<Integer, List<DataSet>> codynaqSingleResult) {

        List<Double> delayList = new ArrayList<>();
        for (Map.Entry<Integer, List<DataSet>> entry : codynaqSingleResult.entrySet()) {

            int changeRequestId = entry.getKey();
            List<DataSet> codynaqSingleList = entry.getValue();
            List<DataSet> fifoList = fifoResult.get(changeRequestId);
            long totalFailedCount = fifoList.stream().filter(dataSet -> dataSet.getStatus().equals("FAILED")).count();

            int countOfDelay = 0;
            for (int i = 0; i < codynaqSingleList.size(); i++) {
                DataSet dataSet = codynaqSingleList.get(i);

                if (dataSet.getStatus().equals("FAILED")) {
                    int indexOfFifo = IntStream.range(0, fifoList.size())
                            .filter(j -> fifoList.get(j).getTestSuite().equals(dataSet.getTestSuite()))
                            .findFirst()
                            .orElse(-1);

                    if (indexOfFifo - i < 0) {
                        countOfDelay++;
                    }
                }
            }

            delayList.add(totalFailedCount != 0 ? ((double) countOfDelay) / totalFailedCount : 0);
        }

        logger.info("Number of change request with at-least one Delay: {}",
                delayList.stream().filter(d -> d > 0).count());

        return Utils.getMedian(delayList);
    }

    public static double getPercentageDelayWithAtLeastOneFailure(Map<Integer, List<DataSet>> fifoResult,
                                                                 Map<Integer, List<DataSet>> codynaqSingleResult) {

        List<Double> delayList = new ArrayList<>();

        for (Map.Entry<Integer, List<DataSet>> entry : codynaqSingleResult.entrySet()) {
            int changeRequestId = entry.getKey();

            List<DataSet> fifoList = fifoResult.get(changeRequestId);
            List<DataSet> codynaqSingleList = entry.getValue();
            long totalFailedCount = fifoList.stream().filter(dataSet -> dataSet.getStatus().equals("FAILED")).count();

            int countOfDelay = 0;
            boolean hasAtleastOneFailure = false;
            for (int i = 0; i < codynaqSingleList.size(); i++) {
                DataSet dataSet = codynaqSingleList.get(i);

                if (dataSet.getStatus().equals("FAILED")) {
                    hasAtleastOneFailure = true;
                    int indexOfFifo = IntStream.range(0, fifoList.size())
                            .filter(j -> fifoList.get(j).getTestSuite().equals(dataSet.getTestSuite()))
                            .findFirst()
                            .orElse(-1);

                    if (indexOfFifo - i < 0) {
                        countOfDelay++;
                    }
                }
            }

            if (hasAtleastOneFailure) {
                delayList.add(totalFailedCount != 0 ? ((double) countOfDelay) / totalFailedCount : 0);
            }
        }

        return Utils.getMedian(delayList);
    }

    public static double getMedianGainAllFail(Map<Integer, List<DataSet>> fifoResult,
                                                                   Map<Integer, List<DataSet>> codynaqSingleResult) {

        List<Double> gain = new ArrayList<>();

        for (Map.Entry<Integer, List<DataSet>> entry : codynaqSingleResult.entrySet()) {
            int changeRequestId = entry.getKey();

            List<DataSet> fifoList = fifoResult.get(changeRequestId);
            List<DataSet> codynaqSingleList = entry.getValue();

            long totalFailedCount = fifoList.stream().filter(dataSet -> dataSet.getStatus().equals("FAILED")).count();
            long failCount = 0;
            double waitingTimeFifo = 0;
            for (DataSet dataSet : fifoList) {
                if (dataSet.getStatus().equals("FAILED")) {
                    failCount++;
                }

                waitingTimeFifo += dataSet.getExecutionTimeInMilliSeconds();

                if (failCount == totalFailedCount) {
                    break;
                }
            }

            failCount = 0;
            double waitingTimeCodynaqSingle = 0;
            for (DataSet dataSet : codynaqSingleList) {
                if (dataSet.getStatus().equals("FAILED")) {
                    failCount++;
                }

                waitingTimeCodynaqSingle += dataSet.getExecutionTimeInMilliSeconds();

                if (failCount == totalFailedCount) {
                    break;
                }
            }

            gain.add((waitingTimeFifo - waitingTimeCodynaqSingle) / waitingTimeFifo);
        }

        return Utils.getMedian(gain);
    }

    public static double getMedianGainAllFailWithAtLeastOneFailure(Map<Integer, List<DataSet>> fifoResult,
                                                                   Map<Integer, List<DataSet>> codynaqSingleResult) {

        List<Double> gain = new ArrayList<>();

        for (Map.Entry<Integer, List<DataSet>> entry : codynaqSingleResult.entrySet()) {
            int changeRequestId = entry.getKey();

            List<DataSet> fifoList = fifoResult.get(changeRequestId);
            List<DataSet> codynaqSingleList = entry.getValue();

            long totalFailedCount = fifoList.stream().filter(dataSet -> dataSet.getStatus().equals("FAILED")).count();
            long failCount = 0;
            double waitingTimeFifo = 0;
            for (DataSet dataSet : fifoList) {
                if (dataSet.getStatus().equals("FAILED")) {
                    failCount++;
                }

                waitingTimeFifo += dataSet.getExecutionTimeInMilliSeconds();

                if (failCount == totalFailedCount) {
                    break;
                }
            }

            failCount = 0;
            boolean hasAtleastOneFailure = false;
            double waitingTimeCodynaqSingle = 0;
            for (DataSet dataSet : codynaqSingleList) {
                if (dataSet.getStatus().equals("FAILED")) {
                    hasAtleastOneFailure = true;
                    failCount++;
                }

                waitingTimeCodynaqSingle += dataSet.getExecutionTimeInMilliSeconds();

                if (failCount == totalFailedCount) {
                    break;
                }
            }

            if (hasAtleastOneFailure) {
                gain.add((waitingTimeFifo - waitingTimeCodynaqSingle) / waitingTimeFifo);
            }
        }

        return Utils.getMedian(gain);
    }

    public static double getMedianGainFirstFailure(Map<Integer, List<DataSet>> fifoResult,
                                                   Map<Integer, List<DataSet>> codynaqSingleResult) {

        List<Double> diffList = new ArrayList<>();

        for (Map.Entry<Integer, List<DataSet>> entry : codynaqSingleResult.entrySet()) {
            int changeRequestId = entry.getKey();

            List<DataSet> fifoList = fifoResult.get(changeRequestId);
            List<DataSet> codynaqSingleList = entry.getValue();

            double waitingTimeFifo = 0;
            for (DataSet dataSet : fifoList) {
                if (dataSet.getStatus().equals("FAILED")) {
                    waitingTimeFifo += dataSet.getExecutionTimeInMilliSeconds();
                    break;
                } else {
                    waitingTimeFifo += dataSet.getExecutionTimeInMilliSeconds();
                }
            }

            double waitingTimeCodynaqSingle = 0;
            for (DataSet dataSet : codynaqSingleList) {
                if (dataSet.getStatus().equals("FAILED")) {
                    waitingTimeCodynaqSingle += dataSet.getExecutionTimeInMilliSeconds();
                    break;
                } else {
                    waitingTimeCodynaqSingle += dataSet.getExecutionTimeInMilliSeconds();
                }
            }

            diffList.add((waitingTimeFifo - waitingTimeCodynaqSingle) / waitingTimeFifo);
        }

        return Utils.getMedian(diffList);
    }

    public static double getMedianGainFirstFailAtLeastOneFailure(Map<Integer, List<DataSet>> fifoResult,
                                                                  Map<Integer, List<DataSet>> codynaqSingleResult) {
        List<Double> diffList = new ArrayList<>();

        for (Map.Entry<Integer, List<DataSet>> entry : codynaqSingleResult.entrySet()) {
            int changeRequestId = entry.getKey();

            List<DataSet> fifoList = fifoResult.get(changeRequestId);
            List<DataSet> codynaqSingleList = entry.getValue();

            double waitingTimeFifo = 0;
            boolean hasAtleastOneFail = false;
            for (DataSet dataSet : fifoList) {
                if (dataSet.getStatus().equals("FAILED")) {
                    hasAtleastOneFail = true;
                    waitingTimeFifo += dataSet.getExecutionTimeInMilliSeconds();
                    break;
                } else {
                    waitingTimeFifo += dataSet.getExecutionTimeInMilliSeconds();
                }
            }

            double waitingTimeCodynaqSingle = 0;
            for (DataSet dataSet : codynaqSingleList) {
                if (dataSet.getStatus().equals("FAILED")) {
                    waitingTimeCodynaqSingle += dataSet.getExecutionTimeInMilliSeconds();
                    break;
                } else {
                    waitingTimeCodynaqSingle += dataSet.getExecutionTimeInMilliSeconds();
                }
            }

            if (hasAtleastOneFail) {
                diffList.add((waitingTimeFifo - waitingTimeCodynaqSingle) / waitingTimeFifo);
            }
        }

        return Utils.getMedian(diffList);
    }


}
