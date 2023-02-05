package ru.yandex.navi.tests;

import com.google.common.collect.Iterables;
import io.qameta.allure.Issue;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.Region;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;
import ru.yandex.navi.ui.DownloadMapsScreen;

import java.time.Duration;

@RunWith(RetryRunner.class)
public final class OfflineCacheTest extends BaseTest {
    @Test
    @Issue("MOBNAVI-14176")
    // TODO: changed: @TmsLink("navigator-915")
    public void downloadCaches() {
        tabBar.clickMenu().clickDownloadMaps().downloadMap("Минск");
    }

    @Test
    @Category({UnstableIos.class})
    public void updateCache() {
        settings.setOfflineCacheWifiOnly(false);

        downloadCache(Region.VLADIMIR);

        DownloadMapsScreen screen = tabBar.clickMenu().clickDownloadMaps();
        screen.clickRegion("Владимирская область");
        user.findElementByText("Загружено");

        commands.setCachesOutdated();
        user.shouldSee("Загружено");

        commands.setCachesNeedUpdate();
        user.shouldSee("Обновить");

        user.clicks(Iterables.getLast(screen.buttonsDownload));

        user.waitForLog("download-maps.download-map-completed", Duration.ofSeconds(30));
        user.shouldSee("Загружено");

        commands.setCacheUnsupported(Region.VLADIMIR);
        user.shouldSee("Загружено");
    }
}
