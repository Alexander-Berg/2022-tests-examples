package ru.yandex.qe.hitman.tvm.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by akhvorov on 07.09.17.
 */
public class TVM2TicketServiceTest {
    @Test
    public void test() throws Exception {
        HttpUriRequest request = new HttpGet("localhost");
        final TVM2TicketService ticketService = new FakeTVM2TicketService(new ObjectMapper()
                .registerModules(new KotlinModule.Builder().build(), new Jdk8Module(), new JavaTimeModule()));
        request = ticketService.addTicketHeader(request, "Ticket", "321",
                "12345", "secret");
        assertTrue(request.containsHeader("Ticket"));
        assertEquals(request.getHeaders("Ticket")[0].getValue(),
                "https://tvm-api.yandex.net/2/ticket?grant_type=client_credentials&src=321&dst=12345&" +
                        "ts=1234567891&sign=SEKx0u6c155ybvi2CyL_tPqD-QWvM7oNsRyc2chLVt4");
    }
}
