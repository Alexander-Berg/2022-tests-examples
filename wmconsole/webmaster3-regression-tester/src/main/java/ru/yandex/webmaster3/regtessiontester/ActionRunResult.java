package ru.yandex.webmaster3.regtessiontester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Результат выполнения action'а на одном хосте
 * Created by Oleg Bazdyrev on 30/05/2017.
 */
public class ActionRunResult {

    private static final ObjectMapper OM_PRETTY = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final String host;
    private final JsonNode data;
    private final boolean successful;
    private final String errorText;

    public ActionRunResult(String host, JsonNode data, boolean successful, String errorText) {
        this.host = host;
        this.data = data;
        this.successful = successful;
        this.errorText = errorText;
    }

    public String getHost() {
        return host;
    }

    public JsonNode getData() {
        return data;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getErrorText() {
        return errorText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionRunResult that = (ActionRunResult) o;

        if (successful != that.successful) return false;
        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        return errorText != null ? errorText.equals(that.errorText) : that.errorText == null;
    }

    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (successful ? 1 : 0);
        result = 31 * result + (errorText != null ? errorText.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (data != null && successful) {
            try {
                return OM_PRETTY.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return "Error: " + errorText;
    }

}
