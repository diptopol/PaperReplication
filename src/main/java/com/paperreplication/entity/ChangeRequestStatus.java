package com.paperreplication.entity;

import java.util.Objects;

/**
 * @author Diptopol
 * @since 11/14/2020 7:29 PM
 */
public class ChangeRequestStatus {

    public static final String[] HEADERS = {"changeRequestId", "passCount", "failCount"};

    private int changeRequestId;

    private long passCount;

    private long failCount;

    public ChangeRequestStatus(int changeRequestId, long passCount, long failCount) {
        this.changeRequestId = changeRequestId;
        this.passCount = passCount;
        this.failCount = failCount;
    }

    public int getChangeRequestId() {
        return changeRequestId;
    }

    public long getPassCount() {
        return passCount;
    }

    public long getFailCount() {
        return failCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeRequestStatus that = (ChangeRequestStatus) o;
        return changeRequestId == that.changeRequestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(changeRequestId);
    }
}
