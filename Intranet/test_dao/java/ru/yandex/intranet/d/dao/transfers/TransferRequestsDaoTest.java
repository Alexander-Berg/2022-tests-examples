package ru.yandex.intranet.d.dao.transfers;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.transfers.FoldersResponsible;
import ru.yandex.intranet.d.model.transfers.QuotaTransfer;
import ru.yandex.intranet.d.model.transfers.ResourceQuotaTransfer;
import ru.yandex.intranet.d.model.transfers.ServiceResponsible;
import ru.yandex.intranet.d.model.transfers.TransferNotified;
import ru.yandex.intranet.d.model.transfers.TransferParameters;
import ru.yandex.intranet.d.model.transfers.TransferRequestByServiceModel;
import ru.yandex.intranet.d.model.transfers.TransferRequestModel;
import ru.yandex.intranet.d.model.transfers.TransferRequestStatus;
import ru.yandex.intranet.d.model.transfers.TransferRequestSubtype;
import ru.yandex.intranet.d.model.transfers.TransferRequestType;
import ru.yandex.intranet.d.model.transfers.TransferResponsible;
import ru.yandex.intranet.d.model.transfers.TransferVotes;

/**
 * Transfer requests DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class TransferRequestsDaoTest {

    @Autowired
    private TransferRequestsDao dao;
    @Autowired
    private TransferRequestByServiceDao transferRequestByServiceDao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void upsertAndGetById() {
        TransferRequestModel transferRequest = TransferRequestModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .summary("Summary")
                .description("Description")
                .trackerIssueKey("TEST-1")
                .type(TransferRequestType.QUOTA_TRANSFER)
                .subtype(TransferRequestSubtype.DEFAULT_QUOTA_TRANSFER)
                .status(TransferRequestStatus.PENDING)
                .createdBy("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .updatedBy(null)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .updatedAt(null)
                .appliedAt(null)
                .parameters(TransferParameters.builder()
                        .addQuotaTransfers(
                                QuotaTransfer.builder()
                                    .serviceId(2)
                                    .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                    .addTransfer(ResourceQuotaTransfer.builder()
                                            .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                            .delta(10L)
                                            .build())
                                    .build(),
                                QuotaTransfer.builder()
                                    .serviceId(1)
                                    .folderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                    .addTransfer(ResourceQuotaTransfer.builder()
                                            .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                            .delta(-10L)
                                            .build())
                                    .build())
                        .build())
                .responsible(TransferResponsible.builder()
                        .addResponsibles(
                                FoldersResponsible.builder()
                                        .addFolderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(2L)
                                                .addResponsibleId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                                                .build())
                                        .build(),
                                FoldersResponsible.builder()
                                        .addFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(1L)
                                                .addResponsibleId("e484b490-b9de-4f39-b712-c8c2f73e0612")
                                                .build())
                                        .build())
                        .build())
                .votes(TransferVotes.builder().build())
                .applicationDetails(null)
                .nextHistoryOrder(1L)
                .transferNotified(TransferNotified.builder()
                        .notifiedUserIds(Set.of(
                                "0b204534-d0ec-452d-99fe-a3d1da5a49a9",
                                "e484b490-b9de-4f39-b712-c8c2f73e0612")
                        ).build()
                ).build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertOneRetryable(txSession, transferRequest)))
                .block();
        Optional<TransferRequestModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, transferRequest.getId(), transferRequest.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(transferRequest), result);
    }

    @Test
    public void upsertAndGetByIds() {
        TransferRequestModel requestOne = TransferRequestModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .summary("Summary-1")
                .description("Description-1")
                .trackerIssueKey("TEST-1")
                .type(TransferRequestType.QUOTA_TRANSFER)
                .subtype(TransferRequestSubtype.DEFAULT_QUOTA_TRANSFER)
                .status(TransferRequestStatus.PENDING)
                .createdBy("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .updatedBy(null)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .updatedAt(null)
                .appliedAt(null)
                .parameters(TransferParameters.builder()
                        .addQuotaTransfers(
                                QuotaTransfer.builder()
                                        .serviceId(2)
                                        .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(10L)
                                                .build())
                                        .build(),
                                QuotaTransfer.builder()
                                        .serviceId(1)
                                        .folderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(-10L)
                                                .build())
                                        .build())
                        .build())
                .responsible(TransferResponsible.builder()
                        .addResponsibles(
                                FoldersResponsible.builder()
                                        .addFolderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(2L)
                                                .addResponsibleId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                                                .build())
                                        .build(),
                                FoldersResponsible.builder()
                                        .addFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(1L)
                                                .addResponsibleId("e484b490-b9de-4f39-b712-c8c2f73e0612")
                                                .build())
                                        .build())
                        .build())
                .votes(TransferVotes.builder().build())
                .applicationDetails(null)
                .nextHistoryOrder(1L)
                .transferNotified(TransferNotified.builder()
                        .notifiedUserIds(Set.of(
                                "0b204534-d0ec-452d-99fe-a3d1da5a49a9",
                                "e484b490-b9de-4f39-b712-c8c2f73e0612")
                        ).build()
                ).build();
        TransferRequestModel requestTwo = TransferRequestModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .summary("Summary-2")
                .description("Description-2")
                .trackerIssueKey("TEST-2")
                .type(TransferRequestType.QUOTA_TRANSFER)
                .subtype(TransferRequestSubtype.DEFAULT_QUOTA_TRANSFER)
                .status(TransferRequestStatus.PENDING)
                .createdBy("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .updatedBy(null)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .updatedAt(null)
                .appliedAt(null)
                .parameters(TransferParameters.builder()
                        .addQuotaTransfers(
                                QuotaTransfer.builder()
                                        .serviceId(2)
                                        .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(10L)
                                                .build())
                                        .build(),
                                QuotaTransfer.builder()
                                        .serviceId(1)
                                        .folderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(-10L)
                                                .build())
                                        .build())
                        .build())
                .responsible(TransferResponsible.builder()
                        .addResponsibles(
                                FoldersResponsible.builder()
                                        .addFolderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(2L)
                                                .addResponsibleId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                                                .build())
                                        .build(),
                                FoldersResponsible.builder()
                                        .addFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(1L)
                                                .addResponsibleId("e484b490-b9de-4f39-b712-c8c2f73e0612")
                                                .build())
                                        .build())
                        .build())
                .votes(TransferVotes.builder().build())
                .applicationDetails(null)
                .nextHistoryOrder(1L)
                .transferNotified(TransferNotified.builder()
                        .notifiedUserIds(Set.of(
                                "0b204534-d0ec-452d-99fe-a3d1da5a49a9",
                                "e484b490-b9de-4f39-b712-c8c2f73e0612")
                        ).build()
                ).build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, List.of(requestOne, requestTwo))))
                .block();
        List<TransferRequestModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(requestOne.getId(), requestTwo.getId()),
                                Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of(requestOne, requestTwo), new HashSet<>(result));
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void upsertAndGetByServiceAndDate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        TransferRequestModel requestOne = TransferRequestModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .summary("Summary-1")
                .description("Description-1")
                .trackerIssueKey("TEST-1")
                .type(TransferRequestType.QUOTA_TRANSFER)
                .status(TransferRequestStatus.PENDING)
                .createdBy("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .updatedBy(null)
                .createdAt(now)
                .updatedAt(null)
                .appliedAt(null)
                .parameters(TransferParameters.builder()
                        .addQuotaTransfers(
                                QuotaTransfer.builder()
                                        .serviceId(2)
                                        .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(10L)
                                                .build())
                                        .build(),
                                QuotaTransfer.builder()
                                        .serviceId(1)
                                        .folderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(-10L)
                                                .build())
                                        .build())
                        .build())
                .responsible(TransferResponsible.builder()
                        .addResponsibles(
                                FoldersResponsible.builder()
                                        .addFolderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(2L)
                                                .addResponsibleId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                                                .build())
                                        .build(),
                                FoldersResponsible.builder()
                                        .addFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(1L)
                                                .addResponsibleId("e484b490-b9de-4f39-b712-c8c2f73e0612")
                                                .build())
                                        .build())
                        .build())
                .votes(TransferVotes.builder().build())
                .applicationDetails(null)
                .nextHistoryOrder(1L)
                .transferNotified(TransferNotified.builder()
                        .notifiedUserIds(Set.of(
                                "0b204534-d0ec-452d-99fe-a3d1da5a49a9",
                                "e484b490-b9de-4f39-b712-c8c2f73e0612")
                        ).build()
                ).build();
        TransferRequestModel requestTwo = TransferRequestModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .summary("Summary-2")
                .description("Description-2")
                .trackerIssueKey("TEST-2")
                .type(TransferRequestType.QUOTA_TRANSFER)
                .status(TransferRequestStatus.PENDING)
                .createdBy("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .updatedBy(null)
                .createdAt(Instant.EPOCH)
                .updatedAt(now)
                .appliedAt(now)
                .parameters(TransferParameters.builder()
                        .addQuotaTransfers(
                                QuotaTransfer.builder()
                                        .serviceId(2)
                                        .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(10L)
                                                .build())
                                        .build(),
                                QuotaTransfer.builder()
                                        .serviceId(1)
                                        .folderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(-10L)
                                                .build())
                                        .build())
                        .build())
                .responsible(TransferResponsible.builder()
                        .addResponsibles(
                                FoldersResponsible.builder()
                                        .addFolderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(2L)
                                                .addResponsibleId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                                                .build())
                                        .build(),
                                FoldersResponsible.builder()
                                        .addFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(1L)
                                                .addResponsibleId("e484b490-b9de-4f39-b712-c8c2f73e0612")
                                                .build())
                                        .build())
                        .build())
                .votes(TransferVotes.builder().build())
                .applicationDetails(null)
                .nextHistoryOrder(1L)
                .transferNotified(TransferNotified.builder()
                        .notifiedUserIds(Set.of(
                                "0b204534-d0ec-452d-99fe-a3d1da5a49a9",
                                "e484b490-b9de-4f39-b712-c8c2f73e0612")
                        ).build()
                ).build();
        TransferRequestModel requestThree = TransferRequestModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .summary("Summary-3")
                .description("Description-3")
                .trackerIssueKey("TEST-3")
                .type(TransferRequestType.QUOTA_TRANSFER)
                .status(TransferRequestStatus.PENDING)
                .createdBy("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .updatedBy(null)
                .createdAt(Instant.EPOCH)
                .updatedAt(null)
                .appliedAt(null)
                .parameters(TransferParameters.builder()
                        .addQuotaTransfers(
                                QuotaTransfer.builder()
                                        .serviceId(2)
                                        .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(10L)
                                                .build())
                                        .build(),
                                QuotaTransfer.builder()
                                        .serviceId(1)
                                        .folderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(-10L)
                                                .build())
                                        .build())
                        .build())
                .responsible(TransferResponsible.builder()
                        .addResponsibles(
                                FoldersResponsible.builder()
                                        .addFolderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(2L)
                                                .addResponsibleId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                                                .build())
                                        .build(),
                                FoldersResponsible.builder()
                                        .addFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(1L)
                                                .addResponsibleId("e484b490-b9de-4f39-b712-c8c2f73e0612")
                                                .build())
                                        .build())
                        .build())
                .votes(TransferVotes.builder().build())
                .applicationDetails(null)
                .nextHistoryOrder(1L)
                .transferNotified(TransferNotified.builder()
                        .notifiedUserIds(Set.of(
                                "0b204534-d0ec-452d-99fe-a3d1da5a49a9",
                                "e484b490-b9de-4f39-b712-c8c2f73e0612")
                        ).build()
                ).build();

        TransferRequestByServiceModel requestOneByService = TransferRequestByServiceModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .serviceId(1L)
                .status(requestOne.getStatus())
                .createdAt(requestOne.getCreatedAt())
                .transferRequestId(requestOne.getId())
                .build();
        TransferRequestByServiceModel requestTwoByService = TransferRequestByServiceModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .serviceId(1L)
                .status(requestTwo.getStatus())
                .createdAt(requestTwo.getCreatedAt())
                .transferRequestId(requestTwo.getId())
                .build();
        TransferRequestByServiceModel requestThreeByService = TransferRequestByServiceModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .serviceId(1L)
                .status(requestThree.getStatus())
                .createdAt(requestThree.getCreatedAt())
                .transferRequestId(requestThree.getId())
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> dao.upsertAllRetryable(txSession,
                                        List.of(requestOne, requestTwo, requestThree))))
                .block();
        ydbTableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> transferRequestByServiceDao.upsertAllRetryable(txSession,
                                        List.of(requestOneByService, requestTwoByService, requestThreeByService))))
                .block();

        List<TransferRequestModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                        session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                                txSession -> dao.getByServicesAndDate(txSession, List.of(1L),
                                        Tenants.DEFAULT_TENANT_ID, Instant.now().minus(Duration.ofDays(2)),
                                        Instant.now().plus(Duration.ofDays(2)))))
                .block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of(requestOne, requestTwo), new HashSet<>(result));
    }

    @Test
    public void updateTrackerIssueKey() {
        TransferRequestModel transferRequest = TransferRequestModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .version(0L)
                .summary("Summary")
                .description("Description")
                .type(TransferRequestType.QUOTA_TRANSFER)
                .subtype(TransferRequestSubtype.DEFAULT_QUOTA_TRANSFER)
                .status(TransferRequestStatus.PENDING)
                .createdBy("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .updatedBy(null)
                .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .updatedAt(null)
                .appliedAt(null)
                .parameters(TransferParameters.builder()
                        .addQuotaTransfers(
                                QuotaTransfer.builder()
                                        .serviceId(2)
                                        .folderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(10L)
                                                .build())
                                        .build(),
                                QuotaTransfer.builder()
                                        .serviceId(1)
                                        .folderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addTransfer(ResourceQuotaTransfer.builder()
                                                .resourceId("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                                                .delta(-10L)
                                                .build())
                                        .build())
                        .build())
                .responsible(TransferResponsible.builder()
                        .addResponsibles(
                                FoldersResponsible.builder()
                                        .addFolderId("f2fe5d5d-b19f-44f6-9bf7-b5a232b81846")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(2L)
                                                .addResponsibleId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                                                .build())
                                        .build(),
                                FoldersResponsible.builder()
                                        .addFolderId("f714c483-c347-41cc-91d0-c6722f5daac7")
                                        .addResponsible(ServiceResponsible.builder()
                                                .serviceId(1L)
                                                .addResponsibleId("e484b490-b9de-4f39-b712-c8c2f73e0612")
                                                .build())
                                        .build())
                        .build())
                .votes(TransferVotes.builder().build())
                .applicationDetails(null)
                .nextHistoryOrder(1L)
                .transferNotified(TransferNotified.builder()
                        .notifiedUserIds(Set.of(
                                "0b204534-d0ec-452d-99fe-a3d1da5a49a9",
                                "e484b490-b9de-4f39-b712-c8c2f73e0612")
                        ).build()
                ).build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertOneRetryable(txSession, transferRequest)))
                .block();
        Optional<TransferRequestModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, transferRequest.getId(), transferRequest.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(transferRequest), result);

        String trackerIssueKey = "DISPENSERTREQ-1";
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.updateTrackerIssueKeyRetryable(txSession, transferRequest.getId(),
                                transferRequest.getTenantId(), trackerIssueKey)))
                .block();
        TransferRequestModel updatedTransferRequest = TransferRequestModel.builder(transferRequest)
                .trackerIssueKey(trackerIssueKey)
                .build();
        Optional<TransferRequestModel> updatedResult = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, transferRequest.getId(), transferRequest.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(updatedTransferRequest), updatedResult);
    }

}
