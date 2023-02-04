package ru.yandex.intranet.d.grpc.transfers

import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID
import ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_6_ID
import ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID
import ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2_ID
import ru.yandex.intranet.d.TestProviders.YP_ID
import ru.yandex.intranet.d.TestResources.YP_HDD_MAN
import ru.yandex.intranet.d.backend.service.proto.*
import ru.yandex.intranet.d.backend.service.proto.TransfersServiceGrpc.TransfersServiceBlockingStub
import ru.yandex.intranet.d.grpc.MockGrpcUser

/**
 * ProvisionTransferTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 11-02-2022
 */
@IntegrationTest
class ProvisionTransferTest {
    @GrpcClient("inProcess")
    private lateinit var transfersService: TransfersServiceBlockingStub

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun createTransferTest() {
        val result = transfersService
            .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
            .createTransfer(
                CreateTransferRequest.newBuilder()
                    .setType(TransferType.PROVISION_TRANSFER)
                    .setAddConfirmation(false)
                    .setParameters(
                        TransferParameters.newBuilder()
                            .setProvisionTransfer(ProvisionTransferParameters.newBuilder()
                                .addProvisionTransfer(ProvisionTransfer.newBuilder()
                                    .setSourceFolderId(TEST_FOLDER_1_ID)
                                    .setSourceAccountId(TEST_ACCOUNT_1_ID)
                                    .setDestinationFolderId(TEST_FOLDER_2_ID)
                                    .setDestinationAccountId(TEST_ACCOUNT_6_ID)
                                    .addSourceAccountTransfers(ResourceTransfer.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setDelta(TransferAmount.newBuilder()
                                            .setValue(-1L)
                                            .setUnitKey("gigabytes")
                                        )
                                    )
                                    .addDestinationAccountTransfers(ResourceTransfer.newBuilder()
                                        .setProviderId(YP_ID)
                                        .setResourceId(YP_HDD_MAN)
                                        .setDelta(TransferAmount.newBuilder()
                                            .setValue(1L)
                                            .setUnitKey("gigabytes")
                                        )
                                    )
                                )
                            )
                    )
                    .build()
            )

        assertNotNull(result)
        val getResult = transfersService
            .withCallCredentials(MockGrpcUser.uid("1120000000000010"))
            .getTransfer(
                GetTransferRequest.newBuilder()
                    .setTransferId(result.transferId)
                    .build()
            )
        assertNotNull(getResult)
        assertEquals(TransferType.PROVISION_TRANSFER, getResult.type)
        assertEquals(TransferSubtype.DEFAULT_PROVISION_TRANSFER, getResult.transferSubtype)
        assertEquals(TransferStatus.PENDING, getResult.status)
        assertEquals("1120000000000010", getResult.createdBy.uid)
        assertTrue(getResult.votes.votersList.isEmpty())
        assertEquals(1, getResult.responsible.groupedList.size)
        assertTrue(getResult.responsible.groupedList.any { it.folderIdsList.contains(TEST_FOLDER_1_ID) })
        assertTrue(getResult.responsible.groupedList.any { it.folderIdsList.contains(TEST_FOLDER_2_ID) })
        val provisionTransfer = getResult.parameters.provisionTransfer.provisionTransferList[0]
        assertEquals(TEST_FOLDER_1_ID, provisionTransfer.sourceFolderId)
        assertEquals(TEST_ACCOUNT_1_ID, provisionTransfer.sourceAccountId)
        assertEquals(TEST_FOLDER_2_ID, provisionTransfer.destinationFolderId)
        assertEquals(TEST_ACCOUNT_6_ID, provisionTransfer.destinationAccountId)
        val sourceAccountTransfer = provisionTransfer.sourceAccountTransfersList[0]
        assertEquals(YP_HDD_MAN, sourceAccountTransfer.resourceId)
        assertEquals(-1000000L, sourceAccountTransfer.delta.value)
        assertEquals("kilobytes", sourceAccountTransfer.delta.unitKey)
        val destinationAccountTransfer = provisionTransfer.destinationAccountTransfersList[0]
        assertEquals(YP_HDD_MAN, destinationAccountTransfer.resourceId)
        assertEquals(1000000L, destinationAccountTransfer.delta.value)
        assertEquals("kilobytes", destinationAccountTransfer.delta.unitKey)

    }
}
