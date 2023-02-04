package ru.yandex.webmaster3.core.sitestructure;

import org.junit.Assert;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by Oleg Bazdyrev on 26/09/2018.
 * Довольно бессмысленный тест ввиду автоматической генерации REVERSE_MAPPING
 */
public class SearchUrlStatusUtilTest {

    private static final Set<SearchUrlStatusEnum> INGORED_STATUSES =
            EnumSet.of(SearchUrlStatusEnum.INDEXED_NOTSEARCHABLE, SearchUrlStatusEnum.INDEXED_SEARCHABLE,
                    SearchUrlStatusEnum.REDIRECT_SEARCHABLE, SearchUrlStatusEnum.OTHER);

    @Test
    public void testStatusEnumConversion() {
        // т.к. одному статусу может соответствовать несколько raw - начнем именно с SearchUrlStatusEnum
        for (SearchUrlStatusEnum status : SearchUrlStatusEnum.values()) {
            if (INGORED_STATUSES.contains(status)) {
                continue;
            }
            Set<RawSearchUrlStatusEnum> rawStatuses = SearchUrlStatusUtil.view2AllRaw(status);
            for (RawSearchUrlStatusEnum rawStatus : rawStatuses) {
                Assert.assertEquals(status, SearchUrlStatusUtil.raw2View(rawStatus, false));
            }
        }
    }

}
