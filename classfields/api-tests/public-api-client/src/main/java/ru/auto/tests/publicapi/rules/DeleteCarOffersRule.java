package ru.auto.tests.publicapi.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiOffer;
import ru.auto.tests.publicapi.model.AutoApiOfferListingResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Created by dskuznetsov on 14.08.18
 */

public class DeleteCarOffersRule extends ExternalResource {
    private static final Logger log = Logger.getLogger(DeleteCarOffersRule.class);

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    @Prod
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

    public DeleteCarOffersRule() {
    }

    protected void after() {
        this.accountKeeper.get().forEach(this::deleteCarOffers);
    }

    @Step("Удаляем все офферы легковых у пользователя {account.login}:{account.password} ({account.id})")
    private void deleteCarOffers(Account account) {
        try {
            String sessionId = adaptor.login(account).getSession().getId();
            AutoApiOfferListingResponse userOffersResponse = api.userOffers().offers().reqSpec(defaultSpec())
                    .categoryPath(CARS)
                    .xSessionIdHeader(sessionId)
                    .executeAs(Function.identity());

            if (userOffersResponse.getOffers() != null) {
                List<String> ids = userOffersResponse.getOffers().stream()
                        .map(AutoApiOffer::getId)
                        .collect(Collectors.toList());

                for (String id : ids) {
                    api.userOffers().hideOffer().categoryPath(CARS).offerIDPath(id)
                            .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(Function.identity());
                }
            }
        } catch (Throwable e) {
            log.error(String.format("Can't delete offers for uid %s", account.getId()), e);
        }

    }
}
