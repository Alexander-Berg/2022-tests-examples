package bsyeti.json.compare;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;


public class Compare {
    protected Compare() {
    }

    String read(String path) throws IOException {
        return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
    }

    boolean isNumber(String val) {
        if (val.length() == 0) {
            return false;
        }
        boolean isNum = true;
        for (var ch : val.toCharArray()) {
            if (ch < '0' || ch > '9') {
                isNum = false;
            }
        }
        return isNum;
    }

    void printStat(TreeMap<String, Integer> stat) {
        for (var entry : stat.entrySet()) {
            System.out.print(entry.getKey());
            System.out.print(": ");
            System.out.print(entry.getValue());
            System.out.print("\n");
        }
    }

    boolean isImportant(String[] path) {
        for (var s : path) {
            if (s.startsWith("!")) {
                return true;
            }
        }
        return false;
    }

    void postProcess(JsonNode node) {
        System.out.println("bad override");
    }

    public void call(String[] args) throws IOException, InterruptedException {
        var mapper = new ObjectMapper();
        var json1 = mapper.readTree(read(args[0]));
        var json2 = mapper.readTree(read(args[1]));
        postProcess(json1);
        postProcess(json2);

        var patch = JsonDiff.asJson(json1, json2);
        TreeMap<String, Integer> stat = new TreeMap<String, Integer>();
        TreeMap<String, Integer> importantStat = new TreeMap<String, Integer>();
        for (var diff : patch) {
            var list = diff.path("path").asText().split("/");
            StringBuilder builder = new StringBuilder();
            builder.append(diff.path("op").asText().toUpperCase());
            for (var idx = 0; idx < list.length; ++idx) {
                if (isNumber(list[idx])) {
                    list[idx] = "*";
                }
            }
            builder.append(String.join(".", list));
            var fullKey = builder.toString();
            var dct = stat;
            if (isImportant(list)) {
                dct = importantStat;
            }
            dct.put(fullKey, stat.getOrDefault(fullKey, 0) + 1);
        }
        printStat(importantStat);
        System.out.print("\n");
        printStat(stat);
        System.out.flush();
        Process diff = new ProcessBuilder().command("diff", "-u", args[0], args[1]).inheritIO().start();
        int exitCode = diff.waitFor();
        if (stat.size() > 0 || exitCode != 0) {
            System.exit(1);
        }
    }
}
