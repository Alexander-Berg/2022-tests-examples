package ru.yandex.vertis.subscriptions.plugin.remote;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 *
 * @author zvez
 */
public class JsonRequestUtilsTest {

    @Test
    public void getServantletResponse() throws Exception {
        String response = Resources.toString(
                getClass().getResource("/realty-proposal.json"),
                Charsets.UTF_8);
        String parsed = JsonRequestUtils.getServantletResponse(response);

        String expected = new JSONObject(response).toString();
        assertTrue(parsed.equals(expected));
    }
}
