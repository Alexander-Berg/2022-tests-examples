package ru.yandex.darkspirit.it_tests.template_classes;

import java.util.ArrayList;

public class LaunchStageInfoField {
    public static class LaunchStageProcessInfoFieldList extends ArrayList<LaunchStageProcessInfoField> {}

    public int success;
    public int failed;
    public int waiting;
    public int finished;
    public int total;
    public boolean locked;
    public LaunchStageProcessInfoFieldList processes_info;
}
