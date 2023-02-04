package ru.yandex.intranet.d.web.api.quotas;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogCommentDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.WithTenant;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogCommentModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.quotas.QuotaTransferInputDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.intranet.d.TestResources.YP_CPU_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;

/**
 * BulkQuotaMoveTest
 *
 * @author Denis Blokhin <denblo@yandex-team.ru>
 */
@IntegrationTest
public class BulkQuotaMoveTest {
    @Autowired
    private WebTestClient webClient;

    @Autowired
    private YdbTableClient ydbTableClient;

    @Autowired
    private QuotasDao quotasDao;

    @Autowired
    private FolderDao folderDao;

    @Autowired
    private FolderOperationLogDao folderOperationLogDao;

    @Autowired
    private FolderOperationLogCommentDao folderOperationLogCommentDao;


    private ErrorCollectionDto expectErrors(int code, QuotaTransferInputDto body) {
        return webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .value((c) -> assertEquals(code, c))
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    public void transfersAmountShouldBeValidated() {
        ErrorCollectionDto errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(),
                null
        ));

        assertEquals(Map.of("transfers", Set.of("Field is required.")), errors.getFieldErrors());

        errors = expectErrors(422, new QuotaTransferInputDto(
                Collections.nCopies(1001, new QuotaTransferInputDto.Transfer(null, null, null, null, null)),
                null
        ));

        assertEquals(Map.of("transfers", Set.of("Transfers count more than 1000.")), errors.getFieldErrors());
    }

    @Test
    public void invalidFoldersMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(404, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID + "invalid",
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Folder with id 'f714c483-c347-41cc-91d0-c6722f5daac7invalid' not found."),
                errors.getErrors());

    }

    @Test
    public void missingFieldMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                null,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Source folder id, destination folder id and provider id required for transfer."),
                errors.getErrors());

        errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_2_ID,
                                null,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Source folder id, destination folder id and provider id required for transfer."),
                errors.getErrors());

        errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        null,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Source folder id, destination folder id and provider id required for transfer."),
                errors.getErrors());
    }

    @Test
    public void invalidProvidersMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(404, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID + "invalid",
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Resource with provider id '96e779cf-7d3f-4e74-ba41-c2acc7f04235invalid' " +
                "and resource id 'ef333da9-b076-42f5-b7f5-84cd04ab7fcc' not found."), errors.getErrors());

    }

    @Test
    public void invalidResourcesMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(404, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN + "invalid",
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Resource with provider id '96e779cf-7d3f-4e74-ba41-c2acc7f04235' " +
                "and resource id 'ef333da9-b076-42f5-b7f5-84cd04ab7fccinvalid' not found."), errors.getErrors());

    }

    @Test
    public void invalidUnitsMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(404, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "exabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Unit for resource 'yp_hdd_man' and key 'exabytes' not found."), errors.getErrors());
    }

    @Test
    public void invalidResourceTypesMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(404, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        null,
                                        new QuotaTransferInputDto.ExternalResourceId(
                                                "hdd2", List.of()
                                        )
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Resource type with provider id '96e779cf-7d3f-4e74-ba41-c2acc7f04235' " +
                "and key 'hdd2' not found."), errors.getErrors());
    }

    @Test
    public void invalidExternalResourceMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(404, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        null,
                                        new QuotaTransferInputDto.ExternalResourceId(
                                                "hdd", List.of()
                                        )
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Resource with type id '44f93060-e367-44e6-b069-98c20d03dd81' " +
                "and segment ids '' not found."), errors.getErrors());
    }

    @Test
    public void invalidSegmentationsMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(404, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        null,
                                        new QuotaTransferInputDto.ExternalResourceId(
                                                "hdd", List.of(new QuotaTransferInputDto.Segment(
                                                "invalidSegmentationId",
                                                "invalidSegmentId"
                                        ))
                                        )
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Segmentation with provider id '96e779cf-7d3f-4e74-ba41-c2acc7f04235' " +
                "and key 'invalidSegmentationId' not found."), errors.getErrors());
    }

    @Test
    public void invalidSegmentsMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(404, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        null,
                                        new QuotaTransferInputDto.ExternalResourceId(
                                                "hdd", List.of(new QuotaTransferInputDto.Segment(
                                                "segment",
                                                "invalidSegmentId"
                                        ))
                                        )
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Segment with segmentation id '4654c7c8-cb87-4a73-8af4-0b8d4a92f16a' " +
                "and key 'invalidSegmentId' not found."), errors.getErrors());
    }

    @Test
    public void commentMustBeValidated() {
        ErrorCollectionDto errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                null
        ));

        assertEquals(Map.of("comment", Set.of("Field is required.")), errors.getFieldErrors());
    }

    @Test
    public void transferConstrainsMustBeChecked() {
        ErrorCollectionDto errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        ),
                        new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Duplicate folders pair: source id 'f714c483-c347-41cc-91d0-c6722f5daac7', " +
                "destination id '11d1bcdb-3edc-4c21-8a79-4570e3c09c21' and " +
                "resource id 'ef333da9-b076-42f5-b7f5-84cd04ab7fcc'."), errors.getErrors());

        errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        ),
                        new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_2_ID,
                                TestFolders.TEST_FOLDER_1_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));

        assertEquals(Set.of("Folder resource can't be source and destination in same request."), errors.getErrors());

        errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                -1L,
                                "gigabytes"
                        )
                ),
                "test"
        ));
        assertEquals(Set.of("Only positive transfer amount is allowed."), errors.getErrors());

        errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_2_ID,
                                TestFolders.TEST_FOLDER_1_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1000000L,
                                "gigabytes"
                        )
                ),
                "test"
        ));
        assertEquals(Set.of("Quota balance can't be negative after transfer. " +
                "Negative quota in folder id '11d1bcdb-3edc-4c21-8a79-4570e3c09c21'."), errors.getErrors());

        errors = expectErrors(422, new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        new QuotaTransferInputDto.ExternalResourceId("", List.of())
                                ),
                                10L,
                                "gigabytes"
                        )
                ),
                "test"
        ));
        assertEquals(Set.of("Either resource id or external resource id allowed."), errors.getErrors());
    }

    @Test
    public void bulkQuotaMoveShouldMoveQuota() {

        Map<String, QuotaModel> quotaByFolderIdBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));


        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(new QuotaTransferInputDto(
                        List.of(new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_2_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                )
                        ),
                        "test"
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        Map<String, QuotaModel> quotaByFolderIdAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));

        assertEquals(1_000_000_000L, quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_2_ID).getQuota() -
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_2_ID).getQuota()
        );

        assertEquals(1_000_000_000L, quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_1_ID).getQuota() -
                quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_1_ID).getQuota()
        );
    }

    @Test
    public void bulkQuotaMoveShouldMoveQuotaByExternalResource() {

        Map<String, QuotaModel> quotaByFolderIdBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));


        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(new QuotaTransferInputDto(
                        List.of(new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_2_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                null,
                                                new QuotaTransferInputDto.ExternalResourceId(
                                                        "hdd", List.of(
                                                        new QuotaTransferInputDto.Segment(
                                                                "segment",
                                                                "default"
                                                        ), new QuotaTransferInputDto.Segment(
                                                                "location",
                                                                "man"
                                                        ))
                                                )
                                        ),
                                        1L,
                                        "gigabytes"
                                )
                        ),
                        "test"
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        Map<String, QuotaModel> quotaByFolderIdAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));

        assertEquals(1_000_000_000L, quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_2_ID).getQuota() -
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_2_ID).getQuota()
        );

        assertEquals(1_000_000_000L, quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_1_ID).getQuota() -
                quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_1_ID).getQuota()
        );
    }

    @Test
    public void bulkQuotaMoveToSeveralTargetsShouldMoveQuota() {

        Map<String, QuotaModel> quotaByFolderIdBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_3_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));


        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(new QuotaTransferInputDto(
                        List.of(new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_2_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                ),
                                new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                )
                        ),
                        "test"
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        Map<String, QuotaModel> quotaByFolderIdAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_3_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));

        assertEquals(1_000_000_000L, quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_2_ID).getQuota() -
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_2_ID).getQuota()
        );

        assertEquals(1_000_000_000L, quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_3_ID).getQuota() -
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getQuota()
        );

        assertEquals(2_000_000_000L, quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_1_ID).getQuota() -
                quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_1_ID).getQuota()
        );
    }

    @Test
    public void bulkQuotaMoveFromSeveralSourcesShouldMoveQuota() {

        Map<String, QuotaModel> quotaByFolderIdBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_3_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));


        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(new QuotaTransferInputDto(
                        List.of(new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                ),
                                new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_2_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                )
                        ),
                        "test"
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        Map<String, QuotaModel> quotaByFolderIdAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_3_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));

        assertEquals(1_000_000_000L, quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_2_ID).getQuota() -
                quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_2_ID).getQuota()
        );

        assertEquals(2_000_000_000L, quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_3_ID).getQuota() -
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getQuota()
        );

        assertEquals(1_000_000_000L, quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_1_ID).getQuota() -
                quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_1_ID).getQuota()
        );
    }

    @Test
    public void bulkQuotaMoveShouldLogOperations() {

        Map<String, FolderModel> folderByIdBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                TestFolders.TEST_FOLDER_3_ID
                        ), Tenants.DEFAULT_TENANT_ID
                )).block().stream().collect(Collectors.toMap(FolderModel::getId, Function.identity()));

        Map<String, QuotaModel> quotaByFolderIdBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_3_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));


        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(new QuotaTransferInputDto(
                        List.of(new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                ),
                                new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_2_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                )
                        ),
                        "test"
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        Map<String, FolderModel> folderByIdAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                folderDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        List.of(TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                TestFolders.TEST_FOLDER_3_ID
                        ), Tenants.DEFAULT_TENANT_ID
                )).block().stream().collect(Collectors.toMap(FolderModel::getId, Function.identity()));

        assertEquals(1L, folderByIdAfter.get(TestFolders.TEST_FOLDER_1_ID).getNextOpLogOrder() -
                folderByIdBefore.get(TestFolders.TEST_FOLDER_1_ID).getNextOpLogOrder()
        );

        assertEquals(1L, folderByIdAfter.get(TestFolders.TEST_FOLDER_2_ID).getNextOpLogOrder() -
                folderByIdBefore.get(TestFolders.TEST_FOLDER_2_ID).getNextOpLogOrder()
        );

        assertEquals(2L, folderByIdAfter.get(TestFolders.TEST_FOLDER_3_ID).getNextOpLogOrder() -
                folderByIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getNextOpLogOrder()
        );
        Map<Long, FolderOperationLogModel> folder3LogsByOrder = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogDao.getFirstPageByFolder(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        Tenants.DEFAULT_TENANT_ID,
                        TestFolders.TEST_FOLDER_3_ID,
                        SortOrderDto.ASC,
                        100
                )).block().stream().collect(Collectors.toMap(FolderOperationLogModel::getOrder, Function.identity()));

        FolderOperationLogModel firstLog = folder3LogsByOrder.get(0L);
        FolderOperationLogModel secondLog = folder3LogsByOrder.get(1L);

        assertEquals(firstLog.getOldQuotas().asMap().get(YP_HDD_MAN),
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getQuota());
        assertEquals(firstLog.getOldBalance().asMap().get(YP_HDD_MAN),
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getBalance());

        assertEquals(firstLog.getNewQuotas().asMap().get(YP_HDD_MAN),
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getQuota() + 1_000_000_000L);
        assertEquals(firstLog.getNewBalance().asMap().get(YP_HDD_MAN),
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getBalance() + 1_000_000_000L);

        assertEquals(firstLog.getNewQuotas(), secondLog.getOldQuotas());
        assertEquals(firstLog.getNewBalance(), secondLog.getOldBalance());

        assertEquals(secondLog.getNewQuotas().asMap().get(YP_HDD_MAN),
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getQuota() + 2_000_000_000L);
        assertEquals(secondLog.getNewBalance().asMap().get(YP_HDD_MAN),
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getBalance() + 2_000_000_000L);
    }

    @Test
    public void bulkQuotaMoveShouldStoreComment() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(new QuotaTransferInputDto(
                        List.of(new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                )
                        ),
                        "custom comment"
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        String commentId = ydbTableClient.usingSessionMonoRetryable(session ->
                        folderOperationLogDao.getFirstPageByFolder(
                                session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                Tenants.DEFAULT_TENANT_ID,
                                TestFolders.TEST_FOLDER_3_ID,
                                SortOrderDto.ASC,
                                100
                        )).block().stream()
                .sorted(Comparator.comparing(FolderOperationLogModel::getOrder).reversed())
                .findFirst()
                .flatMap(FolderOperationLogModel::getCommentId)
                .get();

        assertNotNull(commentId);


        FolderOperationLogCommentModel commentModel = ydbTableClient.usingSessionMonoRetryable(session ->
                folderOperationLogCommentDao.getById(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        commentId,
                        Tenants.DEFAULT_TENANT_ID
                )).block().get();

        assertEquals("custom comment", commentModel.getText());
    }

    @Test
    public void bulkQuotaMoveToClosingService() {
        QuotaTransferInputDto body = new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        TestResources.YP_HDD_SAS,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        ),
                        new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )),
                "test"
        );

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Set<String> errors = errorCollection.getErrors();
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Current service status is not allowed."));
                });
    }

    @Test
    public void bulkQuotaMoveFromClosingService() {
        QuotaTransferInputDto body = new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        ),
                        new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        TestResources.YP_HDD_SAS,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )),
                "test"
        );

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    public void bulkQuotaMoveToNonExportableService() {
        QuotaTransferInputDto body = new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        TestResources.YP_HDD_SAS,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        ),
                        new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )),
                "test"
        );

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Set<String> errors = errorCollection.getErrors();
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Services in the sandbox are not allowed."));
                });
    }

    @Test
    public void bulkQuotaMoveFromNonExportableService() {
        QuotaTransferInputDto body = new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        ),
                        new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        TestResources.YP_HDD_SAS,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )),
                "test"
        );

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    public void bulkQuotaMoveToRenamingService() {
        QuotaTransferInputDto body = new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        TestResources.YP_HDD_SAS,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        ),
                        new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )),
                "test"
        );

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    public void bulkQuotaMoveFromRenamingService() {
        QuotaTransferInputDto body = new QuotaTransferInputDto(
                List.of(new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_1_ID,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        YP_HDD_MAN,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        ),
                        new QuotaTransferInputDto.Transfer(
                                TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE,
                                TestFolders.TEST_FOLDER_2_ID,
                                new QuotaTransferInputDto.Resource(
                                        TestProviders.YP_ID,
                                        TestResources.YP_HDD_SAS,
                                        null
                                ),
                                1L,
                                "gigabytes"
                        )),
                "test"
        );

        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    public void bulkQuotaMoveShouldMoveQuotaByExternalAndInternalResourcesTest() {
        Map<String, QuotaModel> quotaByFolder1Id = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));

        QuotaModel quotaModelFromFolder1 = quotaByFolder1Id.get(TestFolders.TEST_FOLDER_1_ID);

        ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.upsertOneRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        QuotaModel.builder(quotaModelFromFolder1)
                                .quota(quotaModelFromFolder1.getQuota() + 1_000_000_000_000L)
                                .balance(quotaModelFromFolder1.getBalance() + 1_000_000_000_000L)
                                .build()
                )).block();

        Map<String, QuotaModel> quotaByFolderIdBefore = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_3_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));


        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(new QuotaTransferInputDto(
                        List.of(new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_2_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                null,
                                                new QuotaTransferInputDto.ExternalResourceId(
                                                        "hdd", List.of(
                                                        new QuotaTransferInputDto.Segment(
                                                                "segment",
                                                                "default"
                                                        ), new QuotaTransferInputDto.Segment(
                                                                "location",
                                                                "man"
                                                        ))
                                                )
                                        ),
                                        1L,
                                        "gigabytes"
                                ),
                                new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "terabytes"
                                )

                        ),
                        "test"
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        Map<String, QuotaModel> quotaByFolderIdAfter = ydbTableClient.usingSessionMonoRetryable(session ->
                        quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_1_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_2_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        )),
                                        new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                TestFolders.TEST_FOLDER_3_ID,
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN
                                        ))
                                ))
                ).block().stream()
                .collect(Collectors.toMap(QuotaModel::getFolderId, Function.identity()));

        assertEquals(1_000_000_000L + 1_000_000_000_000L,
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_1_ID).getQuota() -
                        quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_1_ID).getQuota()
        );

        assertEquals(1_000_000_000L, quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_2_ID).getQuota() -
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_2_ID).getQuota()
        );

        assertEquals(1_000_000_000_000L, quotaByFolderIdAfter.get(TestFolders.TEST_FOLDER_3_ID).getQuota() -
                quotaByFolderIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getQuota()
        );
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void bulkQuotaMoveShouldLogOperationsInOneRecordTest() {
        ydbTableClient.usingSessionMonoRetryable(session ->
                quotasDao.upsertAllRetryable(session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        List.of(QuotaModel.builder()
                                .tenantId(Tenants.DEFAULT_TENANT_ID)
                                .folderId(TestFolders.TEST_FOLDER_1_ID)
                                .providerId(TestProviders.YP_ID)
                                .resourceId(YP_CPU_MAN)
                                .quota(1_000L)
                                .balance(1_000L)
                                .frozenQuota(0L)
                                .build(),
                        QuotaModel.builder()
                                .tenantId(Tenants.DEFAULT_TENANT_ID)
                                .folderId(TestFolders.TEST_FOLDER_2_ID)
                                .providerId(TestProviders.YP_ID)
                                .resourceId(YP_CPU_MAN)
                                .quota(1_000L)
                                .balance(1_000L)
                                .frozenQuota(0L)
                                .build()
                ))).block();

        Map<String, FolderModel> folderByIdBefore = Objects.requireNonNull(
                ydbTableClient.usingSessionMonoRetryable(session ->
                        folderDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_2_ID,
                                        TestFolders.TEST_FOLDER_3_ID
                                ), Tenants.DEFAULT_TENANT_ID
                        )).block()).stream().collect(Collectors.toMap(FolderModel::getId, Function.identity()));

        Map<String, Map<String, QuotaModel>> quotaByFolderIdByResourceBefore = Objects.requireNonNull(
                        ydbTableClient.usingSessionMonoRetryable(session ->
                                quotasDao.getByKeys(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                        List.of(new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                        TestFolders.TEST_FOLDER_1_ID,
                                                        TestProviders.YP_ID,
                                                        YP_HDD_MAN
                                                )),
                                                new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                        TestFolders.TEST_FOLDER_2_ID,
                                                        TestProviders.YP_ID,
                                                        YP_HDD_MAN
                                                )),
                                                new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                        TestFolders.TEST_FOLDER_3_ID,
                                                        TestProviders.YP_ID,
                                                        YP_HDD_MAN
                                                )),
                                                new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                        TestFolders.TEST_FOLDER_1_ID,
                                                        TestProviders.YP_ID,
                                                        YP_CPU_MAN
                                                )),
                                                new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                        TestFolders.TEST_FOLDER_2_ID,
                                                        TestProviders.YP_ID,
                                                        YP_CPU_MAN
                                                )),
                                                new WithTenant<>(Tenants.DEFAULT_TENANT_ID, new QuotaModel.Key(
                                                        TestFolders.TEST_FOLDER_3_ID,
                                                        TestProviders.YP_ID,
                                                        YP_CPU_MAN
                                                ))
                                        ))
                        ).block()).stream()
                .collect(Collectors.groupingBy(QuotaModel::getFolderId, Collectors.toMap(QuotaModel::getResourceId,
                        Function.identity())));


        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/api/v1/bulkQuotaMove")
                .bodyValue(new QuotaTransferInputDto(
                        List.of(new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                ),
                                new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_2_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_HDD_MAN,
                                                null
                                        ),
                                        1L,
                                        "gigabytes"
                                ),
                                new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_CPU_MAN,
                                                null
                                        ),
                                        1L,
                                        "cores"
                                ),
                                new QuotaTransferInputDto.Transfer(
                                        TestFolders.TEST_FOLDER_2_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        new QuotaTransferInputDto.Resource(
                                                TestProviders.YP_ID,
                                                YP_CPU_MAN,
                                                null
                                        ),
                                        1L,
                                        "cores"
                                )
                        ),
                        "test"
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();

        Map<String, FolderModel> folderByIdAfter = Objects.requireNonNull(
                ydbTableClient.usingSessionMonoRetryable(session ->
                        folderDao.getByIds(session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                List.of(TestFolders.TEST_FOLDER_1_ID,
                                        TestFolders.TEST_FOLDER_2_ID,
                                        TestFolders.TEST_FOLDER_3_ID
                                ), Tenants.DEFAULT_TENANT_ID
                        )).block()).stream().collect(Collectors.toMap(FolderModel::getId, Function.identity()));

        assertEquals(1L, folderByIdAfter.get(TestFolders.TEST_FOLDER_1_ID).getNextOpLogOrder() -
                folderByIdBefore.get(TestFolders.TEST_FOLDER_1_ID).getNextOpLogOrder()
        );

        assertEquals(1L, folderByIdAfter.get(TestFolders.TEST_FOLDER_2_ID).getNextOpLogOrder() -
                folderByIdBefore.get(TestFolders.TEST_FOLDER_2_ID).getNextOpLogOrder()
        );

        assertEquals(2L, folderByIdAfter.get(TestFolders.TEST_FOLDER_3_ID).getNextOpLogOrder() -
                folderByIdBefore.get(TestFolders.TEST_FOLDER_3_ID).getNextOpLogOrder()
        );

        Map<Long, FolderOperationLogModel> folder1LogsByOrder = Objects.requireNonNull(
                        ydbTableClient.usingSessionMonoRetryable(session ->
                                folderOperationLogDao.getFirstPageByFolder(
                                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                        Tenants.DEFAULT_TENANT_ID,
                                        TestFolders.TEST_FOLDER_1_ID,
                                        SortOrderDto.ASC,
                                        100
                                )).block()).stream()
                .collect(Collectors.toMap(FolderOperationLogModel::getOrder, Function.identity()));

        FolderOperationLogModel firstLogFolder1 = folder1LogsByOrder.get(0L);

        Map<Long, FolderOperationLogModel> folder2LogsByOrder = Objects.requireNonNull(
                        ydbTableClient.usingSessionMonoRetryable(session ->
                                folderOperationLogDao.getFirstPageByFolder(
                                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                        Tenants.DEFAULT_TENANT_ID,
                                        TestFolders.TEST_FOLDER_2_ID,
                                        SortOrderDto.ASC,
                                        100
                                )).block()).stream()
                .collect(Collectors.toMap(FolderOperationLogModel::getOrder, Function.identity()));

        FolderOperationLogModel firstLogFolder2 = folder2LogsByOrder.get(0L);

        Map<Long, FolderOperationLogModel> folder3LogsByOrder = Objects.requireNonNull(
                        ydbTableClient.usingSessionMonoRetryable(session ->
                                folderOperationLogDao.getFirstPageByFolder(
                                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                                        Tenants.DEFAULT_TENANT_ID,
                                        TestFolders.TEST_FOLDER_3_ID,
                                        SortOrderDto.ASC,
                                        100
                                )).block()).stream()
                .collect(Collectors.toMap(FolderOperationLogModel::getOrder, Function.identity()));

        FolderOperationLogModel firstLog = folder3LogsByOrder.get(0L);
        FolderOperationLogModel secondLog = folder3LogsByOrder.get(1L);

        Set<Tuple2<String, String>> expectedLogs = Stream.of(firstLogFolder1, firstLogFolder2)
                .map(log -> Tuples.of(log.getSourceFolderOperationsLogId().orElseThrow(),
                        log.getDestinationFolderOperationsLogId().orElseThrow()))
                .collect(Collectors.toSet());

        assertEquals(2, expectedLogs.size());
        assertEquals(expectedLogs, Stream.of(firstLog, secondLog)
                .map(log -> Tuples.of(log.getSourceFolderOperationsLogId().orElseThrow(),
                        log.getDestinationFolderOperationsLogId().orElseThrow()))
                .collect(Collectors.toSet()));

        assertEquals(firstLog.getOldQuotas().asMap().get(YP_HDD_MAN),
                quotaByFolderIdByResourceBefore.get(TestFolders.TEST_FOLDER_3_ID).get(YP_HDD_MAN).getQuota());
        assertEquals(firstLog.getOldBalance().asMap().get(YP_HDD_MAN),
                quotaByFolderIdByResourceBefore.get(TestFolders.TEST_FOLDER_3_ID).get(YP_HDD_MAN).getBalance());
        assertNull(quotaByFolderIdByResourceBefore.get(TestFolders.TEST_FOLDER_3_ID).get(YP_CPU_MAN));
        assertNull(quotaByFolderIdByResourceBefore.get(TestFolders.TEST_FOLDER_3_ID).get(YP_CPU_MAN));

        assertEquals(firstLog.getNewQuotas().asMap().get(YP_HDD_MAN), quotaByFolderIdByResourceBefore
                .get(TestFolders.TEST_FOLDER_3_ID).get(YP_HDD_MAN).getQuota() + 1_000_000_000L);
        assertEquals(firstLog.getNewBalance().asMap().get(YP_HDD_MAN), quotaByFolderIdByResourceBefore
                .get(TestFolders.TEST_FOLDER_3_ID).get(YP_HDD_MAN).getBalance() + 1_000_000_000L);
        assertEquals(firstLog.getNewQuotas().asMap().get(YP_CPU_MAN), 1_000L);
        assertEquals(firstLog.getNewBalance().asMap().get(YP_CPU_MAN), 1_000L);

        assertEquals(firstLog.getNewQuotas(), secondLog.getOldQuotas());
        assertEquals(firstLog.getNewBalance(), secondLog.getOldBalance());

        assertEquals(secondLog.getNewQuotas().asMap().get(YP_HDD_MAN), quotaByFolderIdByResourceBefore
                .get(TestFolders.TEST_FOLDER_3_ID).get(YP_HDD_MAN).getQuota() + 2_000_000_000L);
        assertEquals(secondLog.getNewBalance().asMap().get(YP_HDD_MAN), quotaByFolderIdByResourceBefore
                .get(TestFolders.TEST_FOLDER_3_ID).get(YP_HDD_MAN).getBalance() + 2_000_000_000L);
        assertEquals(secondLog.getNewQuotas().asMap().get(YP_CPU_MAN), 2_000L);
        assertEquals(secondLog.getNewBalance().asMap().get(YP_CPU_MAN), 2_000L);
    }

}
