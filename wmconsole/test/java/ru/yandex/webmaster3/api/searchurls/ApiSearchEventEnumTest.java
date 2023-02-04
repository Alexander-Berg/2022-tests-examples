package ru.yandex.webmaster3.api.searchurls;

import org.junit.Test;
import ru.yandex.webmaster3.api.searchurls.data.ApiSearchEventEnum;
import ru.yandex.webmaster3.storage.searchurl.history.data.SearchUrlHistoryIndicator;
import ru.yandex.webmaster3.storage.searchurl.samples.data.SearchUrlEventType;

/**
 * @author leonidrom
 */
public class ApiSearchEventEnumTest {
    @Test
    public void allSearchUrlHistoryIndicatorsShouldBeMapped() {
        for (SearchUrlHistoryIndicator indicator : SearchUrlHistoryIndicator.values()) {
            if (indicator == SearchUrlHistoryIndicator.COUNT) {
                continue;
            }

            // для неизвестного значения будет исключение
            //noinspection ResultOfMethodCallIgnored
            ApiSearchEventEnum.fromSearchUrlHistoryIndicator(indicator);
        }
    }

    @Test
    public void allSearchUrlEventTypesShouldBeMapped() {
        for (SearchUrlEventType type : SearchUrlEventType.values()) {
            // для неизвестного значения будет исключение
            //noinspection ResultOfMethodCallIgnored
            ApiSearchEventEnum.fromSearchUrlEventType(type);
        }
    }
}
