package ru.yandex.intranet.d.utils;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import reactor.util.function.Tuple2;

import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.model.accounts.AccountSpaceModel;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.model.resources.ResourceModel;
import ru.yandex.intranet.d.model.resources.ResourceSegmentSettingsModel;
import ru.yandex.intranet.d.model.resources.ResourceUnitsModel;

/**
 * DummyModels.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 09-09-2021
 */
public class DummyModels {
    private static final String GRPC_URI = "in-process:test";

    private DummyModels() {
    }

    public static ProviderModel providerModel() {
        return providerModel(GRPC_URI, null, false, false, true, true, false, false, false, false);
    }

    @SuppressWarnings("ParameterNumber")
    public static ProviderModel providerModel(String grpcUri, String restUri, boolean accountsSpacesSupported,
                                        boolean softDeleteSupported, boolean accountKeySupported,
                                        boolean accountDisplayNameSupported, boolean perAccountVersionSupported,
                                        boolean perProvisionVersionSupported, boolean perAccountLastUpdateSupported,
                                        boolean operationIdDeduplicationSupported) {
        return ProviderModel.builder()
                .id(UUID.randomUUID().toString())
                .grpcApiUri(grpcUri)
                .restApiUri(restUri)
                .destinationTvmId(42L)
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .sourceTvmId(42L)
                .serviceId(69L)
                .deleted(false)
                .readOnly(false)
                .multipleAccountsPerFolder(true)
                .accountTransferWithQuota(true)
                .managed(true)
                .key("test")
                .trackerComponentId(1L)
                .accountsSettings(AccountsSettingsModel.builder()
                        .displayNameSupported(accountDisplayNameSupported)
                        .keySupported(accountKeySupported)
                        .deleteSupported(true)
                        .softDeleteSupported(softDeleteSupported)
                        .moveSupported(true)
                        .renameSupported(true)
                        .perAccountVersionSupported(perAccountVersionSupported)
                        .perProvisionVersionSupported(perProvisionVersionSupported)
                        .perAccountLastUpdateSupported(perAccountLastUpdateSupported)
                        .perProvisionLastUpdateSupported(true)
                        .operationIdDeduplicationSupported(operationIdDeduplicationSupported)
                        .syncCoolDownDisabled(false)
                        .retryCoolDownDisabled(false)
                        .accountsSyncPageSize(1000L)
                        .build())
                .importAllowed(true)
                .accountsSpacesSupported(accountsSpacesSupported)
                .syncEnabled(true)
                .grpcTlsOn(true)
                .build();
    }

    @SuppressWarnings("ParameterNumber")
    public static ResourceModel resourceModel(
            String providerId,
            String key,
            String resourceTypeId,
            String unitsEnsembleId,
            Set<String> allowedUnitIds,
            String defaultUnitId,
            String baseUnitId
    ) {
        return resourceModel(
                providerId,
                key,
                resourceTypeId,
                unitsEnsembleId,
                allowedUnitIds,
                defaultUnitId,
                baseUnitId,
                builder -> {
                }
        );
    }

    @SuppressWarnings("ParameterNumber")
    public static ResourceModel resourceModel(
            String providerId,
            String key,
            String resourceTypeId,
            String unitsEnsembleId,
            Set<String> allowedUnitIds,
            String defaultUnitId,
            String baseUnitId,
            Consumer<ResourceModel.Builder> builderConsumer
    ) {
        ResourceModel.Builder builder = ResourceModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .key(key)
                .nameEn("Test")
                .nameRu("Test")
                .descriptionEn("Test")
                .descriptionRu("Test")
                .deleted(false)
                .unitsEnsembleId(unitsEnsembleId)
                .providerId(providerId)
                .resourceTypeId(resourceTypeId)
                .resourceUnits(new ResourceUnitsModel(allowedUnitIds, defaultUnitId, null))
                .managed(true)
                .orderable(true)
                .readOnly(false)
                .baseUnitId(baseUnitId);
        builderConsumer.accept(builder);
        return builder.build();
    }

    public static AccountSpaceModel accountSpaceModel(
            String providerId, String key, Set<Tuple2<String, String>> segments
    ) {
        return AccountSpaceModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setVersion(0L)
                .setOuterKeyInProvider(key)
                .setNameEn("Test")
                .setNameRu("Test")
                .setDescriptionEn("Test")
                .setDescriptionRu("Test")
                .setDeleted(false)
                .setProviderId(providerId)
                .setSegments(segments.stream().map(t -> new ResourceSegmentSettingsModel(t.getT1(), t.getT2()))
                        .collect(Collectors.toSet()))
                .build();
    }
}
