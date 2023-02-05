package com.yandex.launcher.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LauncherUtilTest {

    @Test
    public void testTransformToReversedPageScrollsDifferentSituations(){
        int[][] targetArrays = new int[][]{
                new int[]{0, 2, 4, 8, 16},      //odd array
                new int[]{0, 2, 4, 8, 16, 32},  //even array
                new int[]{0, 2}                 //2-element array

        };
        int[][] emptyArray = new int[][]{
                new int[5],
                new int[6],
                new int[2]
        };
        int[][] resultArrays = new int[][]{
                new int[]{0, -16, -8, -4, -2},
                new int[]{0, -32, -16, -8, -4, -2},
                new int[]{0, -2}
        };
        for(int arrInd = 0; arrInd < targetArrays.length; arrInd++){
            int[] targetArray = targetArrays[arrInd];
            int[] resultArray = resultArrays[arrInd];
            LauncherUtils.transformToReversedPageScrolls(targetArray, emptyArray[arrInd]);
            for(int i = 0; i < targetArray.length; i++){
                assertEquals(emptyArray[arrInd][i], resultArray[i]);
            }
        }
    }

    //Should be no errors
    @Test
    public void testTransformToReversedPageScrollsNullOrEmptyArray(){
        Exception exc = null;
        int[][] targets = new int[][]{
                new int[0],     //empty
                null            //null
        };
        int[][] emptyArray = new int[][]{
                new int[0],
                new int[0]
        };
        for(int i = 0; i < 2; i++) {
            try {
                LauncherUtils.transformToReversedPageScrolls(targets[i], emptyArray[i]);
            } catch (Exception e) {
                exc = e;
            }
            assertNull(exc);
        }
    }
}
