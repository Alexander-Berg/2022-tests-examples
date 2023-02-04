package ru.yandex.intranet.d.services.transfer;

import java.util.List;

import io.jsonwebtoken.lang.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestServices;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.UnitIds;
import ru.yandex.intranet.d.services.tracker.TrackerClient;
import ru.yandex.intranet.d.services.tracker.TrackerIssueResolution;
import ru.yandex.intranet.d.util.result.ErrorCollection;
import ru.yandex.intranet.d.util.result.Result;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.tracker.TrackerCreateTicketDto;
import ru.yandex.intranet.d.web.model.tracker.TrackerCreateTicketResponseDto;
import ru.yandex.intranet.d.web.model.tracker.TrackerTransitionExecuteDto;
import ru.yandex.intranet.d.web.model.tracker.TrackerUpdateTicketDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestVoteTypeDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateReserveTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontPutTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontTransferRequestVotingDto;

import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests for tracker service
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
public class TransferRequestTicketTest {

    @Autowired
    private WebTestClient webClient;
    @MockBean
    private TrackerClient trackerClient;

    @Value("${tracker.transfer.queue}")
    private String trackerQueue;
    @Value("${tracker.transfer.type.transfer}")
    private Long transferComponentId;
    @Value("${tracker.transfer.type.reserve}")
    private Long transferReserveComponentId;

    @Test
    public void transferRequestCreateTest() {
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto("DISPENSERTREQ-1"))));

        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .description("")
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(result);
        Assert.notNull(result.getTransfer());

        TrackerCreateTicketDto createTicketDto = new TrackerCreateTicketDto(trackerQueue,
                "Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка",
                "Сервис-источник: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Сервис-получатель: https://abc.test.yandex-team.ru/services/d\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" + result.getTransfer().getId() +
                        " Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-MAN: 1 GB",
                "login-2",
                List.of(1L, transferComponentId),
                List.of("dispenser", "d"),
                trackerQueue + "_" + result.getTransfer().getId());
        Mockito.verify(trackerClient).createTicket(createTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void transferRequestCreateTestWithRounding() {
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto("DISPENSERTREQ-1"))));

        FrontCreateTransferRequestDto firstBody = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .description("")
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("12345")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-12345")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontCreateTransferRequestDto secondBody = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .description("")
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("100")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-100")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(firstBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(firstResult);
        Assert.notNull(firstResult.getTransfer());

        TrackerCreateTicketDto firstCreateTicketDto = new TrackerCreateTicketDto(trackerQueue,
                "Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка",
                "Сервис-источник: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Сервис-получатель: https://abc.test.yandex-team.ru/services/d\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" + firstResult.getTransfer().getId() +
                        " Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-MAN: 12.35 TB",
                "login-2",
                List.of(1L, transferComponentId),
                List.of("dispenser", "d"),
                trackerQueue + "_" + firstResult.getTransfer().getId());
        Mockito.verify(trackerClient).createTicket(firstCreateTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());

        FrontSingleTransferRequestDto secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(secondBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(secondResult);
        Assert.notNull(secondResult.getTransfer());

        TrackerCreateTicketDto secondCreateTicketDto = new TrackerCreateTicketDto(trackerQueue,
                "Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка",
                "Сервис-источник: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Сервис-получатель: https://abc.test.yandex-team.ru/services/d\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" + secondResult.getTransfer().getId() +
                        " Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-MAN: 100 GB",
                "login-2",
                List.of(1L, transferComponentId),
                List.of("dispenser", "d"),
                trackerQueue + "_" + secondResult.getTransfer().getId());
        Mockito.verify(trackerClient).createTicket(secondCreateTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());

    }

    @Test
    public void transferRequestCreateTestWithDescription() {
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto("DISPENSERTREQ-1"))));
        String description = "Описание заявки\nТекст";

        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .description(description)
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(result);
        Assert.notNull(result.getTransfer());

        TrackerCreateTicketDto createTicketDto = new TrackerCreateTicketDto(trackerQueue,
                "Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка",
                "Сервис-источник: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Сервис-получатель: https://abc.test.yandex-team.ru/services/d\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" + result.getTransfer().getId() +
                        " Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-MAN: 1 GB\n\n" +
                        "Комментарий:\n" +
                        description,
                "login-2",
                List.of(1L, transferComponentId),
                List.of("dispenser", "d"),
                trackerQueue + "_" + result.getTransfer().getId());
        Mockito.verify(trackerClient).createTicket(createTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void transferRequestCreateAndAppliedTest() {
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto("DISPENSERTREQ-1"))));
        Mockito.when(trackerClient.closeTicket(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(Result.success(null)));

        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_CLOSING))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(result);
        Assert.notNull(result.getTransfer());

        TrackerCreateTicketDto createTicketDto = new TrackerCreateTicketDto(trackerQueue,
                "Перенос квоты из test-closing:Folder in closing service в dispenser:Проверочная папка",
                "Сервис-источник: https://abc.test.yandex-team.ru/services/test-closing\n" +
                        "Подтверждающие: staff:login-1\n" +
                        "Сервис-получатель: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие: staff:login-1\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" + result.getTransfer().getId() +
                        " Перенос квоты из test-closing:Folder in closing service в dispenser:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-SAS: 1 GB",
                "login-1",
                List.of(1L, transferComponentId),
                List.of("dispenser", "test-closing"),
                trackerQueue + "_" + result.getTransfer().getId());
        Mockito.verify(trackerClient).createTicket(createTicketDto);
        Mockito.verify(trackerClient).closeTicket(trackerQueue + "-1", "close",
                new TrackerTransitionExecuteDto(TrackerIssueResolution.FIXED.toString()));
    }

    @Test
    public void transferRequestCreateTestWithMonoError() {
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.error(new RuntimeException("Test error")));
        Mockito.when(trackerClient.closeTicket(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(Result.success(null)));

        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_CLOSING))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(result);
        Assert.notNull(result.getTransfer());

        TrackerCreateTicketDto createTicketDto = new TrackerCreateTicketDto(trackerQueue,
                "Перенос квоты из test-closing:Folder in closing service в dispenser:Проверочная папка",
                "Сервис-источник: https://abc.test.yandex-team.ru/services/test-closing\n" +
                        "Подтверждающие: staff:login-1\n" +
                        "Сервис-получатель: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие: staff:login-1\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" + result.getTransfer().getId() +
                        " Перенос квоты из test-closing:Folder in closing service в dispenser:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-SAS: 1 GB",
                "login-1",
                List.of(1L, transferComponentId),
                List.of("dispenser", "test-closing"),
                trackerQueue + "_" + result.getTransfer().getId());
        Mockito.verify(trackerClient).createTicket(createTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void transferRequestCreateTestWithResultFailure() {
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.failure(ErrorCollection.builder().build())));
        Mockito.when(trackerClient.closeTicket(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(Result.success(null)));

        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(true)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_CLOSING))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(result);
        Assert.notNull(result.getTransfer());

        TrackerCreateTicketDto createTicketDto = new TrackerCreateTicketDto(trackerQueue,
                "Перенос квоты из test-closing:Folder in closing service в dispenser:Проверочная папка",
                "Сервис-источник: https://abc.test.yandex-team.ru/services/test-closing\n" +
                        "Подтверждающие: staff:login-1\n" +
                        "Сервис-получатель: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие: staff:login-1\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" + result.getTransfer().getId() +
                        " Перенос квоты из test-closing:Folder in closing service в dispenser:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-SAS: 1 GB",
                "login-1",
                List.of(1L, transferComponentId),
                List.of("dispenser", "test-closing"),
                trackerQueue + "_" + result.getTransfer().getId());
        Mockito.verify(trackerClient).createTicket(createTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void transferRequestUpdateTest() {
        String ticketKet = "DISPENSERTREQ-1";
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto(ticketKet))));
        Mockito.when(trackerClient.updateTicket(eq(ticketKet), Mockito.any()))
                .thenReturn(Mono.just(Result.failure(ErrorCollection.builder().build())));

        FrontCreateTransferRequestDto createBody = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(createResult);
        Assert.notNull(createResult.getTransfer());

        String description = "Test description";
        FrontPutTransferRequestDto updateBody = FrontPutTransferRequestDto.builder()
                .addConfirmation(false)
                .description(description)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .put()
                .uri("/front/transfers/{id}?version={version}", createResult.getTransfer().getId(),
                        createResult.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(createResult);
        Assert.notNull(createResult.getTransfer());

        TrackerUpdateTicketDto updateTicketDto = new TrackerUpdateTicketDto(
                "Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка",
                "Сервис-источник: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Сервис-получатель: https://abc.test.yandex-team.ru/services/d\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" +
                        updateResult.getTransfer().getId() +
                        " Перенос квоты из dispenser:Проверочная папка в d:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-MAN: 1 GB\n\n" +
                        "Комментарий:\n" +
                        description,
                List.of(1L, transferComponentId),
                List.of("dispenser", "d"));
        Mockito.verify(trackerClient).updateTicket(ticketKet, updateTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void transferRequestAppliedTest() {
        String ticketKet = "DISPENSERTREQ-1";
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto(ticketKet))));
        Mockito.when(trackerClient.closeTicket(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(Result.success(null)));

        FrontCreateTransferRequestDto createBody = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(createResult);
        Assert.notNull(createResult.getTransfer());

        FrontSingleTransferRequestDto voteResult = webClient
                .mutateWith(MockUser.uid("1120000000000010"))
                .post()
                .uri("/front/transfers/{id}/_vote?version={version}",
                        createResult.getTransfer().getId(), createResult.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(FrontTransferRequestVotingDto.builder()
                        .voteType(TransferRequestVoteTypeDto.CONFIRM)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(voteResult);
        Assert.notNull(voteResult.getTransfer());

        Mockito.verify(trackerClient).closeTicket(ticketKet, "close",
                new TrackerTransitionExecuteDto(TrackerIssueResolution.FIXED.toString()));
    }

    @Test
    public void transferRequestCancelTest() {
        String ticketKet = "DISPENSERTREQ-1";
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto(ticketKet))));
        Mockito.when(trackerClient.closeTicket(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(Result.success(null)));

        FrontCreateTransferRequestDto createBody = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(createResult);
        Assert.notNull(createResult.getTransfer());

        FrontSingleTransferRequestDto cancelResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers/{id}/_cancel?version={version}", createResult.getTransfer().getId(),
                        createResult.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(cancelResult);
        Assert.notNull(cancelResult.getTransfer());

        Mockito.verify(trackerClient).closeTicket(ticketKet, "close",
                new TrackerTransitionExecuteDto(TrackerIssueResolution.WONT_FIX.toString()));
    }

    @Test
    public void transferRequestReserveCreateTest() {
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto("DISPENSERTREQ-1"))));

        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                .description("")
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                .providerId(TestProviders.YP_ID)
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(result);
        Assert.notNull(result.getTransfer());

        TrackerCreateTicketDto createTicketDto = new TrackerCreateTicketDto(trackerQueue,
                "Выдача квоты из резерва YP в d:Проверочная папка",
                "Сервис-получатель: https://abc.test.yandex-team.ru/services/d\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Провайдер-источник: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие от провайдера: staff:login-10\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" + result.getTransfer().getId() +
                        " Выдача квоты из резерва YP в d:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-SAS: 1 GB\n",
                "login-2",
                List.of(1L, transferReserveComponentId),
                List.of("dispenser", "d"),
                trackerQueue + "_" + result.getTransfer().getId());
        Mockito.verify(trackerClient).createTicket(createTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void transferRequestReserveCreateTestWithDescription() {
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto("DISPENSERTREQ-1"))));
        String description = "Описание заявки\nТекст";

        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                .description(description)
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                .providerId(TestProviders.YP_ID)
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(result);
        Assert.notNull(result.getTransfer());

        TrackerCreateTicketDto createTicketDto = new TrackerCreateTicketDto(trackerQueue,
                "Выдача квоты из резерва YP в d:Проверочная папка",
                "Сервис-получатель: https://abc.test.yandex-team.ru/services/d\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Провайдер-источник: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие от провайдера: staff:login-10\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" + result.getTransfer().getId() +
                        " Выдача квоты из резерва YP в d:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-SAS: 1 GB\n\n" +
                        "Комментарий:\n" +
                        description,
                "login-2",
                List.of(1L, transferReserveComponentId),
                List.of("dispenser", "d"),
                trackerQueue + "_" + result.getTransfer().getId());
        Mockito.verify(trackerClient).createTicket(createTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void transferRequestReserveUpdateTest() {
        String ticketKet = "DISPENSERTREQ-1";
        Mockito.when(trackerClient.createTicket(Mockito.any()))
                .thenReturn(Mono.just(Result.success(new TrackerCreateTicketResponseDto(ticketKet))));
        Mockito.when(trackerClient.updateTicket(eq(ticketKet), Mockito.any()))
                .thenReturn(Mono.just(Result.failure(ErrorCollection.builder().build())));

        FrontCreateTransferRequestDto createBody = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.RESERVE_TRANSFER)
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                .providerId(TestProviders.YP_ID)
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto createResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(createResult);
        Assert.notNull(createResult.getTransfer());

        String description = "Test description";
        FrontPutTransferRequestDto updateBody = FrontPutTransferRequestDto.builder()
                .addConfirmation(false)
                .description(description)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .reserveTransfer(FrontCreateReserveTransferDto.builder()
                                .providerId(TestProviders.YP_ID)
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("2")
                                        .resourceId(TestResources.YP_HDD_SAS)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto updateResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .put()
                .uri("/front/transfers/{id}?version={version}", createResult.getTransfer().getId(),
                        createResult.getTransfer().getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(updateBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(createResult);
        Assert.notNull(createResult.getTransfer());

        TrackerUpdateTicketDto updateTicketDto = new TrackerUpdateTicketDto(
                "Выдача квоты из резерва YP в d:Проверочная папка",
                "Сервис-получатель: https://abc.test.yandex-team.ru/services/d\n" +
                        "Подтверждающие: staff:login-10, staff:login-12\n" +
                        "Провайдер-источник: https://abc.test.yandex-team.ru/services/dispenser\n" +
                        "Подтверждающие от провайдера: staff:login-10\n" +
                        "Заявка в ABCD: " +
                        "((https://abc.test.yandex-team.ru/folders/transfers/" +
                        updateResult.getTransfer().getId() +
                        " Выдача квоты из резерва YP в d:Проверочная папка))\n\n" +
                        "**Провайдер: YP**\n" +
                        "YP-HDD-SAS: 2 GB\n\n" +
                        "Комментарий:\n" +
                        description,
                List.of(1L, transferReserveComponentId),
                List.of("dispenser", "d"));
        Mockito.verify(trackerClient).updateTicket(ticketKet, updateTicketDto);
        Mockito.verify(trackerClient, Mockito.atMost(0))
                .closeTicket(Mockito.any(), Mockito.any(), Mockito.any());
    }

}
