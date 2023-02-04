package ru.yandex.partner.core.entity.block.service.type.duplicate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.block.BlockUniqueIdConverter;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.container.BlockContainerImpl;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BlockWithCustomBkDataAndDesignTemplates;
import ru.yandex.partner.core.entity.block.service.BlockService;
import ru.yandex.partner.core.entity.block.service.OperationMode;
import ru.yandex.partner.core.entity.block.type.custombkdata.BlockWithCustomBkDataAndDesignTemplatesAddOperationTypeSupport;
import ru.yandex.partner.core.entity.designtemplates.model.DesignTemplates;
import ru.yandex.partner.core.filter.CoreFilterNode;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CoreTest
class BlockWithCustomBkDataDuplicateTest {

    @Autowired
    BlockWithCustomBkDataAndDesignTemplatesAddOperationTypeSupport addOperationTypeSupport;

    @Autowired
    BlockService blockService;

    @Value("classpath:mock/bkdata/bkdata_before_1.txt")
    Resource bkDataBefore1;
    @Value("classpath:mock/bkdata/bkdata_before_2.txt")
    Resource bkDataBefore2;
    @Value("classpath:mock/bkdata/bkdata_expected_1.txt")
    Resource expected1;
    @Value("classpath:mock/bkdata/bkdata_expected_2.txt")
    Resource expected2;

    @Test
    void testChangeBkData() throws IOException, JSONException {
        var container = BlockContainerImpl.create(OperationMode.DUPLICATE);
        var blocks = blockService.findAll(QueryOpts.forClass(BlockWithCustomBkDataAndDesignTemplates.class)
                .withFilter(CoreFilterNode.in(BlockFilters.ID, List.of(347674247175L)))
                .forUpdate()
        );
        // 1 вариант
        blocks.get(0).setBkData(getResourceString(bkDataBefore1.getURL().openStream()));
        blocks.get(0).setBlockId(500L);
        blocks.get(0).setId(BlockUniqueIdConverter.convertToUniqueId(
                BlockUniqueIdConverter.Prefixes.CONTEXT_ON_SITE_RTB_PREFIX, 41446, 500));

        addOperationTypeSupport.beforeExecution(container, blocks);
        assertThat(blocks.get(0).getBkData()).isEqualTo(getResourceString(expected1.getURL().openStream()));
        // 2 вариант
        blocks.get(0).setBkData(getResourceString(bkDataBefore2.getURL().openStream()));

        addOperationTypeSupport.beforeExecution(container, blocks);
        blocks.get(0).setDesignTemplates(List.of(new DesignTemplates().withId(333L),
                new DesignTemplates().withId(444L)));
        addOperationTypeSupport.afterExecution(container, blocks);

        JSONAssert.assertEquals(
                getResourceString(expected2.getURL().openStream()),
                blocks.get(0).getBkData(),
                true
        );
    }

    private String getResourceString(InputStream inputStream) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
