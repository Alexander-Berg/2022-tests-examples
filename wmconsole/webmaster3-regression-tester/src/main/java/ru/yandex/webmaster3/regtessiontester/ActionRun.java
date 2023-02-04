package ru.yandex.webmaster3.regtessiontester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Факт запуска одного action'а
 * Created by Oleg Bazdyrev on 30/05/2017.
 */
public class ActionRun {

    private static final Set<String> IGNORE_PARAM_KEYS = Sets.newHashSet("balancerRequestId", "userIp", "frontendIp");

    private final String action;
    private final JsonNode params;
    private final Map<String, ActionRunResult> resultsByHost = new HashMap<>();

    public ActionRun(String action, JsonNode params) {
        this.action = action;
        this.params = params;
    }

    public String getAction() {
        return action;
    }

    public JsonNode getParams() {
        return params;
    }

    public Map<String, ActionRunResult> getResultsByHost() {
        return resultsByHost;
    }

    public boolean hasDifferentResults() {
        if (resultsByHost.isEmpty())
            return false;
        Iterator<ActionRunResult> iterator = resultsByHost.values().iterator();
        ActionRunResult firstResult = iterator.next();
        while (iterator.hasNext()) {
            if (!iterator.next().equals(firstResult))
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionRun actionRun = (ActionRun) o;

        if (!action.equals(actionRun.action)) return false;
        if (params instanceof ObjectNode) {
            ObjectNode node = (ObjectNode) params;
            ObjectNode other = (ObjectNode) actionRun.params;
            if (node.size() != other.size())
                return false;
            Iterator<String> iterator = node.fieldNames();
            while (iterator.hasNext()) {
                String fieldName = iterator.next();
                if (IGNORE_PARAM_KEYS.contains(fieldName)) {
                    continue;
                }
                if (!node.get(fieldName).equals(other.get(fieldName))) {
                    return false;
                }
            }
            return true;
        } else {
            return params.equals(actionRun.params);
        }
    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + params.hashCode();
        return result;
    }
}
