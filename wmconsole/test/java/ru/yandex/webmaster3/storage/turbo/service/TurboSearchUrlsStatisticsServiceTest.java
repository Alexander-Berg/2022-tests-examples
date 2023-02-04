package ru.yandex.webmaster3.storage.turbo.service;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.turbo.model.autoparser.AutoparserToggleState;
import ru.yandex.webmaster3.storage.turbo.dao.automorda.TurboAutoMordaStatus;


/**
 * ishalaru
 * 16.03.2020
 **/
public class TurboSearchUrlsStatisticsServiceTest {

    @Test
    public void checkIsEnableParser(){
        long value = 0;
        TurboSearchUrlsStatisticsService.TurboSourceStatuses turboSourceStatuses =
                new TurboSearchUrlsStatisticsService.TurboSourceStatuses(AutoparserToggleState.OFF, TurboAutoMordaStatus.OK, false, false, false, false);
        Assert.assertEquals("Turbo with zero value",false,TurboSearchUrlsStatisticsService.isEnabledTurboPage(value,turboSourceStatuses));
        value = 8;
        Assert.assertEquals("Turbo with YML",true,TurboSearchUrlsStatisticsService.isEnabledTurboPage(value,turboSourceStatuses));
        value = 1;
        Assert.assertEquals("Turbo autoparsed, and disabled autoparser",false,TurboSearchUrlsStatisticsService.isEnabledTurboPage(value,turboSourceStatuses));
        value = 2;
        Assert.assertEquals("Turbo autoparsed-button, and disabled autoparser",false,TurboSearchUrlsStatisticsService.isEnabledTurboPage(value,turboSourceStatuses));
        turboSourceStatuses = new TurboSearchUrlsStatisticsService.TurboSourceStatuses(AutoparserToggleState.ON, TurboAutoMordaStatus.OK, false, false, false, false);
        value = 0;
        Assert.assertEquals("Turbo with zero value, and enabled autoparser",false,TurboSearchUrlsStatisticsService.isEnabledTurboPage(value,turboSourceStatuses));
        value = 1;
        Assert.assertEquals("Turbo autoparsed, and enabled autoparser",true,TurboSearchUrlsStatisticsService.isEnabledTurboPage(value,turboSourceStatuses));
        value = 2;
        Assert.assertEquals("Turbo autoparsed-button, and enabled autoparser",true,TurboSearchUrlsStatisticsService.isEnabledTurboPage(value,turboSourceStatuses));
        value = 3;
        Assert.assertEquals("Turbo autoparser + autoparsed-button, and enabled autoparser + ",true,TurboSearchUrlsStatisticsService.isEnabledTurboPage(value,turboSourceStatuses));

    }
}