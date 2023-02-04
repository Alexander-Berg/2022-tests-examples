package ru.yandex.webmaster3.storage.turbo.dao.scc;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.core.turbo.model.TurboHostSettings;
import ru.yandex.webmaster3.core.turbo.model.app.TurboAppSettings;
import ru.yandex.webmaster3.core.turbo.model.commerce.TurboCommerceSettings;
import ru.yandex.webmaster3.storage.turbo.dao.scc.model.TurboSccPremoderationStatus;
import ru.yandex.webmaster3.storage.turbo.service.TurboDomainsStateService;

/**
 * @author kravchenko99
 * @date 10/21/20
 */

@RunWith(MockitoJUnitRunner.class)
public class TurboSccServiceTest {

    public void check(TurboDomainsStateService.TurboDomainState turboDomainState,
                      TurboAppSettings appSettings,
                      TurboHostSettings turboSettings,
                      TurboSccService.FrontModerationStatus expectedStatus) {
//        final TurboSccService.FrontModerationStatus actualStatus = TurboSccService.getFrontModerationStatus(turboDomainState, appSettings, turboSettings);
//        Assert.assertEquals(expectedStatus, actualStatus);
    }

    private static final TurboDomainsStateService.TurboDomainState EMPTY = TurboDomainsStateService.TurboDomainState.empty("");


    @Test
    public void checkOuterCartStatus() {
        final TurboHostSettings turboSettings = TurboHostSettings.builder().setCommerceSettings(
                new TurboCommerceSettings.TurboCommerceSettingsBuilder().setCartUrlEnabled(true).build()).build();
        check(EMPTY,
                null,
                turboSettings,
                TurboSccService.FrontModerationStatus.OUTER_CART);
    }

    @Test
    public void checkBannedStatus() {
        final TurboDomainsStateService.TurboDomainState turboDomainState = TurboDomainsStateService.TurboDomainState.builder().bannedScc(new TurboDomainsStateService.TurboBannedScc("comment")).build();
        check(turboDomainState, null, null, TurboSccService.FrontModerationStatus.BANNED);

    }

    @Test
    public void checkInProgressStatus() {
        final DateTime launchDateTime = DateTime.now();
        final TurboAppSettings appSettings = TurboAppSettings.builder()
                .sccCheckStartDate(launchDateTime).build();
        check(EMPTY, appSettings, null, TurboSccService.FrontModerationStatus.IN_PROGRESS);
        final TurboDomainsStateService.TurboDomainState turboDomainState = TurboDomainsStateService.TurboDomainState.builder()
                .premoderationResult(new TurboSccPremoderationStatus(launchDateTime.minusMinutes(1), null, null, null))
                .bannedScc(TurboDomainsStateService.TurboBannedScc.empty())
                .build();
        check(turboDomainState, appSettings, null, TurboSccService.FrontModerationStatus.IN_PROGRESS);

    }

    @Test
    public void checkPassStatus() {
        final TurboDomainsStateService.TurboDomainState turboDomainStateFailed = TurboDomainsStateService.TurboDomainState.builder()
                .premoderationResult(new TurboSccPremoderationStatus(null, TurboSccPremoderationStatus.ModerationStatus.PASS, null, null))
                .bannedScc(TurboDomainsStateService.TurboBannedScc.empty())
                .build();
        check(turboDomainStateFailed, null, null, TurboSccService.FrontModerationStatus.PASS);
        final TurboDomainsStateService.TurboDomainState turboDomainStateNoPlacement = TurboDomainsStateService.TurboDomainState.builder()
                .premoderationResult(new TurboSccPremoderationStatus(null, TurboSccPremoderationStatus.ModerationStatus.PROBLEM_PASS, null, null))
                .bannedScc(TurboDomainsStateService.TurboBannedScc.empty())
                .build();
        check(turboDomainStateNoPlacement, null, null, TurboSccService.FrontModerationStatus.PASS);

    }

    @Test
    public void checkFailedStatus() {
        final TurboDomainsStateService.TurboDomainState turboDomainStateFailed = TurboDomainsStateService.TurboDomainState.builder()
                .premoderationResult(new TurboSccPremoderationStatus(null, TurboSccPremoderationStatus.ModerationStatus.FAILED, null, null))
                .bannedScc(TurboDomainsStateService.TurboBannedScc.empty())
                .build();
        check(turboDomainStateFailed, null, null, TurboSccService.FrontModerationStatus.FAILED);
        final TurboDomainsStateService.TurboDomainState turboDomainStateNoPlacement = TurboDomainsStateService.TurboDomainState.builder()
                .premoderationResult(new TurboSccPremoderationStatus(null, TurboSccPremoderationStatus.ModerationStatus.NO_PLACEMENT, null, null))
                .bannedScc(TurboDomainsStateService.TurboBannedScc.empty())
                .build();
        check(turboDomainStateNoPlacement, null, null, TurboSccService.FrontModerationStatus.FAILED);


    }

    @Test
    public void checkUnknownStatus() {
        check(EMPTY, null, null, TurboSccService.FrontModerationStatus.UNKNOWN);
    }
}
