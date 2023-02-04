package ru.yandex.vertis;

public final class RerunConstant {

    public static final String RUN_TYPE = "surefireRerunFailed";

    public static final String SERVER_DISPLAY_NAME = "Surefire Rerun Failed Tests";


    public static final String MAVEN_RUNNER = "Maven2";

    public static final String TEST_GOAL = "test";


    public static final String RERUN_PROPERTY = "rerun.test.property";

    public static final String BUILD_NUMBER_PROPERTY = "rerun.test.build";


    public static final String TC_HIDDEN_DIR = ".teamcity";

    public static final String FAILED = "failed.txt";

    public static final String OLD_FAILED = "old-" + FAILED;

    public static final String TEAMCITY_FAILED_ARTIFACT_PATH = TC_HIDDEN_DIR + "/" + FAILED;

}
