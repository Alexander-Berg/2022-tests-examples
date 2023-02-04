package ru.yandex.qe.dispenser.standalone;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import ru.yandex.qe.dispenser.domain.d.AmountDto;
import ru.yandex.qe.dispenser.domain.d.DApi;
import ru.yandex.qe.dispenser.domain.d.DeliverableFolderOperationDto;
import ru.yandex.qe.dispenser.domain.d.DeliverableMetaResponseDto;
import ru.yandex.qe.dispenser.domain.d.DeliverableResponseDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryAndProvideMetaRequestDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryDestinationRequestDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryDestinationResponseDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryRequestDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryResponseDto;
import ru.yandex.qe.dispenser.domain.d.DeliveryStatusResponseDto;
import ru.yandex.qe.dispenser.domain.d.ProvideAccountDto;
import ru.yandex.qe.dispenser.domain.d.ProvideOperationDto;
import ru.yandex.qe.dispenser.domain.d.ProvideOperationStatusDto;
import ru.yandex.qe.dispenser.domain.d.ProvideProviderDto;
import ru.yandex.qe.dispenser.domain.d.ProvideRequestDto;
import ru.yandex.qe.dispenser.domain.d.ProvideRequestedQuota;
import ru.yandex.qe.dispenser.domain.d.ProvideResourceDto;
import ru.yandex.qe.dispenser.domain.d.ProvideResponseDto;

public class MockDApi implements DApi {
    private final List<DeliveryDestinationRequestDto> destinationRequests = new ArrayList<>();
    private final List<List<String>> deliveryIds = new ArrayList<>();

    private Function<DeliveryRequestDto, DeliveryResponseDto> processor;
    private Function<ProvideRequestDto, ProvideResponseDto> provideProcessor;
    private DeliveryDestinationResponseDto destinations;
    private DeliveryStatusResponseDto status = new DeliveryStatusResponseDto(List.of());

    @Override
    public DeliveryResponseDto deliver(DeliveryRequestDto deliveryRequest) {
        final Function<DeliveryRequestDto, DeliveryResponseDto> deliveryFunction = processor != null ? processor
                : this::defaultDelivery;
        return deliveryFunction.apply(deliveryRequest);
    }

    @Override
    public DeliveryDestinationResponseDto findDestinations(String lang, DeliveryDestinationRequestDto request) {
        destinationRequests.add(request);
        return Objects.requireNonNullElseGet(destinations, () -> new DeliveryDestinationResponseDto(List.of(),
                List.of()));
    }

    @Override
    public DeliveryStatusResponseDto getStatuses(String lang, List<String> deliveryIds) {
        this.deliveryIds.add(deliveryIds);
        return status;
    }

    @Override
    public ProvideResponseDto provide(ProvideRequestDto provideRequestDto) {
        final Function<ProvideRequestDto, ProvideResponseDto> provideFunction = provideProcessor != null ? provideProcessor
                : this::defaultProvide;
        return provideFunction.apply(provideRequestDto);
    }

    private ProvideResponseDto defaultProvide(ProvideRequestDto provideRequestDto) {
        ProvideResponseDto.Builder builder = ProvideResponseDto.builder();

        builder.deliveryId(UUID.randomUUID().toString());
        List<ProvideOperationDto> provideOperationDto = new ArrayList<>();

        provideRequestDto.getDeliverables().forEach(deliveryDto ->
                provideOperationDto.add(ProvideOperationDto.builder()
                        .operationId(UUID.randomUUID().toString())
                        .accountId(deliveryDto.getAccountId())
                        .providerId(deliveryDto.getProviderId())
                        .requestedQuota(ProvideRequestedQuota.builder()
                                .resourceId(deliveryDto.getResourceId())
                                .amount(AmountDto.builder()
                                        .readableAmount(String.valueOf(deliveryDto.getDelta().getAmount()))
                                        .readableUnit(deliveryDto.getDelta().getUnitKey())
                                        .rawAmount(String.valueOf(deliveryDto.getDelta().getAmount()))
                                        .rawUnit(deliveryDto.getDelta().getUnitKey())
                                        .forEditAmount(String.valueOf(deliveryDto.getDelta().getAmount()))
                                        .forEditUnitId(deliveryDto.getDelta().getUnitKey())
                                        .amountInMinAllowedUnit(String.valueOf(deliveryDto.getDelta().getAmount()))
                                        .minAllowedUnit(deliveryDto.getDelta().getUnitKey())
                                        .build())
                                .build())
                        .createDateTime(Instant.now())
                        .updateDateTime(Instant.now())
                        .status(ProvideOperationStatusDto.SUCCESS)
                        .meta(DeliveryAndProvideMetaRequestDto.builder()
                                .quotaRequestId(deliveryDto.getMeta().getQuotaRequestId())
                                .campaignId(deliveryDto.getMeta().getCampaignId())
                                .addBigOrderId(deliveryDto.getMeta().getBigOrderId())
                                .build())
                        .addFolderOperationLog(DeliverableFolderOperationDto.builder()
                                .id(UUID.randomUUID().toString())
                                .folderId(deliveryDto.getFolderId())
                                .timestamp(Instant.now())
                                .build())
                        .build()
                ));
        builder.operations(provideOperationDto);

        Map<String, ProvideAccountDto> accounts = new HashMap<>();
        Map<String, ProvideProviderDto> providers = new HashMap<>();
        Map<String, ProvideResourceDto> resources = new HashMap<>();

        provideOperationDto.forEach(operation -> {
            accounts.computeIfAbsent(operation.getAccountId(), k -> ProvideAccountDto.builder()
                    .accountId(k)
                    .providerId(operation.getProviderId())
                    .displayName(k)
                    .build());
            providers.computeIfAbsent(operation.getProviderId(), k -> ProvideProviderDto.builder()
                    .providerId(k)
                    .displayName(k)
                    .build());
            operation.getRequestedQuotas().forEach(requestedQuota ->
                    resources.computeIfAbsent(requestedQuota.getResourceId(), k -> ProvideResourceDto.builder()
                            .resourceId(k)
                            .displayName(k)
                            .segments(Set.of())
                            .build()));
        });

        return builder.accounts(new ArrayList<>(accounts.values()))
                .providers(new ArrayList<>(providers.values()))
                .resources(new ArrayList<>(resources.values()))
                .build();
    }

    public DeliveryResponseDto defaultDelivery(DeliveryRequestDto deliveryRequest) {
        DeliveryResponseDto.Builder builder = DeliveryResponseDto.builder();
        deliveryRequest.getDeliverables().forEach(deliverable -> {
            DeliverableResponseDto.Builder deliverableBuilder = DeliverableResponseDto.builder();
            deliverable.getFolderId().ifPresent(deliverableBuilder::folderId);
            deliverable.getServiceId().ifPresent(deliverableBuilder::serviceId);
            deliverableBuilder.resourceId(deliverable.getResourceId());
            deliverableBuilder.meta(DeliverableMetaResponseDto.builder()
                    .setBigOrderId(deliverable.getMeta().getBigOrderId())
                    .setCampaignId(deliverable.getMeta().getCampaignId())
                    .setQuotaRequestId(deliverable.getMeta().getQuotaRequestId())
                    .build());
            DeliverableFolderOperationDto.Builder folderOperationBuilder = DeliverableFolderOperationDto.builder();
            if (deliverable.getFolderId().isPresent()) {
                folderOperationBuilder.folderId(deliverable.getFolderId().get());
            } else {
                folderOperationBuilder.folderId(UUID.randomUUID().toString());
            }
            folderOperationBuilder.id(UUID.randomUUID().toString());
            folderOperationBuilder.timestamp(Instant.now());
            deliverableBuilder.folderOperationLog(folderOperationBuilder.build());
            builder.addDeliverable(deliverableBuilder.build());
        });
        return builder.build();
    }

    public void reset() {
        processor = null;
        provideProcessor = null;
    }

    public void setDeliveryProcessor(Function<DeliveryRequestDto, DeliveryResponseDto> processor) {
        this.processor = processor;
    }

    public void setProvideProcessor(Function<ProvideRequestDto, ProvideResponseDto> provideProcessor) {
        this.provideProcessor = provideProcessor;
    }

    public void setDestinations(DeliveryDestinationResponseDto destinations) {
        this.destinations = destinations;
    }

    public void clearDestinationRequests() {
        destinationRequests.clear();
    }

    public List<DeliveryDestinationRequestDto> getDestinationRequests() {
        return destinationRequests;
    }

    public void setStatus(DeliveryStatusResponseDto status) {
        this.status = status;
    }

    public List<List<String>> getDeliveryIds() {
        return deliveryIds;
    }

    public void clearDeliveryIds() {
        deliveryIds.clear();
    }

}
