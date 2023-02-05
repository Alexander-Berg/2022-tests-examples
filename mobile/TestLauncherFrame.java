package com.yandexlauncher.performance;

import java.io.IOException;

import static com.yandexlauncher.performance.Helper.runAdb;
import static com.yandexlauncher.performance.Main.*;
import static com.yandexlauncher.performance.Utils.*;

class TestLauncherFrame {

    private static int[][] allAppsOpenByClick;
    private static int[][] allAppsClose;
    private static int[][] shtorkaOpen;
    private static int[][] shtorkaClose;
    private static int[][] widgetOpen;
    private static int[][] widgetClose;
    private static int[][] folderOpen;
    private static int[][] folderClose;
    private static float dpX;
    private static float dpY;
    private static String metrica1;
    private static String metrica2;
    private static int[][] arrayMetrica1;
    private static int[][] arrayMetrica2;
    private static String nameForWikiMetrica1;
    private static String nameForWikiMetrica2;

    private static void inputTapPX(float dpX, float dpY) throws IOException, InterruptedException {
//        float x = Utils.dpToPx(dpX);
//        float y = Utils.dpToPx(dpY);
        Utils.pressInputTapPX(dpX, dpY);
    }


    static void testAllApps() throws IOException, InterruptedException {
        System.out.println(model+" start test allaps");
        allAppsOpenByClick = new int[repeat][];
        allAppsClose = new int[repeat][];
        dpX = phone.AllApsdpX;
        dpY = phone.AllApsdpY;
        String metric1 = "OpenByClick";
        String metric2 = "Close";
        String wiki1 = "AllAps Open. Lost Frames";
        String wiki2 = "AllAps Close. Lost Frames";
        pressByDp(dpX, dpY, metric1, metric2, allAppsOpenByClick, allAppsClose, wiki1, wiki2);
        Thread.sleep(3000);
    }

    static void testFolder() throws IOException, InterruptedException {
        System.out.println(model+" start test folder");
        folderOpen = new int[repeat][];
        folderClose = new int[repeat][];
        dpX = phone.FolderdpX;
        dpY = phone.FolderdpY;
        String metric1 = "Folder.Open";
        String metric2 = "Folder.Close";
        String wiki1 = "Folder Open. Lost Frames";
        String wiki2 = "Folder Close. Lost Frames";
        pressByDp(dpX, dpY, metric1, metric2, folderOpen, folderClose, wiki1, wiki2);
        Thread.sleep(3000);
    }

    static void testSearchShtorka() throws IOException, InterruptedException {
        System.out.println(model+" start test shtorka");
        shtorkaOpen = new int[repeat][];
        shtorkaClose = new int[repeat][];
        dpX = phone.ShtorkadpX;
        dpY = phone.ShtorkadpY;
        String metric1 = "Search.OpenedByClick";
        String metric2 = "Search.Close";
        String wiki1 = "Search Shtorka Open. Lost Frames: ";
        String wiki2 = "Search Shtorka Close. Lost Frames:";

        pressByDp(dpX, dpY, metric1, metric2, shtorkaOpen, shtorkaClose, wiki1, wiki2);

    }

    static void testWidget() throws IOException, InterruptedException {
        System.out.println(model+" start test widget");
        widgetOpen = new int[repeat][];
        widgetClose = new int[repeat][];
        dpX = phone.WidgetdpX;
        dpY = phone.WidgetdpY;
        String metrica1 = "Weather.Open";
        String metrica2 = "Weather.Close";
        String wiki1 = "Widget Open. Lost Frames: ";
        String wiki2 = "Widget Close. Lost Frames: ";
        pressByDp(dpX, dpY, metrica1, metrica2, widgetOpen, widgetClose, wiki1, wiki2);
        Thread.sleep(3000);
    }

    /**
     * A pressByDp method that uses coordinates to interact with an object
     *
     * @param dpX                 DP X axis
     * @param dpY                 DP Y axis
     * @param metric1            The line in the log that reports the opening of the object
     * @param metric2            The line in the log that reports the close of the object
     * @param arrayMetrica1       Array for  metric1
     * @param arrayMetrica2       Array for metric2
     * @param nameForWikiMetrica1 A name for the table Wiki for metric1
     * @param nameForWikiMetrica2 A name for the table Wiki for metric2
     * @throws IOException IOException IOException
     * @throws InterruptedException InterruptedException
     */
    private static void pressByDp(float dpX, float dpY, String metric1, String metric2, int[][] arrayMetrica1, int[][] arrayMetrica2, String nameForWikiMetrica1, String nameForWikiMetrica2) throws IOException, InterruptedException {
        runAdb("logcat -c", 500);
        TestLauncherFrame.dpX = dpX;
        TestLauncherFrame.dpY = dpY;
        TestLauncherFrame.metrica1 = metric1;
        TestLauncherFrame.metrica2 = metric2;
        TestLauncherFrame.arrayMetrica1 = arrayMetrica1;
        TestLauncherFrame.arrayMetrica2 = arrayMetrica2;
        TestLauncherFrame.nameForWikiMetrica1 = nameForWikiMetrica1;
        TestLauncherFrame.nameForWikiMetrica2 = nameForWikiMetrica2;
        System.out.println(dpX);
        System.out.println(dpY);
        for (int run = 0; run < repeat; run++) {
            System.out.printf("Launch count - %d%n", run);
            inputTapPX(dpX, dpY);
            String log=pressHome(metric1);
            pressBack(1);
            log +=pressHome(metric2);
            String[] logs = logHistograms(log);

            for (int i = 0; i < logs.length; i++) {
                if (logs[i].contains(metric1)) {
                    String logJsonOpen = logs[i + 1].substring(logs[i + 1].lastIndexOf("Values: ") + "Values: ".length()).trim();
                    int[] arrayFramesOpen = Helper.jsonToIntArray(logJsonOpen);
                    arrayMetrica1[run] = arrayFramesOpen;
                }
                if (logs[i].contains(metric2)) {
                    String logJsonClose = logs[i + 1].substring(logs[i + 1].lastIndexOf("Values: ") + "Values: ".length()).trim();
                    int[] arrayFrameClose = Helper.jsonToIntArray(logJsonClose);
                    arrayMetrica2[run] = arrayFrameClose;
                }
            }
            runAdb("logcat -c", 500);
        }
        if (arrayMetrica1[0] == null || arrayMetrica1[0].length == 0) {
            throw new NullPointerException(metric1 + " is empty ");
        }
        if (arrayMetrica2[0] == null || arrayMetrica2[0].length == 0) {
            throw new NullPointerException(metric2 + " is empty ");
        }
        String lostopen = String.format("%.2f", Helper.lostFrame(arrayMetrica1));
        int[] lostopenstat = Helper.lostFrameStatics(arrayMetrica1);
        String lostclose = String.format("%.2f", Helper.lostFrame(arrayMetrica2));
        int[] lostclosenstat = Helper.lostFrameStatics(arrayMetrica2);
        String str = (System.lineSeparator() +
            "#|" + System.lineSeparator() +
            "||" + nameForWikiMetrica1 + "|" + lostopen + "%" + "||" + System.lineSeparator() +
            "||Count "+frameMs+"-"+frameMs*2+"|"+lostopenstat[0]+"|Count "+frameMs*2+"-"+frameMs*3+"|"+lostopenstat[1]+"|Count "+frameMs*3+"-..."+"|"+lostopenstat[2]+"||"+System.lineSeparator()+
            "||" + nameForWikiMetrica2 + "|" + lostclose + "%" + "||" + System.lineSeparator() +
            "||Count "+frameMs+"-"+frameMs*2+"|"+lostclosenstat[0]+"|Count "+frameMs*2+"-"+frameMs*3+"|"+lostclosenstat[1]+"|Count "+frameMs*3+"-..."+"|"+lostclosenstat[2]+"||"+System.lineSeparator()+
            "|#" + System.lineSeparator()
        );
        Helper.writeUsingFiles(str);
        System.out.println();
        System.out.println();
        System.out.println("Average " + nameForWikiMetrica1 + " " + lostopen);
        System.out.println("Average " + nameForWikiMetrica2 + " " + lostclose);
        System.out.println("Finish.");
    }

}
