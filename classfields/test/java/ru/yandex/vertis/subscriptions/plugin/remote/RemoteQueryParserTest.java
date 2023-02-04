package ru.yandex.vertis.subscriptions.plugin.remote;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;
import ru.yandex.vertis.subscriptions.Model;

import static org.junit.Assert.assertTrue;

/**
 * Tests on parsing {@link RemoteJsonQueryParser} responses.
 */
public class RemoteQueryParserTest {

    private final static RemoteJsonQueryParser parser = new RemoteJsonQueryParser(new Resource(""));
    @Test
    public void parseResponse() throws Exception {
        final String response = Resources.toString(
                getClass().getResource("/query-parser-response.json"),
                Charsets.UTF_8);
        final Model.Query query = parser.getQuery(response);
        assertTrue(query.hasAnd());
    }

    @Test(expected = ErroneousResponseException.class)
    public void malformedResponse() throws Exception {
        final String response = Resources.toString(
                getClass().getResource("/query-parser-response.json"),
                Charsets.UTF_8);
        parser.getQuery(response.substring(2));
    }
}
