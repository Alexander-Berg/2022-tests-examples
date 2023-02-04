package ru.yandex.intranet.d.dao.transfers;

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
import ru.yandex.intranet.d.model.transfers.TransferParameters;
import ru.yandex.intranet.d.model.transfers.TransferRequestEventType;
import ru.yandex.intranet.d.model.transfers.TransferRequestHistoryFields;
import ru.yandex.intranet.d.model.transfers.TransferRequestHistoryModel;
import ru.yandex.intranet.d.model.transfers.TransferRequestStatus;
import ru.yandex.intranet.d.model.transfers.TransferRequestType;
import ru.yandex.intranet.d.model.transfers.TransferResponsible;

/**
 * Transfer request history DAO test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class TransferRequestHistoryDaoTest {

    @Autowired
    private TransferRequestHistoryDao dao;
    @Autowired
    private YdbTableClient ydbTableClient;

    @Test
    public void upsertAndGetById() {
        TransferRequestHistoryModel transferRequest = TransferRequestHistoryModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .transferRequestId(UUID.randomUUID().toString())
                .type(TransferRequestEventType.CREATED)
                .timestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .authorId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .oldFields(null)
                .newFields(TransferRequestHistoryFields.builder()
                        .summary("Summary")
                        .description("Description")
                        .version(0L)
                        .status(TransferRequestStatus.PENDING)
                        .type(TransferRequestType.QUOTA_TRANSFER)
                        .trackerIssueKey("TEST-1")
                        .build())
                .oldParameters(null)
                .oldResponsible(null)
                .oldVotes(null)
                .oldApplicationDetails(null)
                .newParameters(TransferParameters.builder()
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
                .newResponsible(TransferResponsible.builder()
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
                .newVotes(null)
                .newApplicationDetails(null)
                .order(0L)
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertOneRetryable(txSession, transferRequest)))
                .block();
        Optional<TransferRequestHistoryModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getById(txSession, transferRequest.getIdentity(),
                                transferRequest.getTenantId())))
                .block();
        Assertions.assertEquals(Optional.of(transferRequest), result);
    }

    @Test
    public void upsertAndGetByIds() {
        TransferRequestHistoryModel requestOne = TransferRequestHistoryModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .transferRequestId(UUID.randomUUID().toString())
                .type(TransferRequestEventType.CREATED)
                .timestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .authorId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .oldFields(null)
                .newFields(TransferRequestHistoryFields.builder()
                        .summary("Summary-1")
                        .description("Description-1")
                        .version(0L)
                        .status(TransferRequestStatus.PENDING)
                        .type(TransferRequestType.QUOTA_TRANSFER)
                        .trackerIssueKey("TEST-1")
                        .build())
                .oldParameters(null)
                .oldResponsible(null)
                .oldVotes(null)
                .oldApplicationDetails(null)
                .newParameters(TransferParameters.builder()
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
                .newResponsible(TransferResponsible.builder()
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
                .newVotes(null)
                .newApplicationDetails(null)
                .order(0L)
                .build();
        TransferRequestHistoryModel requestTwo = TransferRequestHistoryModel.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .transferRequestId(UUID.randomUUID().toString())
                .type(TransferRequestEventType.CREATED)
                .timestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .authorId("0b204534-d0ec-452d-99fe-a3d1da5a49a9")
                .oldFields(null)
                .newFields(TransferRequestHistoryFields.builder()
                        .summary("Summary-2")
                        .description("Description-2")
                        .version(0L)
                        .status(TransferRequestStatus.PENDING)
                        .type(TransferRequestType.QUOTA_TRANSFER)
                        .trackerIssueKey("TEST-2")
                        .build())
                .oldParameters(null)
                .oldResponsible(null)
                .oldVotes(null)
                .oldApplicationDetails(null)
                .newParameters(TransferParameters.builder()
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
                .newResponsible(TransferResponsible.builder()
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
                .newVotes(null)
                .newApplicationDetails(null)
                .order(0L)
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.upsertAllRetryable(txSession, List.of(requestOne, requestTwo))))
                .block();
        List<TransferRequestHistoryModel> result = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> dao.getByIds(txSession, List.of(requestOne.getIdentity(),
                                requestTwo.getIdentity()), Tenants.DEFAULT_TENANT_ID)))
                .block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of(requestOne, requestTwo), new HashSet<>(result));
    }

}
