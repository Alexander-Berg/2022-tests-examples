package ru.yandex.realty.goals.phoneallshow.card.village;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.beans.Goal;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.beans.Goal.goal;
import static ru.yandex.realty.beans.Goal.params;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.GoalsConsts.Goal.PHONE_ALL_SHOW;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.CARD;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.TRUE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.VILLAGE;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.VILLAGE_BASE_INFO;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.VILLAGE_DEV_INFO;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.VILLAGE_DEV_OBJECTS;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.VILLAGE_GALLERY;
import static ru.yandex.realty.consts.GoalsConsts.Parameters.VILLAGE_STICKY_RIGHT;
import static ru.yandex.realty.consts.Location.MOSCOW_OBL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.GOALS;
import static ru.yandex.realty.step.CommonSteps.FIRST;

@DisplayName("Цель «phone.all.show». Карточка коттеджного поселка")
@Feature(GOALS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class PhoneShowVillageOfferGoalsTest {

    private static final String PHONE_OFFER_ID = "phone.offer_id";
    private static final String ID = "1832934";
    private Goal.Params phoneAllShowParams;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ProxySteps proxy;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Before
    public void before() {
        phoneAllShowParams = params()
                .offerId(ID)
                .pageType(CARD)
                .page(VILLAGE)
                .payed(TRUE);
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        basePageSteps.resize(1600, 1800);
        urlSteps.testing().path(MOSCOW_OBL.getPath()).path(KUPIT).path(KOTTEDZHNYE_POSELKI)
                .path("orlov/").queryParam("id", ID).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показать телефон в карточке основной информации")
    public void shouldSeeBaseInfoPhoneAllAddGoal() {
        proxy.clearHarUntilThereAreNoHarEntries();
        phoneAllShowParams.placement(VILLAGE_BASE_INFO);
        basePageSteps.onVillageSitePage().villageCardAbout().showPhoneButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneAllShowParams)).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показать телефон в плавающем блоке справа")
    public void shouldSeeStickyRightPhoneAllAddGoal() {
        basePageSteps.scrollingUntil(() ->
                basePageSteps.onVillageSitePage().hideableBlock().showPhoneButton(), isDisplayed());
        proxy.clearHarUntilThereAreNoHarEntries();
        phoneAllShowParams.placement(VILLAGE_STICKY_RIGHT);
        basePageSteps.onVillageSitePage().hideableBlock().showPhoneButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneAllShowParams)).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показать телефон в блоке застройщика")
    public void shouldSeeDevPhoneAllAddGoal() {
        proxy.clearHarUntilThereAreNoHarEntries();
        phoneAllShowParams.placement(VILLAGE_DEV_INFO);
        basePageSteps.onVillageSitePage().cardDev().showPhoneButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneAllShowParams)).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показать телефон в блоке объектов от застройщика")
    public void shouldSeeFromDevPhoneAllAddGoal() {
        proxy.clearHarUntilThereAreNoHarEntries();
        phoneAllShowParams.placement(VILLAGE_DEV_OBJECTS);
        basePageSteps.onVillageSitePage().fromDev(FIRST).showPhoneButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneAllShowParams)).withIgnoringPaths(PHONE_OFFER_ID).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Показать телефон в галерее")
    public void shouldSeeGalleryPhoneAllAddGoal() {
        proxy.clearHarUntilThereAreNoHarEntries();
        phoneAllShowParams.placement(VILLAGE_GALLERY);
        basePageSteps.onVillageSitePage().galleryPic().click();
        basePageSteps.onVillageSitePage().galleryAside().showPhoneButton().click();
        goalsSteps.urlMatcher(containsString(PHONE_ALL_SHOW)).withGoalParams(
                goal().setPhone(phoneAllShowParams)).shouldExist();
    }
}