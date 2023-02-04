package ru.yandex.realty.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.extern.java.Log;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import ru.auto.test.api.realty.offer.create.userid.Offer;
import ru.auto.tests.passport.account.AccountKeeper;
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.anno.WithOffers;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.utils.AccountType;

import java.util.List;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;
import static ru.yandex.realty.utils.AccountType.AGENT;
import static ru.yandex.realty.utils.RealtyUtils.reformatOfferCreateDate;

/**
 * @author kurau (Yuri Kalinin)
 */
@Log
public class CreateOfferRule implements TestRule {

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private AccountKeeper accountKeeper;

    @Inject
    private Vos2Adaptor adaptor;

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    before(description);
                    base.evaluate();
                } finally {
                    deleteOffers();
                }
            }
        };
    }

    private void before(Description description) {
        if (description.getAnnotation(WithOffers.class) != null) {
            WithOffers withOffers = description.getAnnotation(WithOffers.class);
            initAccount(withOffers);

            Offer offer = OfferBuildingSteps.getDefaultOffer(withOffers.offerType());
            withCreateTime(withOffers, offer);
            withUpdateTime(withOffers, offer);

            offerBuildingSteps.addNewOffer(accountKeeper.get().get(0))
                    .withBody(offer)
                    .count(withOffers.count())
                    .create();
        }
    }

    @Step("Удаляем все объявления у пользователя")
    private void deleteOffers() {
        String uid = accountKeeper.get().get(0).getId();
        try {
            if (adaptor.isVosUser(uid)) {
                List<ru.auto.test.api.realty.useroffers.userid.responses.Offer> respList =
                        adaptor.getUserOffers(uid).getOffers();
                respList.stream().map(ru.auto.test.api.realty.useroffers.userid.responses.Offer::getId).collect(toList())
                        .forEach(id -> adaptor.deleteOffer(uid, id));
            }
        } catch (Exception e) {
            log.info(String.format("Can't delete offers with uid %s. Exception: %s", uid, e.getMessage()));
        }
    }

    private void initAccount(WithOffers withOffers) {
        apiSteps.createVos2AccountWithoutLogin(accountKeeper.get().get(0),
                AccountType.valueOf(withOffers.accountType().toUpperCase()));
    }

    private void withCreateTime(WithOffers withOffers, Offer offer) {
        if (withOffers.createDay() != 0) {
            offer.withCreateTime(reformatOfferCreateDate(now().plusDays(withOffers.createDay())));
        }
    }

    private void withUpdateTime(WithOffers withOffers, Offer offer) {
        if (withOffers.updateDay() != 0) {
            offer.withUpdateTime(reformatOfferCreateDate(now().plusDays(withOffers.updateDay())));
        }
    }
}
