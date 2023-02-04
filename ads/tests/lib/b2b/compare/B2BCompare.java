package bsyeti.json.compare;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class B2BCompare extends Compare {
    public static void main(String[] args) throws IOException, InterruptedException {
        var inst = new B2BCompare();
        inst.call(args);
    }

    void postProcessSingle(JsonNode profiles) {
        for (int profileIdx = 0; profileIdx < profiles.size(); profileIdx++) {
            var profile = profiles.get(profileIdx);
            var counterNode = profile.get("Counters");
            if (counterNode == null || !counterNode.has("PackedCounters")) {
                continue;
            }
            var packedCounters = counterNode.get("PackedCounters");
            for (var packedCounter: packedCounters) {
                var counterIds = packedCounter.get("counter_ids");
                var rawValues = packedCounter.get("values");
                assert counterIds.size() == rawValues.size();
                for (int counterIdx = 0; counterIdx < counterIds.size(); counterIdx++) {
                    var key = "!counter_" + counterIds.get(counterIdx).toString();
                    var listOfValues = rawValues.get(counterIdx);
                    if (listOfValues == null) {
                        continue;
                    }
                    for (var rawValue: listOfValues) {
                        var val = rawValue.get("value");
                        ((ObjectNode) profile).put(key, val);
                    }
                }
            }
        }
    }
    void postProcess(JsonNode profiles) {
        if (profiles.isArray()) {
            postProcessSingle(profiles);
        } else {
            var fields = profiles.fields();
            while (fields.hasNext()) {
                var pair = fields.next();
                postProcessSingle(pair.getValue());
            }
        }
    }
}
