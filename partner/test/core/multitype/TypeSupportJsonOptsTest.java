package ru.yandex.partner.core.multitype;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import org.jooq.Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.Model;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.model.BlockWithInternalMobileRtbOpts;
import ru.yandex.partner.core.entity.block.model.BlockWithInternalRtbOpts;
import ru.yandex.partner.core.entity.block.model.BlockWithMobileRtbOpts;
import ru.yandex.partner.core.entity.block.model.BlockWithRtbOpts;
import ru.yandex.partner.core.entity.block.model.ContentBlock;
import ru.yandex.partner.core.entity.block.model.InternalMobileRtbBlock;
import ru.yandex.partner.core.entity.block.model.InternalRtbBlock;
import ru.yandex.partner.core.entity.block.model.MobileRtbBlock;
import ru.yandex.partner.core.entity.block.model.RtbBlock;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.model.InternalContextPage;
import ru.yandex.partner.core.entity.page.model.InternalMobileApp;
import ru.yandex.partner.core.entity.page.model.MobileAppSettings;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multitype.repository.PartnerRepositoryTypeSupport;
import ru.yandex.partner.core.multitype.repository.PartnerRepositoryTypeSupportFacade;
import ru.yandex.partner.core.multitype.repository.PartnerRepositoryTypeSupportWithMapper;
import ru.yandex.partner.core.service.entitymanager.EntityManager;
import ru.yandex.partner.core.service.entitymanager.EntityMeta;

import static ru.yandex.partner.dbschema.partner.Tables.INTERNAL_MOBILE_APP;
import static ru.yandex.partner.dbschema.partner.Tables.PAGES;
import static ru.yandex.partner.dbschema.partner.tables.ContextOnSiteRtb.CONTEXT_ON_SITE_RTB;

@CoreTest
@ExtendWith(MySqlRefresher.class)
class TypeSupportJsonOptsTest {

    Map<Class<?>, OptsInfo> optsMap = Map.of(
            RtbBlock.class, new OptsInfo(BlockWithRtbOpts.class, CONTEXT_ON_SITE_RTB.OPTS),
            InternalRtbBlock.class, new OptsInfo(BlockWithInternalRtbOpts.class, CONTEXT_ON_SITE_RTB.OPTS),
            MobileRtbBlock.class, new OptsInfo(BlockWithMobileRtbOpts.class, CONTEXT_ON_SITE_RTB.OPTS),
            ContextPage.class, new OptsInfo(null, PAGES.OPTS),         // TODO
            InternalContextPage.class, new OptsInfo(null, PAGES.OPTS), // добавить класс, когда появится
            MobileAppSettings.class, new OptsInfo(null, PAGES.OPTS),    // маппинг полей из opts в модели
            ContentBlock.class, new OptsInfo(null, CONTEXT_ON_SITE_RTB.OPTS), // TODO добавить
            InternalMobileApp.class, new OptsInfo(null, INTERNAL_MOBILE_APP.OPTS), // TODO добавить
            InternalMobileRtbBlock.class, new OptsInfo(BlockWithInternalMobileRtbOpts.class, CONTEXT_ON_SITE_RTB.OPTS)
    );

    @Autowired
    EntityManager entityManager;

    @Autowired
    List<PartnerRepositoryTypeSupportFacade<?, ?, ?, ?>> typeSupportFacades;

    Map<Class<?>, List<PartnerRepositoryTypeSupportFacade<?, ?, ?, ?>>> modelClassToFacadeMap =
            Maps.newHashMapWithExpectedSize(optsMap.size());


    Map<Class<?>, List<PartnerRepositoryTypeSupportFacade<?, ?, ?, ?>>> optsClassToFacadeMap =
            Maps.newHashMapWithExpectedSize(optsMap.size());

    @BeforeEach
    void setUp() {
        for (PartnerRepositoryTypeSupportFacade<?, ?, ?, ?> typeSupportFacade : typeSupportFacades) {
            for (var entry : optsMap.entrySet()) {
                var modelClass = entry.getKey();
                var optsClass = entry.getValue().optsClass;
                if (optsClass == null) {
                    continue;
                }
                if (!typeSupportFacade.getSupportsByClass(modelClass).isEmpty()) {
                    modelClassToFacadeMap
                            .computeIfAbsent(modelClass, (clazz) -> new ArrayList<>())
                            .add(typeSupportFacade);
                }

                if (!typeSupportFacade.getSupportsByClass(optsClass).isEmpty()) {
                    optsClassToFacadeMap
                            .computeIfAbsent(optsClass, (clazz) -> new ArrayList<>())
                            .add(typeSupportFacade);
                }
            }
        }
    }

    @Test
    void checkAllModelTested() {
        entityManager.getAllEntityMetas()
                .stream()
                .map(EntityMeta::getEntityClass)
                .filter(ec -> Objects.nonNull(optsMap.get(ec).optsClass))
                .forEach(entityClass ->
                        Assertions.assertTrue(
                                optsMap.containsKey(entityClass),
                                "Please add info about model to optsMap. Model class = " + entityClass
                        )
                );
    }

    @Test
    void checkOpts() {
        for (var entry : modelClassToFacadeMap.entrySet()) {
            var modelClass = entry.getKey();
            for (PartnerRepositoryTypeSupportFacade<?, ?, ?, ?> supportFacade : entry.getValue()) {
                var optsInfo = optsMap.get(modelClass);
                checkOptsClassImplementAllParts(modelClass, supportFacade, optsInfo);
                checkCountOfProperties(supportFacade, optsInfo);
            }
        }
    }

    private void checkOptsClassImplementAllParts(Class<?> modelClass,
                                                 PartnerRepositoryTypeSupportFacade<?, ?, ?, ?> supportFacade,
                                                 OptsInfo optsInfo) {

        var classes = getOptsRepositoriesTypeClasses(optsInfo.optsField,
                supportFacade.getSupportsByClass(modelClass));
        Assertions.assertTrue(classes.contains(optsInfo.optsClass),
                String.format("OptsClass should be able to read optsField. " +
                                "(ModelClass, OptsClass, OptsField) = (%s,%s,%s)",
                        modelClass, optsInfo.optsClass, optsInfo.optsField));

        for (Class<?> aClass : classes) {
            Assertions.assertTrue(
                    aClass.isAssignableFrom(optsInfo.optsClass),
                    String.format(
                            "Class can read optsField, but OptsClass doesn't implement it. Invalid class = %s" +
                                    ". " +
                                    "(ModelClass, OptsClass, OptsField) = (%s,%s,%s)",
                            aClass, modelClass, optsInfo.optsClass, optsInfo.optsField));
        }
    }

    private void checkCountOfProperties(PartnerRepositoryTypeSupportFacade<?, ?, ?, ?> supportFacade,
                                        OptsInfo optsInfo) {
        int optsClassPropertiesCount = 0;
        int optsPartClassesPropertiesCount = 0;

        for (PartnerRepositoryTypeSupport<?, ?, ?> supportsByClass :
                supportFacade.getSupportsByClass(optsInfo.optsClass)) {

            if (supportsByClass.getTypeClass().equals(optsInfo.optsClass)) {
                optsClassPropertiesCount += supportsByClass.getAffectedModelProperties().size();
            } else if (supportsByClass instanceof PartnerRepositoryTypeSupportWithMapper supportWithMapper
                    && supportWithMapper.getJooqMapper().canReadAtLeastOneProperty(List.of(optsInfo.optsField))) {
                optsPartClassesPropertiesCount += supportsByClass.getAffectedModelProperties().size();
            }
        }

        Assertions.assertEquals(optsPartClassesPropertiesCount, optsClassPropertiesCount,
                "Parts of opts class and Opts class has different model properties count. " + optsInfo);
    }

    private Set<Class<?>> getOptsRepositoriesTypeClasses(
            Field<?> field,
            List<? extends PartnerRepositoryTypeSupport<?, ?, ?>> supports) {
        return supports
                .stream()
                .map(support -> {
                    if (support instanceof PartnerRepositoryTypeSupportWithMapper typeSupportWithMapper) {
                        return typeSupportWithMapper;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(typeSupportWithMapper ->
                        typeSupportWithMapper.getJooqMapper().canReadAtLeastOneProperty(List.of(field)))
                .map(support -> (Class<?>) support.getTypeClass())
                .collect(Collectors.toSet());
    }

    private record OptsInfo(Class<? extends Model> optsClass, Field<?> optsField) {
    }
}
