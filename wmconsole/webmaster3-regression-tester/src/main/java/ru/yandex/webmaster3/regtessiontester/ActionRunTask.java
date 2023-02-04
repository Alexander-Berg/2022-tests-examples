package ru.yandex.webmaster3.regtessiontester;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by Oleg Bazdyrev on 30/05/2017.
 */
public class ActionRunTask implements Callable<ActionRunResult> {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClientBuilder.create().build();
    private static final Map<String, String> KEY_MAPPING = ImmutableMap.of(
            "hostIdString", "hostId",
            "balancerRequestId", "");

    private final String host;
    private final String action;
    private final JsonNode parameters;

    public ActionRunTask(String host, String action, JsonNode parameters) {
        this.host = host;
        this.action = action;
        this.parameters = parameters;
    }

    @Override
    public ActionRunResult call() throws Exception {
        try {
            HttpPost post = new HttpPost(host + action + ".json");
            Iterator<Map.Entry<String, JsonNode>> iterator = parameters.fields();
            ObjectNode paramsNode = OM.createObjectNode();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String key = KEY_MAPPING.getOrDefault(entry.getKey(), entry.getKey());
                if (!key.isEmpty()) {
                    paramsNode.set(key, entry.getValue());
                }
            }
            post.setEntity(new StringEntity(OM.writeValueAsString(paramsNode), ContentType.APPLICATION_JSON));
            HttpResponse response = CLIENT.execute(post);
            JsonNode result = OM.readTree(response.getEntity().getContent());
            // take only data
            boolean successful = result.get("status") != null && "SUCCESS".equals(result.get("status").asText());
            JsonNode data = result.get("data");
            JsonNode errors = result.get("errors");
            String errorText = null;
            if (errors != null && errors.size() > 0) {
                errorText = errors.get(0).get("message").asText();
            }
            return new ActionRunResult(host, data, successful, errorText);
        } catch (Exception e) {
            return new ActionRunResult(host, null, false, e.getMessage());
        }
    }
}
