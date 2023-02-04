package ru.yandex.partner.core.filter.container;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.assistant.filter.AssistantFilters;
import ru.yandex.partner.core.entity.assistant.model.Assistant;
import ru.yandex.partner.core.entity.block.filter.BlockFilters;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.Brand;
import ru.yandex.partner.core.entity.block.model.PiCategory;
import ru.yandex.partner.core.entity.blockseq.filter.BlockSequenceFilters;
import ru.yandex.partner.core.entity.blockseq.model.BlockSequence;
import ru.yandex.partner.core.entity.brand.filter.BrandFilters;
import ru.yandex.partner.core.entity.custombkoptions.filter.CustomBkOptionsFilters;
import ru.yandex.partner.core.entity.custombkoptions.model.CustomBkOptions;
import ru.yandex.partner.core.entity.designtemplates.filter.DesignTemplatesFilters;
import ru.yandex.partner.core.entity.designtemplates.model.BaseDesignTemplates;
import ru.yandex.partner.core.entity.domain.filter.DomainFilters;
import ru.yandex.partner.core.entity.domain.model.BaseDomain;
import ru.yandex.partner.core.entity.dsp.filter.DspFilters;
import ru.yandex.partner.core.entity.dsp.model.BaseDsp;
import ru.yandex.partner.core.entity.kvstorefrontend.model.KvStoreFrontend;
import ru.yandex.partner.core.entity.mirror.filter.MirrorFilters;
import ru.yandex.partner.core.entity.mirror.model.BaseMirror;
import ru.yandex.partner.core.entity.page.filter.PageFilters;
import ru.yandex.partner.core.entity.page.model.BasePage;
import ru.yandex.partner.core.entity.picategories.filter.PiCategoryFilters;
import ru.yandex.partner.core.entity.simplemodels.kvstorefrontend.filter.KvStoreFrontendFilters;
import ru.yandex.partner.core.entity.user.filter.UserFilters;
import ru.yandex.partner.core.entity.user.model.BaseUser;
import ru.yandex.partner.core.filter.db.DbFilter;
import ru.yandex.partner.core.filter.meta.MetaFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@CoreTest
class ModelFilterContainerTest {

    private static final Map<Class<?>, Class<?>> CONTAINER_FILTER_MAP = createMap();

    @Autowired
    List<ModelFilterContainer<?>> modelFilterContainerList;

    @TestFactory
    Stream<DynamicTest> testFactory() {
        return modelFilterContainerList.stream()
                .flatMap(container -> {
                    var filtersClass = CONTAINER_FILTER_MAP.entrySet().stream()
                            .filter(entry -> entry.getKey().isAssignableFrom(container.getModelClass()))
                            .map(Map.Entry::getValue).findAny();

                    if (filtersClass.isPresent()) {
                        var classes = getModelClasses(container);
                        return classes.stream()
                                .map(clazz -> DynamicTest.dynamicTest(clazz.getName(),
                                        () -> test(filtersClass.get(), clazz, container)));
                    } else {
                        return Stream.of();
                    }
                });

    }

    void test(Class<?> filterClass, Class<?> modelClass, ModelFilterContainer<?> modelFilterContainer)
            throws IllegalAccessException {

        var metaFilters = getMetaFilters(filterClass, modelClass);

        var exceptions = new ArrayList<Exception>();
        for (MetaFilter<?, ?> metaFilter : metaFilters) {
            try {
                var dbFilters =
                        ((ModelFilterContainer) modelFilterContainer).getDbFilter(modelClass, metaFilter);
                assertNotNull(dbFilters, "Got NULL for " + metaFilter + " for " + modelClass);
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        assertEquals(List.of(), exceptions, "Has exceptions");
    }

    @Test
    void notTestedContainers() {
        var list = modelFilterContainerList.stream()
                .filter(container -> {
                    var filtersClass = CONTAINER_FILTER_MAP.entrySet().stream()
                            .filter(entry -> entry.getKey().isAssignableFrom(container.getModelClass()))
                            .map(Map.Entry::getValue).findAny();

                    return filtersClass.isEmpty();
                })
                .map(container -> container.getClass())
                .toList();

        assertEquals(List.of(), list, "Has not tested containers. Please, add info in containerFilterMap");
    }

    /**
     * Тут пытаемся найти все конечные классы для modelFilterContainer.getModelClass()
     *
     * @param modelFilterContainer контейнер6 который будет тестироваться
     * @return список моделей для которых он актуален
     */
    private List<Class<?>> getModelClasses(ModelFilterContainer<?> modelFilterContainer) {
        Reflections reflections = new Reflections("ru.yandex.partner.core.entity");

        return Stream.concat(reflections.getSubTypesOf(modelFilterContainer.getModelClass()).stream(),
                        Stream.of(modelFilterContainer.getModelClass()))
                .filter(clazz -> !clazz.isInterface())
                .distinct()
                .toList();
    }

    private Set<MetaFilter<?, ?>> getMetaFilters(Class<?> filterClass, Class<?> modelClass)
            throws IllegalAccessException {

        var declaredFields = filterClass.getDeclaredFields();
        var metaFilters = Sets.<MetaFilter<?, ?>>newHashSetWithExpectedSize(declaredFields.length);
        for (Field declaredField : declaredFields) {
            var declaredFieldObject = declaredField.get(null);
            if (declaredFieldObject instanceof MetaFilter metaFilter
                    && (!(declaredFieldObject instanceof DbFilter))
                    && metaFilter.getModelClass().isAssignableFrom(modelClass)) {
                if (metaFilters.contains(metaFilter)) {
                    fail("Duplicate MetaFilter in " + filterClass + ". " + metaFilter);
                }
                metaFilters.add(metaFilter);
            }
        }
        return metaFilters;
    }

    private static Map<Class<?>, Class<?>> createMap() {
        var map = new HashMap<Class<?>, Class<?>>();
        map.put(BaseBlock.class, BlockFilters.class);
        map.put(BasePage.class, PageFilters.class);
        map.put(Assistant.class, AssistantFilters.class);
        map.put(BlockSequence.class, BlockSequenceFilters.class);
        map.put(CustomBkOptions.class, CustomBkOptionsFilters.class);
        map.put(BaseDesignTemplates.class, DesignTemplatesFilters.class);
        map.put(BaseDomain.class, DomainFilters.class);
        map.put(BaseMirror.class, MirrorFilters.class);
        map.put(BaseDsp.class, DspFilters.class);
        map.put(BaseUser.class, UserFilters.class);
        map.put(KvStoreFrontend.class, KvStoreFrontendFilters.class);
        map.put(Brand.class, BrandFilters.class);
        map.put(PiCategory.class, PiCategoryFilters.class);
        return map;
    }

}
