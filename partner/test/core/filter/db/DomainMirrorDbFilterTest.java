package ru.yandex.partner.core.filter.db;

import java.util.List;

import org.jooq.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.page.model.BaseExternalPage;
import ru.yandex.partner.core.filter.operator.FilterOperator;
import ru.yandex.partner.core.junit.MySqlRefresher;

import static org.junit.jupiter.api.Assertions.assertTrue;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class DomainMirrorDbFilterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DomainMirrorDbFilterTest.class);

    @Autowired
    DomainMirrorDbFilter<BaseExternalPage> domainMirrorDbFilter;

    @Test
    void getCondition() {
        Condition condition = domainMirrorDbFilter.getCondition(FilterOperator.IN, List.of(
                "xn--b1adaavpfpf5e7e.xn--p1ai",
                "english-domain.com",
                "русский-домен.рф",
                ""
        ));

        LOGGER.debug(condition.toString());
        assertTrue(condition.toString().contains("'домдлядвоих.рф', 'english-domain.com', 'русский-домен.рф', ''"));
    }
}
