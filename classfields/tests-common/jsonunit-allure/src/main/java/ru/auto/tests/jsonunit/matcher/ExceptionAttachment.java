package ru.auto.tests.jsonunit.matcher;

import io.qameta.allure.attachment.AttachmentData;

public class ExceptionAttachment implements AttachmentData {

    private String differences;

    public ExceptionAttachment(String differences) {
        this.differences = differences;
    }

    @Override
    public String getName() {
        return "Exception";
    }

    public String getDifferences() {
        return differences;
    }
}
