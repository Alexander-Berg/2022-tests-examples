package ru.yandex.partner.core.service.adfox;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.partner.test.utils.TestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class AdfoxGraphqlServiceTest {

    @Test
    void deleteBlockTest() throws Exception {
        var service = spy(AdfoxGraphqlService.class);
        doReturn(Optional.of(new GraphqlResponse(List.of(), "ok"))).when(service).send(any());
        ArgumentCaptor<GraphqlRequest> captor = ArgumentCaptor.forClass(GraphqlRequest.class);

        service.deleteBlock(List.of(123L, 456L));
        verify(service, Mockito.times(1)).send(captor.capture());

        var req = captor.getValue();
        TestUtils.compareToDataFromFile(req, this.getClass(), "deleteBlock.json");
    }

    @Test
    void updateBlockNameTest() throws Exception {
        var service = spy(AdfoxGraphqlService.class);
        doReturn(Optional.of(new GraphqlResponse(List.of(), "ok"))).when(service).send(any());
        ArgumentCaptor<GraphqlRequest> captor = ArgumentCaptor.forClass(GraphqlRequest.class);

        service.updateBlockName(123L, "asdf");
        verify(service, Mockito.times(1)).send(captor.capture());

        var req = captor.getValue();
        TestUtils.compareToDataFromFile(req, this.getClass(), "updateBlockName.json");
    }

    @Test
    void testGraphqlResponseDeserialization() throws IOException {
        var service = new AdfoxGraphqlService();
        var file = new ClassPathResource("deleteBlock-response.json").getFile();

        Assertions.assertDoesNotThrow(() -> {
            GraphqlResponse resp = service.getObjectMapper().readValue(file, GraphqlResponse.class);
        });
    }
}
