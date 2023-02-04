package ru.yandex.spirit.it_tests;

import lombok.experimental.UtilityClass;
import lombok.val;
import org.json.JSONArray;
import ru.yandex.darkspirit.it_tests.DarkspiritClient;
import ru.yandex.darkspirit.it_tests.DatabaseClient;
import ru.yandex.spirit.it_tests.configuration.SpiritKKT;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import static ru.yandex.spirit.it_tests.Utils.get_secret;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.Matchers.is;
import org.json.JSONObject;


@UtilityClass
public class InteractionsManager {
    private static final String robotDarkspiritTstOauthTokenPath =
            "containers/darkspirit/secrets/robot-darkspirit-darkspirit-oauth-token.txt";
    private static DarkspiritClient darkspiritClient;
    private static WhiteSpiritClient whiteSpiritClient;
    private static DatabaseClient databaseClient;
    private static StartrekClient startrekClient;

    static {
        darkspiritClient = IntegrationManager.getDarkspiritClient();
        whiteSpiritClient = IntegrationManager.getWhiteSpiritClient();
        databaseClient = IntegrationManager.getDatabaseClient();
        startrekClient = new StartrekClient(get_secret(robotDarkspiritTstOauthTokenPath));
    }

    public static void closeStartrekTicket(String ticket) {
        startrekClient.closeTicket(ticket, "fixed").then().statusCode(200);
    }

    public static String getStartrekIssue(String ticket) {
        return startrekClient.getIssue(ticket).then().statusCode(200).extract().asString();
    }

    public static void removeStartrekReregTagInReregistration(String ticket) {
        startrekClient.removeTag(ticket, "ds_rereg_application").then().statusCode(200);
    }

    public static void postStartrekReregCardAttachment(String ticket, SpiritKKT kkt) {
        startrekClient.postAttachment(
                ticket, String.format("rereg_cards/rereg_card_%s_%s.pdf", kkt.kktSN, kkt.fnSN)
        ).then().statusCode(201);
    }

    public static void removeStartrekReregTagInReregistrationForPreviousRuns(String cr_sn, String fn_sn) {
        String queue = "SPIRITSUP";
        String query = String.format(
                "Queue: %s and Tags: ds_rereg_application and Tags: cr_sn_%s and Tags: fn_sn_%s",
                queue, cr_sn, fn_sn
        );


        String ticketFindResultString = startrekClient.findTicket(query)
                .then().statusCode(200).extract().asString();

        JSONArray tickets = new JSONArray(ticketFindResultString);

        for (val ticketObj: tickets) {
            JSONObject ticket = (JSONObject)ticketObj;
            removeStartrekReregTagInReregistration(ticket.get("key").toString());
        }
    }

    public static void waitWhitespiritDarkspirit(List<SpiritKKT> expectedKkts) {
        Set<String> kktSerials = new HashSet<>();
        for (val kkt : expectedKkts) {
            kktSerials.add(kkt.kktSN);
        }
        await().pollInterval(5, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).until(() ->
                WhiteSpiritManager.allKKTsFound(whiteSpiritClient.info().body().asString(), kktSerials)
        );
        given().ignoreExceptions().with().pollInterval(10, TimeUnit.SECONDS).await().atMost(1, TimeUnit.MINUTES)
                .until(() -> {
                    whiteSpiritClient.cashmachines().then().statusCode(200)
                            .body("darkspirit.registered", is(true));
                    return true;
                });
    }
}
