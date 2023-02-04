package ru.auto.tests.vos2.step;

import io.qameta.allure.Step;
import ru.auto.tests.vos2.VosClientRetrofit;

import javax.inject.Inject;
import java.io.IOException;

/**
 * User: timondl@yandex-team.ru
 * Date: 22.12.16
 */
public class VosUserSteps {

    @Inject
    public VosClientRetrofit client;

    @Step("Удаляем оффер {offerId}")
    public void deleteOffer(String category, String offerId) throws IOException {
        client.deleteOffer(category, offerId).execute();
    }

}
