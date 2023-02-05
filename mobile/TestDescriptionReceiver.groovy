package com.yandex.frankenstein.description.receivers


import groovy.transform.CompileStatic

import java.util.function.Consumer

@CompileStatic
class TestDescriptionReceiver implements Consumer<String> {

    final DescriptionLogReceiver mLogReceiver
    final File mTestInfoFile

    TestDescriptionReceiver(final File testInfoFile) {
        this(testInfoFile, new DescriptionLogReceiver(DescriptionLogReceiver.TEST_DESCRIPTION_TAG))
    }

    protected TestDescriptionReceiver(final File testInfoFile, final DescriptionLogReceiver logReceiver) {
        mTestInfoFile = testInfoFile
        mLogReceiver = logReceiver
    }

    @Override
    void accept(final String s) {
        final String description = mLogReceiver.apply(s)
        mTestInfoFile << description
    }
}