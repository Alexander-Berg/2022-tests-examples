package ru.auto.tests.coverage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class CoverageUtils {

    private CoverageUtils() {}

    static Set<String> readRequests(File file) {
        Set<String> result = new HashSet<>();
        try {
            FileInputStream fstream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;

            while ((strLine = br.readLine()) != null) {
                result.add(strLine);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
