package ru.yandex.realty.searcher;

import org.junit.Test;

import java.io.*;

/**
 * @author: berkut
 */
public class GlueQueryAndRegion {

    public static void main(String[] args) throws IOException {
        BufferedReader br1 = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/berkut/tmp/realty/queries.log")));
        BufferedReader br2 = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/berkut/tmp/realty/geoaddr_response.out")));
        PrintWriter out = new PrintWriter("/Users/berkut/tmp/realty/glued.log");
        String input1;
        while ((input1 = br1.readLine()) != null) {
            String input2 = br2.readLine();
            String[] queryAndRegion = input1.split(" # ");
            String[] queryAndGeoaddr = input2.split(" # ");
            if (!queryAndGeoaddr[0].equals(queryAndRegion[0])) {
                System.err.println("Queries differs");
                return;
            }
            out.println(queryAndGeoaddr[0] + " # " + queryAndGeoaddr[1] + " # " + queryAndRegion[1]);
        }
    }

}
