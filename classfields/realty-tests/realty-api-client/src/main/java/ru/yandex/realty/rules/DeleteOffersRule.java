package ru.yandex.realty.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.extern.log4j.Log4j;
import org.junit.rules.ExternalResource;
import ru.auto.test.api.realty.useroffers.userid.responses.Offer;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.yandex.realty.adaptor.Vos2Adaptor;

import java.util.List;

import static java.util.stream.Collectors.toList;


/**
 * Created by vicdev on 29.11.17.
 * VERTISTEST-641
 */
@Log4j
public class DeleteOffersRule extends ExternalResource {

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private Vos2Adaptor adaptor;

    @Override
    protected void after() {
        accountKeeper.get().forEach(a -> deleteOffers(a.getId()));
    }

    @Step("Удаляем все объявления у пользователя {uid}")
    private void deleteOffers(String uid) {
        try {
            if (adaptor.isVosUser(uid)) {
                List<Offer> respList = adaptor.getUserOffers(uid).getOffers();
                respList.stream().map(Offer::getId).collect(toList())
                        .forEach(id -> adaptor.deleteOffer(uid, id));
            }
        } catch (Throwable e) {
            log.info(String.format("Can't delete offers with uid %s. Exception: %s", uid, e.getMessage()));
        }
    }
}
