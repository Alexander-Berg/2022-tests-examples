package ru.yandex.partner.core.configuration;

import java.util.List;
import java.util.Optional;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.partner.core.service.adfox.AdfoxService;
import ru.yandex.partner.core.service.adfox.GraphqlResponse;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class AdfoxGraphqlTestConfig {

    @Bean
    public AdfoxService adfoxService() {
        var service = mock(AdfoxService.class);
        var mockResponse = new GraphqlResponse(
                List.of(),
                "ok"
        );

        Mockito.when(service.deleteBlock(anyList())).thenReturn(Optional.of(mockResponse));
        Mockito.when(service.updateBlockName(anyLong(), anyString())).thenReturn(Optional.of(mockResponse));

        return service;
    }
}
