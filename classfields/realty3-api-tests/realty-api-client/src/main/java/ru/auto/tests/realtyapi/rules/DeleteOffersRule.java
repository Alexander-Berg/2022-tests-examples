package ru.auto.tests.realtyapi.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.extern.log4j.Log4j;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.responses.Offer;
import ru.auto.tests.realtyapi.v1.ApiClient;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;

@Log4j
public class DeleteOffersRule extends ExternalResource {

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private RealtyApiAdaptor adaptor;

    @Inject
    private OAuth oAuth;

    @Inject
    @Prod
    private ApiClient api;

    @Override
    protected void after() {
        accountKeeper.get().forEach(this::deleteOffers);

    }

    @Step("Удаляем все объявления у пользователя {account.id}")
    private void deleteOffers(Account account) {
        try {
            String token = oAuth.getToken(account);
            List<Offer> respList = adaptor.getUserOffers(token).getResponse().getOffers();
            respList.stream().map(Offer::getId).collect(toList())
                    .forEach(id -> deleteOffer(token, id));
        } catch (Exception e) {
            log.error(String.format("Can't delete offers with uid %s. Exception: %s", account.getId(), e.getMessage()));
        }
    }

    @Step("Удаляем оффер {offerId}")
    private void deleteOffer(String token, String offerId) {
        api.userOffers().deleteOfferRoute().offerIdPath(offerId)
                .reqSpec(authSpec())
                .authorizationHeader(token).execute(Function.identity());
    }
}

