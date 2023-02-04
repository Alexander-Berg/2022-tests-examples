package ru.yandex.realty.searcher;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static ru.yandex.common.util.StringUtils.isEmpty;

/**
 * @author: berkut
 */
public class GlueGeoAddrResponse {

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/berkut/tmp/realty/geoaddr_response.log")));
        PrintWriter out = new PrintWriter("/Users/berkut/tmp/realty/geoaddr_response.out");

        String input = br.readLine();

        do {
            String query = input;
            Map<String, String> geoParametersMap = new HashMap<String, String>();
            do {
                input = br.readLine();
                if (input == null) {
                    break;
                }
                String lexemes[] = input.split(" # ");
                if (lexemes.length == 2 && lexemes[1].startsWith("GeoAddr.")) {
                    String lexeme = lexemes[1].substring("GeoAddr.".length());
                    lexeme = lexeme.replaceFirst(": ", " # ");
                    lexemes = lexeme.split(" # ");
                    if (lexemes.length == 2) {
                        if (geoParametersMap.containsKey(lexemes[0].toLowerCase())) {
                            System.err.println("ERROR! we already have such parameter");
                            return;
                        }
                        geoParametersMap.put(lexemes[0].toLowerCase(), stripBrackets(lexemes[1]));
                    }
                }
            } while (!isEmpty(input));
            String geoAddr = "";
            if (geoParametersMap.containsKey("length") &&
                    geoParametersMap.containsKey("body") &&
                    geoParametersMap.containsKey("pos") &&
                    geoParametersMap.containsKey("type")
                    )
            {
                StringBuilder sb = new StringBuilder("[{");
                sb.append("\"length\":\"").append(geoParametersMap.get("length")).append("\",");
                sb.append("\"data\":").append(geoParametersMap.get("body")).append(",");
                sb.append("\"pos\":\"").append(geoParametersMap.get("pos")).append("\",");
                sb.append("\"type\":\"").append(geoParametersMap.get("type")).append("\"");
                sb.append("}]");
                geoAddr = sb.toString();
            }
            out.println(query + " # " + geoAddr);
            input = br.readLine();
        } while (input != null);
        out.close();
    }

    public static String stripBrackets(String string) {
        string = string.substring(1);
        return string.substring(0, string.length() - 1);
    }
}
