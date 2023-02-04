package ru.yandex.webmaster3.worker.mirrors;

import java.util.Arrays;
import java.util.Collections;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.mirrors.dao.MainMirrorRequestsYDao;
import ru.yandex.webmaster3.storage.mirrors.data.MirrorActionEnum;
import ru.yandex.webmaster3.storage.mirrors.data.MirrorRequest;
import ru.yandex.webmaster3.storage.mirrors.data.MirrorRequestStateEnum;
import ru.yandex.webmaster3.worker.mirrors.MirrorServiceRequest.Action;

/**
 * @author aherman
 */
@SuppressWarnings("Duplicates")
public class MirrorRequestStateServiceTest extends TestCase {
    private static final MirrorServiceResponse.Result RESULT_A =
            new MirrorServiceResponse.Result(
                "OK",
                "http://ufk-techno.ru", null, null, null, null);
    private static final MirrorServiceResponse.Result RESULT_B =
            new MirrorServiceResponse.Result(
                "ERROR_BAD_REDIRECT",
                "http://www.isqua.ru", "https://isqua.ru", "https://isqua.ru", null, null);

    public void testResponseDeserialization() throws Exception {
        {
            String responseJson = "{\"Status\": \"ERROR_USER\", \"Results\": []}";
            MirrorServiceResponse response = MirrorServiceResponse.fromJson(responseJson);
            MirrorServiceResponse expected = new MirrorServiceResponse(
                    MirrorServiceResponse.Status.ERROR_USER,
                    Collections.emptyList(),
                    null
            );
            assertEquals(expected, response);
        }
        {
            String responseJson = "{\"Status\": \"OK\", \"Results\": [" +
                    "{ \"Status\": \"OK\", \"Host\": \"http://ufk-techno.ru\" }, " +
                    "{ \"Status\": \"ERROR_BAD_REDIRECT\", " +
                    "  \"Host\": \"http://www.isqua.ru\"," +
                    "  \"NewMain\": \"https://isqua.ru\"," +
                    "  \"Target\": \"https://isqua.ru\" }" +
            "]}";

            MirrorServiceResponse response = MirrorServiceResponse.fromJson(responseJson);

            MirrorServiceResponse expected = new MirrorServiceResponse(
                    MirrorServiceResponse.Status.OK,
                    Arrays.asList(RESULT_A, RESULT_B),
                    null
            );
            assertEquals(expected, response);
        }
    }

    public void testResponseSerialization() throws Exception {
        {
            MirrorServiceResponse before = new MirrorServiceResponse(
                    MirrorServiceResponse.Status.ERROR_INTERNAL, Collections.emptyList(),
                    null
            );

            // stays the same after serialization/deserialization cycle
            MirrorServiceResponse after = MirrorServiceResponse.fromJson(before.asJson());
            assertEquals(before, after);
        }
        {
            MirrorServiceResponse before = new MirrorServiceResponse(
                    MirrorServiceResponse.Status.OK, Arrays.asList(RESULT_A, RESULT_B),
                    null
            );

            // stays the same after serialization/deserialization cycle
            MirrorServiceResponse after = MirrorServiceResponse.fromJson(before.asJson());
            assertEquals(before, after);
        }
    }

    public void testResponseSerializationNotNull() throws Exception {
        MirrorServiceResponse response = new MirrorServiceResponse(
                MirrorServiceResponse.Status.OK, Collections.singletonList(RESULT_A),
                null
        );

        JsonNode jsonResponse = new ObjectMapper().readTree(response.asJson());
        JsonNode result = jsonResponse.get("Results").get(0);

        // assert that nulled fields aren't included in resulting json
        assertFalse(result.has("HttpCode"));
        assertFalse(result.has("NewMain"));
        assertFalse(result.has("Target"));
        // just in case
        assertTrue(result.has("Host"));
        assertTrue(result.has("Status"));
    }

    private void assertThrows(Runnable code) {
        try {
            code.run();
        } catch (Exception x) {
            // ok
            return;
        }
        fail("exception expected");
    }

    public void testRequestInvalidParams() {
        assertThrows(() -> new MirrorServiceRequest(null, null, null));
        assertThrows(() -> new MirrorServiceRequest(Action.MOVE, "ya.ru", null));
        assertThrows(() -> new MirrorServiceRequest(Action.MOVE, null, null));
        assertThrows(() -> new MirrorServiceRequest(Action.SPLIT, null, null));
        assertThrows(() -> new MirrorServiceRequest(Action.SPLIT, null, "ya.ru"));
    }

    public void testMoveRequestToUri() {
        String host = "www.ya.ru";
        String main = "https://ya.ru";
        MirrorServiceRequest request = new MirrorServiceRequest(Action.MOVE, host, main);

        String s = request.toUriString("http://example.com");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(s).build().getQueryParams();

        assertEquals(Collections.singletonList(host), queryParams.get("host"));
        assertEquals(Collections.singletonList(main), queryParams.get("main"));
        assertEquals(Collections.singletonList("move"), queryParams.get("action"));
        assertEquals(3, queryParams.size());
    }

    public void testUnstickRequestToUri() {
        String host = "www.ya.ru";
        MirrorServiceRequest request = new MirrorServiceRequest(Action.SPLIT, host, null);

        String s = request.toUriString("http://example.com");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(s).build().getQueryParams();

        assertEquals(Collections.singletonList(host), queryParams.get("host"));
        assertEquals(Collections.singletonList("split"), queryParams.get("action"));
        assertEquals(2, queryParams.size());
    }

    public void testMirrorActionEnumToAction() {
        assertEquals(Action.MOVE, Action.fromMirrorActionEnum(MirrorActionEnum.MOVE));
        assertEquals(Action.SPLIT, Action.fromMirrorActionEnum(MirrorActionEnum.UNSTICK));
        assertThrows(() -> Action.fromMirrorActionEnum(null));
    }

    public void testCheckNewMirrorRequestOk() throws Exception {
        MainMirrorRequestsYDao mainMirrorRequestsYDao = EasyMock.createMock(MainMirrorRequestsYDao.class);
        MirrorRequestStateService.DoMirrorRequest doRequest = req -> new MirrorServiceResponse(
                MirrorServiceResponse.Status.OK,
                Collections.emptyList(),
                null
        );
        MirrorRequestStateService mirrorRequestStateService =
                new MirrorRequestStateService(mainMirrorRequestsYDao, doRequest);

        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");
        WebmasterHostId wwwHostId = IdUtils.urlToHostId("http://www.example.com");

        MirrorRequest mirrorRequest = new MirrorRequest(hostId, UUIDs.startOf(1000), MirrorRequestStateEnum.NEW,
                hostId, wwwHostId, DateTime.now(), DateTime.now(), 0, "", MirrorActionEnum.MOVE, false);

        mainMirrorRequestsYDao.saveRequest(EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(() -> {
            MirrorRequest mr = (MirrorRequest) EasyMock.getCurrentArguments()[0];
            Assert.assertEquals(MirrorRequestStateEnum.WAITING, mr.getState());
            Assert.assertNotNull(mr.getServiceResponse());
            return null;
        });

        EasyMock.replay(mainMirrorRequestsYDao);
        assertTrue(mirrorRequestStateService.executeMirrorRequest(mirrorRequest));
        EasyMock.verify(mainMirrorRequestsYDao);
    }

    public void testCheckNewMirrorRequestUserError() throws Exception {
        MainMirrorRequestsYDao mainMirrorRequestsYDao = EasyMock.createMock(MainMirrorRequestsYDao.class);
        MirrorRequestStateService.DoMirrorRequest doRequest = req -> new MirrorServiceResponse(
                MirrorServiceResponse.Status.ERROR_USER,
                Collections.emptyList(),
                null
        );
        MirrorRequestStateService mirrorRequestStateService =
                new MirrorRequestStateService(mainMirrorRequestsYDao, doRequest);

        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");
        WebmasterHostId wwwHostId = IdUtils.urlToHostId("http://www.example.com");

        MirrorRequest mirrorRequest = new MirrorRequest(hostId, UUIDs.startOf(1000), MirrorRequestStateEnum.NEW,
                hostId, wwwHostId, DateTime.now(), DateTime.now(), 0, "", MirrorActionEnum.MOVE, false);

        mainMirrorRequestsYDao.saveRequest(EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(() -> {
            MirrorRequest mr = (MirrorRequest) EasyMock.getCurrentArguments()[0];
            Assert.assertEquals(MirrorRequestStateEnum.USER_ERROR, mr.getState());
            Assert.assertNotNull(mr.getServiceResponse());
            return null;
        });

        EasyMock.replay(mainMirrorRequestsYDao);
        Assert.assertTrue(mirrorRequestStateService.executeMirrorRequest(mirrorRequest));
        EasyMock.verify(mainMirrorRequestsYDao);
    }

    public void testCheckNewMirrorRequestServiceError() throws Exception {
        MainMirrorRequestsYDao mainMirrorRequestsYDao = EasyMock.createMock(MainMirrorRequestsYDao.class);
        MirrorRequestStateService.DoMirrorRequest doRequest = req -> new MirrorServiceResponse(
                MirrorServiceResponse.Status.ERROR_INTERNAL,
                Collections.emptyList(),
                null
        );
        MirrorRequestStateService mirrorRequestStateService =
                new MirrorRequestStateService(mainMirrorRequestsYDao, doRequest);

        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");
        WebmasterHostId wwwHostId = IdUtils.urlToHostId("http://www.example.com");

        MirrorRequest mirrorRequest = new MirrorRequest(hostId, UUIDs.startOf(1000), MirrorRequestStateEnum.NEW,
                hostId, wwwHostId, DateTime.now(), DateTime.now(), 0, "", MirrorActionEnum.MOVE, false);

        mainMirrorRequestsYDao.saveRequest(EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(() -> {
            MirrorRequest mr = (MirrorRequest) EasyMock.getCurrentArguments()[0];
            Assert.assertEquals(MirrorRequestStateEnum.SERVICE_ERROR, mr.getState());
            Assert.assertNotNull(mr.getServiceResponse());
            return null;
        });

        EasyMock.replay(mainMirrorRequestsYDao);
        assertFalse(mirrorRequestStateService.executeMirrorRequest(mirrorRequest));
        EasyMock.verify(mainMirrorRequestsYDao);
    }

    public void testCheckNewMirrorRequestErrorTimeout() throws Exception {
        MainMirrorRequestsYDao mainMirrorRequestsYDao = EasyMock.createMock(MainMirrorRequestsYDao.class);
        MirrorRequestStateService.DoMirrorRequest doRequest = req -> new MirrorServiceResponse(
                MirrorServiceResponse.Status.ERROR_TIMEOUT,
                Collections.emptyList(),
                null
        );
        MirrorRequestStateService mirrorRequestStateService =
                new MirrorRequestStateService(mainMirrorRequestsYDao, doRequest);

        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");
        WebmasterHostId wwwHostId = IdUtils.urlToHostId("http://www.example.com");

        MirrorRequest mirrorRequest = new MirrorRequest(hostId, UUIDs.startOf(1000), MirrorRequestStateEnum.NEW,
                hostId, wwwHostId, DateTime.now(), DateTime.now(), 0, "", MirrorActionEnum.MOVE, false);

        mainMirrorRequestsYDao.saveRequest(EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(() -> {
            MirrorRequest mr = (MirrorRequest) EasyMock.getCurrentArguments()[0];
            Assert.assertEquals(MirrorRequestStateEnum.SERVICE_ERROR, mr.getState());
            Assert.assertNotNull(mr.getServiceResponse());
            return null;
        });

        EasyMock.replay(mainMirrorRequestsYDao);
        assertFalse(mirrorRequestStateService.executeMirrorRequest(mirrorRequest));
        EasyMock.verify(mainMirrorRequestsYDao);
    }

    public void testWaitingRequestSuccess() throws Exception {
        MainMirrorRequestsYDao mainMirrorRequestsYDao = EasyMock.createMock(MainMirrorRequestsYDao.class);
        MirrorRequestStateService.DoMirrorRequest doRequest = req -> {
            throw new IllegalStateException("should not be called");
        };
        MirrorRequestStateService mirrorRequestStateService =
                new MirrorRequestStateService(mainMirrorRequestsYDao, doRequest);

        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");
        WebmasterHostId wwwHostId = IdUtils.urlToHostId("http://www.example.com");

        MirrorRequest mirrorRequest = new MirrorRequest(hostId, UUIDs.startOf(1000), MirrorRequestStateEnum.WAITING,
                hostId, wwwHostId, DateTime.now().minusDays(10), DateTime.now().minusDays(10), 0, "", MirrorActionEnum.MOVE, false);

        mainMirrorRequestsYDao.saveRequest(EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(() -> {
            MirrorRequest mr = (MirrorRequest) EasyMock.getCurrentArguments()[0];
            Assert.assertEquals(MirrorRequestStateEnum.SUCCESS, mr.getState());
            return null;
        });

        EasyMock.replay(mainMirrorRequestsYDao);
        mirrorRequestStateService.updateWaitingRequest(wwwHostId, mirrorRequest);
        EasyMock.verify(mainMirrorRequestsYDao);
    }

    public void testWaitingRequestWaiting() throws Exception {
        MainMirrorRequestsYDao mainMirrorRequestsYDao = EasyMock.createMock(MainMirrorRequestsYDao.class);
        MirrorRequestStateService.DoMirrorRequest doRequest = req -> {
            throw new IllegalStateException("should not be called");
        };
        MirrorRequestStateService mirrorRequestStateService =
                new MirrorRequestStateService(mainMirrorRequestsYDao, doRequest);

        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");
        WebmasterHostId wwwHostId = IdUtils.urlToHostId("http://www.example.com");

        MirrorRequest mirrorRequest = new MirrorRequest(hostId, UUIDs.startOf(1000), MirrorRequestStateEnum.WAITING,
                hostId, wwwHostId, DateTime.now().minusDays(10), DateTime.now().minusDays(10), 0, "", MirrorActionEnum.MOVE, false);

        EasyMock.replay(mainMirrorRequestsYDao);
        mirrorRequestStateService.updateWaitingRequest(hostId, mirrorRequest);
        EasyMock.verify(mainMirrorRequestsYDao);
    }

    public void testWaitingRequestExpired() throws Exception {
        MainMirrorRequestsYDao mainMirrorRequestsYDao = EasyMock.createMock(MainMirrorRequestsYDao.class);
        MirrorRequestStateService.DoMirrorRequest doRequest = req -> {
            throw new IllegalStateException("should not be called");
        };
        MirrorRequestStateService mirrorRequestStateService =
                new MirrorRequestStateService(mainMirrorRequestsYDao, doRequest);

        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");
        WebmasterHostId wwwHostId = IdUtils.urlToHostId("http://www.example.com");

        MirrorRequest mirrorRequest = new MirrorRequest(hostId, UUIDs.startOf(1000), MirrorRequestStateEnum.WAITING,
                hostId, wwwHostId, DateTime.now().minusDays(70), DateTime.now().minusDays(61), 0, "", MirrorActionEnum.MOVE, false);

        mainMirrorRequestsYDao.saveRequest(EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(() -> {
            MirrorRequest mr = (MirrorRequest) EasyMock.getCurrentArguments()[0];
            Assert.assertEquals(MirrorRequestStateEnum.EXPIRED, mr.getState());
            return null;
        });

        EasyMock.replay(mainMirrorRequestsYDao);
        mirrorRequestStateService.updateWaitingRequest(hostId, mirrorRequest);
        EasyMock.verify(mainMirrorRequestsYDao);
    }
}
