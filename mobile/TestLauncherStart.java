package com.yandexlauncher.performance;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.yandexlauncher.performance.Helper.runAdb;
import static com.yandexlauncher.performance.Helper.writeUsingFiles;
import static com.yandexlauncher.performance.Main.print;

class TestLauncherStart {


    private String applicationBeforeOnCreate = "Application.Before_onCreate duration:";
    private String applicationOnCreate = "Application.onCreate duration:";
    private String activityOnCreate = "Activity.onCreate duration:";
    private String modelBindFirstScreen = "Model.BindFirstScreen duration:";
    private String workspaceLoad = "Workspace.Load duration:";
    private String modelBindWorkspace = "Model.BindWorkspace duration:";
    private String activityBindFirstScreen = "Activity.BindFirstScreen duration:";
    private String activityOnResume = "Activity.onResume duration:";
    private String applicationLaunchTime = "Application.LaunchTime duration:";
    private String aliceInitialize = "Alice.Initialize duration:";

    private List<Integer> applicationBeforeOnCreateList = new ArrayList<>();
    private List<Integer> applicationOnCreateList = new ArrayList<>();
    private List<Integer> activityOnCreateList = new ArrayList<>();
    private List<Integer> modelBindFirstScreenList = new ArrayList<>();
    private List<Integer> workspaceLoadList = new ArrayList<>();
    private List<Integer> modelBindWorkspaceList = new ArrayList<>();
    private List<Integer> activityBindFirstScreenList = new ArrayList<>();
    private List<Integer> activityOnResumeList = new ArrayList<>();
    private List<Integer> applicationLaunchTimeList = new ArrayList<>();
    private List<Integer> aliceInitializeList = new ArrayList<>();

    /**
     * Average int.
     * Return average of List Integer
     *
     * @param list List Integer
     * @return averege is List
     */
    private static int average(List<Integer> list) {
        int sum = 0;
        for (Integer aList : list) {
            sum += aList;
        }
        return list.size() != 0 ? sum / list.size() : 0;
    }

    /**
     * The method for finding the median of numbers, the parameter is an array
     *
     * @param arrayForFindingTheMedianOfNumber array numbers
     * @return median of numbers
     */
    private static float getMedianOfNumber(int[] arrayForFindingTheMedianOfNumber) {
        Arrays.sort(arrayForFindingTheMedianOfNumber);
        if (arrayForFindingTheMedianOfNumber.length % 2 == 0) {
            return ((arrayForFindingTheMedianOfNumber[arrayForFindingTheMedianOfNumber.length / 2] + arrayForFindingTheMedianOfNumber[arrayForFindingTheMedianOfNumber.length / 2 - 1]) / 2f);
        }
        return arrayForFindingTheMedianOfNumber[arrayForFindingTheMedianOfNumber.length / 2];
    }

    void testing() throws IOException, InterruptedException {
        Main.log = "";
        Utils.initDevice();
        for (int i = 0; i < Main.repeat; i++) {
            System.out.println("Launch count - "+i);
            Utils.forceStop();
            System.out.println("Done Stop");
            Thread.sleep(5000);
            Utils.pressHome();
            Thread.sleep(2500);
            Utils.pressHomeLaunch();
            System.out.println("Done Home");
            String s = Utils.logCat();
            Main.log = Main.log.concat(s);
            runAdb("logcat -c", 500);

        }
        System.out.println("LogHistograms");
        Main.histograms = Utils.logHistograms(Main.log);
        parsingLogHistograms(Main.histograms);
        System.out.println("***");
        System.out.println("applicationBeforeOnCreateList "+applicationBeforeOnCreateList.size());
        System.out.println("applicationOnCreateList "+applicationOnCreateList.size());
        System.out.println("activityOnCreateList "+activityOnCreateList.size());
        System.out.println("modelBindFirstScreenList "+modelBindFirstScreenList.size());
        System.out.println("workspaceLoadList "+workspaceLoadList.size());
        System.out.println("modelBindWorkspaceList "+modelBindWorkspaceList.size());
        System.out.println("activityBindFirstScreenList "+activityBindFirstScreenList.size());
        System.out.println("activityOnResumeList "+activityOnResumeList.size());
        System.out.println("applicationLaunchTimeList "+applicationLaunchTimeList.size());
        System.out.println("aliceInitialize "+aliceInitializeList.size());
        System.out.println("Done.");
        Thread.sleep(3000);
    }

    /**
     * Parsing log histograms.
     * Creates a table with start Launcher information from the Wiki
     * Write table in file.
     * @param logs the string [ ] Logs Histograms
     */
    private void parsingLogHistograms(String[] logs) {
        for (String str : logs) {

            if (str.contains(applicationBeforeOnCreate)) {
                String s = str.substring(str.lastIndexOf(applicationBeforeOnCreate) + applicationBeforeOnCreate.length()).trim();
                applicationBeforeOnCreateList.add(Integer.valueOf(s));
            }

            else if (str.contains(applicationOnCreate)) {
                String s = str.substring(str.lastIndexOf(applicationOnCreate) + applicationOnCreate.length()).trim();
                applicationOnCreateList.add(Integer.valueOf(s));
            }

            else if (str.contains(activityOnCreate)) {
                String s = str.substring(str.lastIndexOf(activityOnCreate) + activityOnCreate.length()).trim();
                activityOnCreateList.add(Integer.valueOf(s));
            }

            else if (str.contains(modelBindFirstScreen)) {
                String s = str.substring(str.lastIndexOf(modelBindFirstScreen) + modelBindFirstScreen.length()).trim();
                modelBindFirstScreenList.add(Integer.valueOf(s));
            }

            else if (str.contains(workspaceLoad)) {
                String s = str.substring(str.lastIndexOf(workspaceLoad) + workspaceLoad.length()).trim();
                workspaceLoadList.add(Integer.valueOf(s));
            }

            else if (str.contains(modelBindWorkspace)) {
                String s = str.substring(str.lastIndexOf(modelBindWorkspace) + modelBindWorkspace.length()).trim();
                modelBindWorkspaceList.add(Integer.valueOf(s));
            }

            else if (str.contains(activityBindFirstScreen)) {
                String s = str.substring(str.lastIndexOf(activityBindFirstScreen) + activityBindFirstScreen.length()).trim();
                activityBindFirstScreenList.add(Integer.valueOf(s));
            }

            else if (str.contains(activityOnResume)) {
                String s = str.substring(str.lastIndexOf(activityOnResume) + activityOnResume.length()).trim();
                activityOnResumeList.add(Integer.valueOf(s));
            }

            else if (str.contains(applicationLaunchTime)) {
                String s = str.substring(str.lastIndexOf(applicationLaunchTime) + applicationLaunchTime.length()).trim();
                applicationLaunchTimeList.add(Integer.valueOf(s));
            }

            else if (str.contains(aliceInitialize)) {
                String s = str.substring(str.lastIndexOf(aliceInitialize) + aliceInitialize.length()).trim();
                aliceInitializeList.add(Integer.valueOf(s));
            }

        }
        String wikiTable = getWikiTable();
        writeUsingFiles(wikiTable);
    }

    private String getWikiTable() {
        int[] array = new int[applicationLaunchTimeList.size()];
        for(int i = 0; i < applicationLaunchTimeList.size(); i++) array[i] = applicationLaunchTimeList.get(i);
        Float medianStart = getMedianOfNumber(array);
        return ("#|" + System.lineSeparator() +
              "||Application.Before_onCreate duration:|" + average(applicationBeforeOnCreateList) + "||" + System.lineSeparator() +
              "||Application.onCreate duration:|" + average(applicationOnCreateList) + "||" + System.lineSeparator() +
              "||Activity.onCreate duration:|" + average(activityOnCreateList) + "||" + System.lineSeparator() +
              "||Alice.Initialize duration:|" + average(aliceInitializeList) + "||" + System.lineSeparator() +
              "||Activity.onResume duration:|" + average(activityOnResumeList) + "||" + System.lineSeparator() +
              "||Model.BindFirstScreen duration:|" + average(modelBindFirstScreenList) + "||" + System.lineSeparator() +
              "||Workspace.Load duration:|" + average(workspaceLoadList) + "||" + System.lineSeparator() +
              "||Model.BindWorkspace duration:|" + average(modelBindWorkspaceList) + "||" + System.lineSeparator() +
              "||Activity.BindingFirstScreen duration:|" + average(activityBindFirstScreenList) + "||" + System.lineSeparator() +
              "||Application.LaunchTime duration:|" + average(applicationLaunchTimeList) + "||" + System.lineSeparator() +
//            "||Application.LaunchTime Median:|" + medianStart.toString() + "||" + System.lineSeparator() +
            "|#" + System.lineSeparator()
        );
    }
}
