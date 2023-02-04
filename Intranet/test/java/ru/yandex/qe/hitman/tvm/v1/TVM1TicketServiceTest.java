package ru.yandex.qe.hitman.tvm.v1;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by akhvorov on 06.09.17.
 */

public class TVM1TicketServiceTest {
    @Test
    public void test() throws Exception {
        HttpUriRequest request = new HttpGet("localhost");
        final TVM1TicketService ticketService = new FakeTVM1TicketService();
        request = ticketService.addTicketHeader(request, "Ticket", "12345", "secret");
        assertTrue(request.containsHeader("Ticket"));
        assertEquals(request.getHeaders("Ticket")[0].getValue(),
                "https://tvm-api.yandex.net/ticket?grant_type=client_credentials&client_id=12345&" +
                        "ts=1234567891&ts_sign=48uw_mLJZM2-pxXO334VyBRkcmQADmtTrBo1hMBO4VA");
    }
}
