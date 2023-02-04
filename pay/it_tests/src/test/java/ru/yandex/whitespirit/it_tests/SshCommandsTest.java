package ru.yandex.whitespirit.it_tests;

import io.restassured.response.Response;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HERKULES;
import static ru.yandex.whitespirit.it_tests.utils.Constants.HORNS_AND_HOOVES;

public class SshCommandsTest {
    private static final WhiteSpiritManager whiteSpiritManager = Context.getWhiteSpiritManager();
    private static final WhiteSpiritClient whiteSpiritClient = whiteSpiritManager.getWhiteSpiritClient();
    private static String kktSN;
    private static String kktForSshKeyUploadSN;

    @BeforeAll
    public static void chooseKKT() {
        kktSN = whiteSpiritManager.getKktSerialNumbersByInn(HORNS_AND_HOOVES.getInn())
                .stream().findAny().orElseThrow();
        kktForSshKeyUploadSN = whiteSpiritManager.getKktSerialNumbersByInn(HERKULES.getInn())
                .stream().findAny().orElseThrow();
    }

    @Test
    @DisplayName("Свежезапущенная касса должна быть доступна по ssh-паролю, но не по ssh-ключу")
    @RunOnlyIfSshTestsAreEnabled
    public void testPingFreshKKT() {
        assertEchoResponse(whiteSpiritClient.sshPing(kktSN, true));
        whiteSpiritClient.sshPing(kktSN, false).then().statusCode(401);
    }

    @Test
    @DisplayName("Получаем пароль с кассы")
    @RunOnlyIfSshTestsAreEnabled
    public void testGetPasswordFromKKT() {
        whiteSpiritClient.getPassword(kktSN, true).then()
                .statusCode(200)
                .body("Password", equalTo(666666));
    }

    @Test
    @DisplayName("После загрузки ключа на кассу, должен заработать доступ на неё без ввода пароля")
    @RunOnlyIfSshTestsAreEnabled
    public void testSetupSSHConnection() {
        assertEchoResponse(whiteSpiritClient.sshPing(kktForSshKeyUploadSN, true));
        whiteSpiritClient.sshPing(kktSN, false).then().statusCode(401);

        whiteSpiritClient.setupSshConnection(kktForSshKeyUploadSN).then();

        assertEchoResponse(whiteSpiritClient.sshPing(kktForSshKeyUploadSN, true));
        assertEchoResponse(whiteSpiritClient.sshPing(kktForSshKeyUploadSN, false));
    }

    private static void assertEchoResponse(Response response) {
        response.then()
                .statusCode(200)
                .body("response", equalTo("pong"));
    }
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Test
@EnabledIfEnvironmentVariable(named = "RUN_SHH_TESTS", matches = "True")
@interface RunOnlyIfSshTestsAreEnabled {}
