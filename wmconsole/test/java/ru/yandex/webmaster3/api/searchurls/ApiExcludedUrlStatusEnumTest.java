package ru.yandex.webmaster3.api.searchurls;

import org.junit.Test;
import ru.yandex.webmaster3.api.searchurls.data.ApiExcludedUrlStatusEnum;
import ru.yandex.webmaster3.core.sitestructure.SearchUrlStatusEnum;

/**
 * @author leonidrom
 */
public class ApiExcludedUrlStatusEnumTest {
    @Test
    public void allCoreStatusesShouldBeMapped() {
        for (SearchUrlStatusEnum coreStatus : SearchUrlStatusEnum.values()) {
            // для неизвестного значения будет исключение
            //noinspection ResultOfMethodCallIgnored
            ApiExcludedUrlStatusEnum.fromCoreStatus(coreStatus);
        }
    }
}
