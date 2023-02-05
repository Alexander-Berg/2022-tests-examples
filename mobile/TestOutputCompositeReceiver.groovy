package com.yandex.frankenstein.description

import groovy.transform.CompileStatic

import java.util.function.Consumer

@CompileStatic
class TestOutputCompositeReceiver implements Consumer<String> {

    private final List<Consumer<String>> receivers = []

    void addReceiver(final Consumer<String> testDescriptionReceiver) {
        receivers.add(testDescriptionReceiver)
    }

    @Override
    void accept(final String message) {
        receivers.each { it.accept(message) }
    }
}