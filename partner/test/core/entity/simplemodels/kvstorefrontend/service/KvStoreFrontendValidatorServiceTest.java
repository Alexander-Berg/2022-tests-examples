package ru.yandex.partner.core.entity.simplemodels.kvstorefrontend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.entity.kvstorefrontend.model.KvStoreFrontend;
import ru.yandex.partner.core.entity.simplemodels.SimpleRepository;
import ru.yandex.partner.core.entity.simplemodels.kvstorefrontend.service.validation.KvStoreFrontendValidatorService;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.validation.result.ValidationResult.getInvalidItems;
import static ru.yandex.partner.core.entity.simplemodels.kvstorefrontend.service.validation.KvStoreDefect.userStoreOverflow;

class KvStoreFrontendValidatorServiceTest {
    public static final int MAX_STORE_SIZE = 10;
    private final KvStoreFrontendValidatorService validatorService;

    private SimpleRepository<KvStoreFrontend> repository =
            (SimpleRepository<KvStoreFrontend>) mock(SimpleRepository.class);

    KvStoreFrontendValidatorServiceTest() {
        this.validatorService = new KvStoreFrontendValidatorService(
                MAX_STORE_SIZE,
                repository
        );

        doReturn(emptySet()).when(repository).existingIds(any());
    }

    @Test
    void validateFirstCase() {
        var testedEntity1 = new KvStoreFrontend().withId(1L).withUserId(1L);
        var testedEntity2 = new KvStoreFrontend().withId(2L).withUserId(2L);

        doReturn(Map.of(
                1L, MAX_STORE_SIZE / 2,
                2L, MAX_STORE_SIZE + 1
        )).when(repository).getCountGroupedBy(eq(KvStoreFrontend.USER_ID.name()), any());

        ValidationResult<List<KvStoreFrontend>, Defect> validationResult =
                validatorService.validate(List.of(testedEntity1, testedEntity2));

        assertThat(validationResult.hasErrors());
        assertThat(getInvalidItems(validationResult))
                .hasSize(1)
                .describedAs("Only last item should have error")
                .contains(testedEntity2);
        assertThat(validationResult.flattenErrors())
                .hasSize(1)
                .allMatch(defectDefectInfo -> userStoreOverflow(2L).equals(defectDefectInfo.getDefect()));
    }

    @Test
    void validateSecondCase() {
        var entities = new ArrayList<KvStoreFrontend>();

        long user3 = 3L;

        // overflow by 2
        for (long i = 0; i < MAX_STORE_SIZE + 2; i++) {
            entities.add(new KvStoreFrontend()
                    .withId(i)
                    .withUserId(user3)
            );
        }

        ValidationResult<List<KvStoreFrontend>, Defect> validationResult =
                validatorService.validate(entities);

        assertThat(validationResult.hasErrors());
        assertThat(getInvalidItems(validationResult))
                .hasSize(2)
                .describedAs("Only last items should have error")
                .containsExactlyElementsOf(
                        entities.stream().skip(MAX_STORE_SIZE).collect(Collectors.toSet())
                );
        assertThat(validationResult.flattenErrors())
                .hasSize(2)
                .allMatch(defectDefectInfo -> userStoreOverflow(3L).equals(defectDefectInfo.getDefect()));
    }

}
