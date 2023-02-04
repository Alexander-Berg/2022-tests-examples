package ru.auto.tests.publicapi.utils;

public class GrouppingIdParams {
    private String techParamId;
    private String complectationId;

    public GrouppingIdParams(String techParamId, String complectationId) {
        this.techParamId = techParamId;
        this.complectationId = complectationId;
    }

    public String getTechParamId() {
        return techParamId;
    }

    public String getComplectationId() {
        return complectationId;
    }

    public void setTechParamId(String techParamId) {
        this.techParamId = techParamId;
    }

    public void setComplectationId(String complectationId) {
        this.complectationId = complectationId;
    }
}
