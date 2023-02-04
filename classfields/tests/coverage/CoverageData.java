package ru.auto.tests.coverage;

import java.util.Set;

public class CoverageData {

    private Set<String> difference;
    private float percentage;

    public static CoverageData coverageData() {
        return new CoverageData();
    }

    private CoverageData() {
    }

    CoverageData withDifference(Set<String> difference) {
        this.difference = difference;
        return this;
    }

    CoverageData withPercentage(float percentage) {
        this.percentage = percentage;
        return this;
    }

    public float getPercentage() {
        return percentage;
    }

    public Set<String> getDifference() {
        return difference;
    }
}
