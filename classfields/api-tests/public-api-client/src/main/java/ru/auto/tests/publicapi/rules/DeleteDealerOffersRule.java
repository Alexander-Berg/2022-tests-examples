package ru.auto.tests.publicapi.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.adaptor.PublicApiDealerAdaptor;
import ru.auto.tests.publicapi.utils.OfferKeeper;

import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_LOGIN;
import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_PASS;
import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_UID;

public class DeleteDealerOffersRule extends ExternalResource {
    private static final Logger log = Logger.getLogger(DeleteDealerOffersRule.class);

    @Inject
    private OfferKeeper offerKeeper;

    @Inject
    private PublicApiDealerAdaptor adaptor;

    public DeleteDealerOffersRule() {
    }

    protected void after() {
        if (!offerKeeper.get().isEmpty()) {
            deleteOffers(DEALER_LOGIN, DEALER_PASS, DEALER_UID);
            offerKeeper.clear();
        }
    }

    @Step("Удаляем созданные офферы у пользователя-дилера {dealerLogin}:{dealerPassword} ({dealerId})")
    private void deleteOffers(String dealerLogin, String dealerPassword, String dealerId) {
        try {
            String sessionId = adaptor.login(Account.builder().login(dealerLogin).password(dealerPassword).id(dealerId).build()).getSession().getId();
            offerKeeper.get().forEach((offerId, category) -> {
                adaptor.deleteOffer(sessionId, category, offerId);
            });
        } catch (Throwable e) {
            log.error(String.format("Can't delete offers for uid %s", DEALER_UID), e);
        }

    }
}
