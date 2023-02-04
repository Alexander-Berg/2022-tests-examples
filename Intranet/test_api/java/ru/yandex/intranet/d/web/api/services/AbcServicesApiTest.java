package ru.yandex.intranet.d.web.api.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.services.CheckServicesRequestDto;
import ru.yandex.intranet.d.web.model.services.ClosingServiceDto;
import ru.yandex.intranet.d.web.model.services.ValueByServiceIdDto;

/**
 * ABC Services API tests
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
public class AbcServicesApiTest {

    @Autowired
    private WebTestClient webClient;
    @Value("${abc.tvmSourceId}")
    private long abcTvmSourceId;

    @Test
    public void checkClosingServicesTest() {
        ClosingServiceDto result = webClient
                .mutateWith(MockUser.tvm(abcTvmSourceId))
                .post()
                .uri("/abc/_checkClosing", 1)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(List.of(1L, 13L, 14L)))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClosingServiceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getClosing());
        Assertions.assertEquals(3, result.getClosing().size());
        Assertions.assertTrue(result.getClosing().contains(new ValueByServiceIdDto(1L, false)));
        Assertions.assertTrue(result.getClosing().contains(new ValueByServiceIdDto(13L, false)));
        Assertions.assertTrue(result.getClosing().contains(new ValueByServiceIdDto(14L, true)));
    }

    @Test
    public void checkClosingServicesNotValidStateTest() {
        ClosingServiceDto result = webClient
                .mutateWith(MockUser.tvm(abcTvmSourceId))
                .post()
                .uri("/abc/_checkClosing")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(List.of(22L)))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClosingServiceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getClosing());
        Assertions.assertEquals(1, result.getClosing().size());
        Assertions.assertTrue(result.getClosing().contains(new ValueByServiceIdDto(22L, false)));
    }

    @Test
    public void checkClosingServicesIgnoreStateTest() {
        ClosingServiceDto result = webClient
                .mutateWith(MockUser.tvm(abcTvmSourceId))
                .post()
                .uri("/abc/_checkClosing?ignoreState=true")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(List.of(22L)))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClosingServiceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getClosing());
        Assertions.assertEquals(1, result.getClosing().size());
        Assertions.assertTrue(result.getClosing().contains(new ValueByServiceIdDto(22L, true)));
    }

    @Test
    public void checkClosingServicesForbiddenErrorTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/abc/_checkClosing", 1)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(List.of(1L)))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    public void checkClosingServicesNotFoundTest() {
        ErrorCollectionDto errorCollection = webClient
                .mutateWith(MockUser.tvm(abcTvmSourceId))
                .post()
                .uri("/abc/_checkClosing", 1)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(List.of(1L, 999L)))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(errorCollection);
        Map<String, Set<String>> errors = errorCollection.getFieldErrors();
        Assertions.assertNotNull(errors);
        Assertions.assertNotNull(errors.get("serviceIds.1"));
        Assertions.assertTrue(errors.get("serviceIds.1").contains("Service not found."));
    }

    @Test
    public void checkClosingServicesWithEmptyListTest() {
        ErrorCollectionDto errorCollection = webClient
                .mutateWith(MockUser.tvm(abcTvmSourceId))
                .post()
                .uri("/abc/_checkClosing", 1)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(List.of()))
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(errorCollection);
        Map<String, Set<String>> errors = errorCollection.getFieldErrors();
        Assertions.assertNotNull(errors);
        Assertions.assertNotNull(errors.get("serviceIds"));
        Assertions.assertTrue(errors.get("serviceIds").contains("Field is required."));
    }

    @Test
    public void checkClosingServicesWithoutListTest() {
        ErrorCollectionDto errorCollection = webClient
                .mutateWith(MockUser.tvm(abcTvmSourceId))
                .post()
                .uri("/abc/_checkClosing", 1)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(null))
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(errorCollection);
        Map<String, Set<String>> errors = errorCollection.getFieldErrors();
        Assertions.assertNotNull(errors);
        Assertions.assertNotNull(errors.get("serviceIds"));
        Assertions.assertTrue(errors.get("serviceIds").contains("Field is required."));
    }

    @Test
    public void checkClosingServicesWithoutFoldersTest() {
        ClosingServiceDto result = webClient
                .mutateWith(MockUser.tvm(abcTvmSourceId))
                .post()
                .uri("/abc/_checkClosing", 1)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(List.of(18L)))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClosingServiceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getClosing());
        Assertions.assertEquals(1, result.getClosing().size());
        Optional<ValueByServiceIdDto> value =
                result.getClosing().stream().filter(dto -> dto.getServiceId() == 18L).findFirst();
        Assertions.assertTrue(value.isPresent());
        Assertions.assertTrue(value.get().isValue());
    }

    @Test
    public void checkClosingServicesWithVirtualResourcesTest() {
        ClosingServiceDto result = webClient
                .mutateWith(MockUser.tvm(abcTvmSourceId))
                .post()
                .uri("/abc/_checkClosing", 1)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(List.of(21L)))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClosingServiceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getClosing());
        Assertions.assertEquals(1, result.getClosing().size());
        Optional<ValueByServiceIdDto> value =
                result.getClosing().stream().filter(dto -> dto.getServiceId() == 21L).findFirst();
        Assertions.assertTrue(value.isPresent());
        Assertions.assertTrue(value.get().isValue());
    }

    @Test
    public void checkNotClosingNonEmptyServicesTest() {
        ClosingServiceDto result = webClient
                .mutateWith(MockUser.tvm(abcTvmSourceId))
                .post()
                .uri("/abc/_checkClosing", 1)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new CheckServicesRequestDto(List.of(13L)))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClosingServiceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getClosing());
        Assertions.assertEquals(1, result.getClosing().size());
        Optional<ValueByServiceIdDto> value =
                result.getClosing().stream().filter(dto -> dto.getServiceId() == 13L).findFirst();
        Assertions.assertTrue(value.isPresent());
        Assertions.assertFalse(value.get().isValue());
    }
}
