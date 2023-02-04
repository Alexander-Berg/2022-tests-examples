package ru.yandex.whitespirit.it_tests.templates;

import lombok.Value;

@Value
public class VariableDocumentData {
    String dt;
    int id;
    long fp;
    String fpBinary;
    String rnm;
    int shift;

    public String dtRaw() {
        return dt.replace(' ', 'T') + 'Z';
    }
}

