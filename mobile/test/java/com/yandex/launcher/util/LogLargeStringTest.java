package com.yandex.launcher.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by alex-garmash on 22.06.17.
 */

public class LogLargeStringTest {

    String result;

    @Before
    public void prepare() {
        result = "";
    }

    @Test
    public void test_LogLarge(){
        String[] target = new String[]{
            "",
            "a",
            "ab",
            "abc",
            "abcd",
            "absdefgh", // even number
            "absdefghi", // odd number
            "12345678912345678", // amount of elements is simple number, 17 in this case
            //real json
            "{\"device\":\"sailfish\",\"firstLaunchDate\":\"2017-06-22_1636+0300\",\"installDate\":\"2017-06-22_1635+0300\",\"sdk\":\"25\",\"carrier\":\"\",\"deviceFingerPrintId\":\"ffffffff-fc8f-d4f2-ffff-ffffd9e6d218\",\"date1\":\"2017-06-22_1635+0300\",\"af_preinstalled\":\"false\",\"advertiserIdEnabled\":\"true\",\"iaecounter\":\"0\",\"lang_code\":\"ru\",\"app_version_name\":\"2.0.1\",\"lang\":\"русский\",\"af_google_instance_id\":\"f3P5nxzRYFw\",\"timepassedsincelastlaunch\":\"264\",\"dkh\":\"2NSKGjzv\",\"advertiserId\":\"8ea9551e-5463-4d22-81b3-c63ada13334c\",\"isGaidWithGps\":\"true\",\"deviceType\":\"user\",\"af_v\":\"3aee9ef53e210ff1066f615b4d7e994e7129635e\",\"app_version_code\":\"2147483647\",\"af_events_api\":\"1\",\"network\":\"WIFI\",\"platformextension\":\"android_native\",\"operator\":\"\",\"country\":\"RU\",\"date2\":\"2017-06-22_1640+0300\",\"brand\":\"google\",\"af_timestamp\":\"1498138828660\",\"uid\":\"1498138557601-3362096936075683518\",\"isFirstCall\":\"false\",\"counter\":\"2\",\"model\":\"Pixel\",\"product\":\"sailfish\"}"
        };

        final int EMPTY_STRING_INDEX = 0;
        final int ONE_SYMBOL_STRING_INDEX = 1;
        final int TWO_SYMBOL_STRING_INDEX = 2;
        final int THREE_SYMBOL_STRING_INDEX = 3;
        final int FOUR_SYMBOL_STRING_INDEX = 4;
        final int EVEN_SYMBOL_STRING_INDEX = 5;
        final int ODD_SYMBOL_STRING_INDEX = 6;
        final int SIMPLE_SYMBOL_STRING_INDEX = 7;
        final int REAL_JSON_STRING_INDEX = 8;

        final int MAX_LENGTH_ONE = 1;
        final int MAX_LENGTH_TWO = 2;
        final int MAX_LENGTH_THREE = 3;
        final int MAX_LENGTH_BIG = 100;

        final int STRING_IND_INDEX = 0;
        final int MAX_LENGTH_INDEX = 1;

        int[][] MAX_LENGTH_TESTS = new int[][] {
            new int[]{EMPTY_STRING_INDEX, MAX_LENGTH_ONE},
            new int[]{EMPTY_STRING_INDEX, MAX_LENGTH_TWO},
            new int[]{EMPTY_STRING_INDEX, MAX_LENGTH_THREE},
            new int[]{EMPTY_STRING_INDEX, MAX_LENGTH_BIG},

            new int[]{ONE_SYMBOL_STRING_INDEX, MAX_LENGTH_ONE},
            new int[]{ONE_SYMBOL_STRING_INDEX, MAX_LENGTH_TWO},
            new int[]{ONE_SYMBOL_STRING_INDEX, MAX_LENGTH_THREE},
            new int[]{ONE_SYMBOL_STRING_INDEX, MAX_LENGTH_BIG},

            new int[]{TWO_SYMBOL_STRING_INDEX, MAX_LENGTH_ONE},
            new int[]{TWO_SYMBOL_STRING_INDEX, MAX_LENGTH_TWO},
            new int[]{TWO_SYMBOL_STRING_INDEX, MAX_LENGTH_THREE},
            new int[]{TWO_SYMBOL_STRING_INDEX, MAX_LENGTH_BIG},

            new int[]{THREE_SYMBOL_STRING_INDEX, MAX_LENGTH_ONE},
            new int[]{THREE_SYMBOL_STRING_INDEX, MAX_LENGTH_TWO},
            new int[]{THREE_SYMBOL_STRING_INDEX, MAX_LENGTH_THREE},
            new int[]{THREE_SYMBOL_STRING_INDEX, MAX_LENGTH_BIG},

            new int[]{FOUR_SYMBOL_STRING_INDEX, MAX_LENGTH_ONE},
            new int[]{FOUR_SYMBOL_STRING_INDEX, MAX_LENGTH_TWO},
            new int[]{FOUR_SYMBOL_STRING_INDEX, MAX_LENGTH_THREE},
            new int[]{FOUR_SYMBOL_STRING_INDEX, MAX_LENGTH_BIG},

            new int[]{EVEN_SYMBOL_STRING_INDEX, MAX_LENGTH_ONE},
            new int[]{EVEN_SYMBOL_STRING_INDEX, MAX_LENGTH_TWO},
            new int[]{EVEN_SYMBOL_STRING_INDEX, MAX_LENGTH_THREE},
            new int[]{EVEN_SYMBOL_STRING_INDEX, MAX_LENGTH_BIG},

            new int[]{ODD_SYMBOL_STRING_INDEX, MAX_LENGTH_ONE},
            new int[]{ODD_SYMBOL_STRING_INDEX, MAX_LENGTH_TWO},
            new int[]{ODD_SYMBOL_STRING_INDEX, MAX_LENGTH_THREE},
            new int[]{ODD_SYMBOL_STRING_INDEX, MAX_LENGTH_BIG},

            new int[]{SIMPLE_SYMBOL_STRING_INDEX, MAX_LENGTH_ONE},
            new int[]{SIMPLE_SYMBOL_STRING_INDEX, MAX_LENGTH_TWO},
            new int[]{SIMPLE_SYMBOL_STRING_INDEX, MAX_LENGTH_THREE},
            new int[]{SIMPLE_SYMBOL_STRING_INDEX, MAX_LENGTH_BIG},

            new int[]{REAL_JSON_STRING_INDEX, MAX_LENGTH_ONE},
            new int[]{REAL_JSON_STRING_INDEX, MAX_LENGTH_TWO},
            new int[]{REAL_JSON_STRING_INDEX, MAX_LENGTH_THREE},
            new int[]{REAL_JSON_STRING_INDEX, MAX_LENGTH_BIG}
        };
        
        StringBuilder sb = null;

        int i;
        for (i = 0; i < MAX_LENGTH_TESTS.length; i++) {
            sb = new StringBuilder();
            result = "";
            int[] testValues = MAX_LENGTH_TESTS[i];
            final String targetString = target[testValues[STRING_IND_INDEX]];
            final int maxLength = testValues[MAX_LENGTH_INDEX];

            sb.append(targetString);
            logLarge(sb, maxLength);
            assertEquals(sb.toString(), result);
        }
        System.out.println("Amount of tests: " + i);
    }


    private void logLarge(StringBuilder sb, final int maxLength) {
        int count = 0;
        while (count < sb.length()) {
            String substring = sb.substring(count, count + Math.min(maxLength, sb.length() - count));
            logD(substring);
            count += substring.length();
        }
    }

    public void logD(String msg) {
        result += msg;
    }
}
