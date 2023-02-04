package ru.yandex.partner.core.entity.page.repository;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.page.filter.PageFilters;
import ru.yandex.partner.core.entity.page.model.BasePage;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.model.PageWithCommonFields;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.filter.operator.FilterOperator;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@CoreTest
class PageTypedRepositoryTest {

    @Autowired
    PageTypedRepository pageTypedRepository;


    @Test
    void getBlocksByFilterTest() {
        CoreFilterNode<PageWithCommonFields> node = CoreFilterNode.create(
                PageFilters.CAPTION,
                FilterOperator.LIKE, List.of("TEST"));
        List<BasePage> pages = pageTypedRepository.getAll(QueryOpts.forClass(ContextPage.class)
                .withFilter(node)
        );
        assertThat(pages.size()).isPositive();
        pages.forEach(page -> assertThat(((PageWithCommonFields) page).getCaption()).containsIgnoringCase("TEST"));
    }
}
