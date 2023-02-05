package ru.yandex.tanker.gradle.android;

import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class GradleTankerTaskTest {

    @Test
    public void testGetCommonPathPrefixSize() throws Exception {
        String sep = File.separator;
        assertEquals(
                4, GradleTankerTask.getCommonPathPrefixSize(
                        "rrr" + sep + "tt" + sep + "gg",
                        "rrr" + sep + "hh" + sep + "ee")
        );
        assertEquals(
                0, GradleTankerTask.getCommonPathPrefixSize(
                        "rrr",
                        "rrr")
        );
        assertEquals(
                4, GradleTankerTask.getCommonPathPrefixSize(
                        "rrr" + sep + "tt1" + sep + "gg",
                        "rrr" + sep + "tt2" + sep + "ee")
        );
        assertEquals(
                4, GradleTankerTask.getCommonPathPrefixSize(
                        "rrr" + sep,
                        "rrr" + sep)
        );
        assertEquals(
                1, GradleTankerTask.getCommonPathPrefixSize(
                        sep + "rrr",
                        sep + "rrr")
        );
        assertEquals(
                1, GradleTankerTask.getCommonPathPrefixSize(
                        sep + "rrr",
                        sep + "rrr1")
        );
        assertEquals(
                1, GradleTankerTask.getCommonPathPrefixSize(
                        sep + "rr1",
                        sep + "rrr")
        );
        assertEquals(
                1, GradleTankerTask.getCommonPathPrefixSize(
                        sep + "rrr",
                        sep + "rrr2")
        );
        assertEquals(
                0, GradleTankerTask.getCommonPathPrefixSize(
                        "rrr",
                        "rrr2")
        );
    }

    @Test
    public void testFindResPath() {
        Set<File> files = new HashSet<>();
        String sep = File.separator;
        files.add(new File(sep + "rrr" + sep + "res"));
        files.add(new File(sep + "main" + sep + "res"));
        files.add(new File(sep + "kkk" + sep + "res"));
        assertEquals(
                sep + "main" + sep + "res",
                GradleTankerTask.findResPath(files, "main")
        );

    }
}
