package ru.yandex.qe.hitman.tvm.v1;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.qe.hitman.tvm.FakeTVMService;

/**
 * Created by akhvorov on 07.09.17.
 */
public class FakeTVM1TicketService extends AbstractTVM1TicketService {
    private static final Logger LOG = LoggerFactory.getLogger(FakeTVM1TicketService.class);

    private FakeTVMService tvmService = new FakeTVMService();

    @Override
    public <T extends HttpRequest> T addTicketHeader(T request, String headerName, String clientId, String secret) {
        final String timestamp = "1234567891";
        request.addHeader(headerName, ticket(clientId, tsSign(secret, timestamp), timestamp));
        return request;
    }

    private String ticket(final String clientId, final String tsSign, final String timestamp) {
        final HttpPost request = buildRequest(clientId, tsSign, timestamp);
        LOG.debug("Requesting to TVM: {}", request.getURI().toString(), "ts_sign");
        final HttpResponse response = tvmService.execute(request);
        return parseResponse(response);
    }
}
