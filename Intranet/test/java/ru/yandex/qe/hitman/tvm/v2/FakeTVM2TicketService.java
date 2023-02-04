package ru.yandex.qe.hitman.tvm.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

import ru.yandex.qe.hitman.tvm.FakeTVMService;

/**
 * Created by akhvorov on 07.09.17.
 */
public class FakeTVM2TicketService extends AbstractTVM2TicketService {

    private FakeTVMService tvmService = new FakeTVMService();

    public FakeTVM2TicketService(final ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public <T extends HttpRequest> T addTicketHeader(T request, String clientId, String destinationId, String secret) {
        return addTicketHeader(request, "X-Ya-Service-Ticket", clientId, destinationId, secret);
    }

    @Override
    public <T extends HttpRequest> T addTicketHeader(T request, String headerName, String clientId,
                                                     String destinationId, String secret) {
        final String timestamp = "1234567891";
        final String sign = sign(destinationId, secret, timestamp);
        request.addHeader(headerName, ticket(clientId, destinationId, sign, timestamp));
        return request;
    }

    private String ticket(final String clientId, final String destinationId, final String sign,
                          final String timestamp) {
        final HttpPost request = buildRequest(clientId, Collections.singletonList(destinationId), sign, timestamp);
        final HttpResponse response = tvmService.execute(request);
        final StringBuilder ticketSB = new StringBuilder();
        try {
            final InputStream responseEntity = response.getEntity().getContent();
            IOUtils.readLines(responseEntity).forEach(ticketSB::append);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ticketSB.toString();
    }
}
