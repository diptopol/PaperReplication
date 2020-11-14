package com.paperreplication.entity;

import org.apache.commons.csv.CSVRecord;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;

/**
 * @author Urbashi
 * @since 11/13/2020 7:21 PM
 */
public class DataSet {

    private String testSuite;

    private int changeRequestId;

    //pres, post
    private String stage;

    //PASSED, FAILED
    private String status;

    private LocalDateTime launchTime;

    private double executionTimeInMilliSeconds;

    //SMALL, MEDIUM, LARGE
    private String size;

    private int shardNumber;

    private int runNumber;

    private String language;

    public DataSet(CSVRecord record) {
        this.testSuite = record.get(0);
        this.changeRequestId = Integer.valueOf(record.get(1));
        this.stage = record.get(2);
        this.status = record.get(3);

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .toFormatter();

        //2014-01-01 00:00:13.672
        this.launchTime = LocalDateTime.parse(record.get(4), formatter);
        this.executionTimeInMilliSeconds = Double.valueOf(record.get(5));
        this.size = record.get(6);
        this.shardNumber = Integer.valueOf(record.get(7));
        this.runNumber = Integer.valueOf(record.get(8));
        this.language = record.get(9);
    }

    public String getTestSuite() {
        return testSuite;
    }

    public int getChangeRequestId() {
        return changeRequestId;
    }

    public String getStage() {
        return stage;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getLaunchTime() {
        return launchTime;
    }

    public double getExecutionTimeInMilliSeconds() {
        return executionTimeInMilliSeconds;
    }

    public String getSize() {
        return size;
    }

    public int getShardNumber() {
        return shardNumber;
    }

    public int getRunNumber() {
        return runNumber;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "DataSet{" +
                "testSuite='" + testSuite + '\'' +
                ", changeRequestId=" + changeRequestId +
                ", stage='" + stage + '\'' +
                ", status='" + status + '\'' +
                ", launchTime=" + launchTime +
                ", executionTimeInMilliSeconds=" + executionTimeInMilliSeconds +
                ", size='" + size + '\'' +
                ", shardNumber=" + shardNumber +
                ", runNumber=" + runNumber +
                ", language='" + language + '\'' +
                '}';
    }
}
