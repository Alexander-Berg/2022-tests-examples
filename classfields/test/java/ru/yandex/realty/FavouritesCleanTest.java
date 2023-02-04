package ru.yandex.realty;

import org.apache.commons.collections.ListUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.concurrent.CommonThreadFactory;
import ru.yandex.common.util.concurrent.Executors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: daedra
 * Date: 15.05.14
 * Time: 16:39
 */
@ContextConfiguration(locations = "/favourites_clean.xml")
public class FavouritesCleanTest extends AbstractJUnit4SpringContextTests {
    private static final Logger log = LoggerFactory.getLogger(FavouritesCleanTest.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final String FILENAME = "/Users/daedra/tmp/invalid.txt";
    private static final String requestPatterm = "http://localhost:36255/getOffersByIdV15.json?1=1";
    private HttpClient httpClient;
    private ConcurrentHashMap<String, UserData> currentUsers = new ConcurrentHashMap<>();
    private Lock writeLock = new ReentrantLock();
    private ExecutorService executorService;

    class UserData {
        public LinkedBlockingQueue<Long> allIds = new LinkedBlockingQueue<>();
        public LinkedBlockingQueue<Long> invalid = new LinkedBlockingQueue<>();
        public AtomicBoolean finished = new AtomicBoolean(false);
        public AtomicBoolean written = new AtomicBoolean(false);
        public LinkedBlockingQueue<Task> tasks = new LinkedBlockingQueue<>();
        public AtomicInteger activeTasks = new AtomicInteger(0);
        public ReentrantLock lock = new ReentrantLock();
        public String type;
        public String uid;

        UserData(String type, String uid) {
            this.type = type;
            this.uid = uid;
        }
    }

    class Task {
        public List<Long> ids;

        Task(List<Long> ids) {
            this.ids = ids;
        }
    }

    @Test
    @Ignore
    public void cleanFavourites() throws IOException {
        httpClient = HttpClientBuilder.create().useSystemProperties().build();
        File file = new File(FILENAME);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream outputStream = new FileOutputStream(file);
        executorService = Executors.newBlockingFixedThreadPool(10, 1, new CommonThreadFactory(true, "t"), Long.MAX_VALUE);

        MyRowCallbackHandler myRowCallbackHandler = new MyRowCallbackHandler(outputStream);

        try {
            PreparedStatementSetter preparedStatementSetter = new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    ps.setFetchSize(Integer.MIN_VALUE);
                }
            };
            jdbcTemplate.query("select * from realty_favorites order by c1", preparedStatementSetter, myRowCallbackHandler);
            processIds(myRowCallbackHandler.currentIds, outputStream, myRowCallbackHandler.currentUid, myRowCallbackHandler.currentType, true);
/*
            processIds(myRowCallbackHandler.currentIds, myRowCallbackHandler);
            outputStream.write(("total: " + myRowCallbackHandler.totalIds.get() + "\tinvalid: " + myRowCallbackHandler.inexistingIds.get() + "\n").getBytes());
*/
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            outputStream.flush();
            outputStream.close();
        }
    }

    class MyRowCallbackHandler implements RowCallbackHandler {
        public List<Long> currentIds = new ArrayList<>(100);
        public String currentUid;
        public String currentType;
        private final FileOutputStream outputStream;
        private int processed = 0;

        MyRowCallbackHandler(FileOutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            processed++;
            if (processed % 100 == 0) {
                log.info("processed = " + processed);
            }
            String offerIdStr = rs.getString("c3");
            Long offerId = null;
            try {
                offerId = Long.parseLong(offerIdStr);
            } catch (NumberFormatException e) {
                log.error("Invalid long: " + offerIdStr);
                return;
            }
            String newUid = rs.getString("c2");
            String newType = rs.getString("c1");
            if (currentIds.size() >= 100 || !newUid.equals(currentUid) && currentUid != null) {
                processIds(currentIds, outputStream, currentUid, currentType, !newUid.equals(currentUid));
                currentIds.clear();
            }
            currentIds.add(offerId);
            if (!newUid.equals(currentUid)) {
                currentUid = newUid;
                currentType = newType;
            }
        }
    }

    class RunnableTask implements Runnable {
        private OutputStream outputStream;
        private String uid;

        RunnableTask(OutputStream outputStream, String uid) {
            this.outputStream = outputStream;
            this.uid = uid;
        }

        @Override
        public void run() {
            try {
                UserData userData = currentUsers.get(uid);
                if (userData == null) {
                    log.error("No user data: " + userData);
                    return;
                }
                List<Long> currentIds = userData.tasks.poll().ids;
                StringBuilder url = new StringBuilder(requestPatterm);
                for (Long id : currentIds) {
                    url.append("&id=").append(id);
                }
                int deleted = 0;
                try {
                    HttpGet httpGet = new HttpGet(url.toString());
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    JSONArray offers = null;
                    try {
                        if (httpResponse.getStatusLine().getStatusCode() != 200) {
                            log.error("wrong response code on url: " + url.toString());
                        }
                        JSONObject response = new JSONObject(IOUtils.readInputStream(httpResponse.getEntity().getContent()));
                        offers = response.getJSONArray("data").getJSONObject(0).getJSONArray("offers");
                    } finally {
                        EntityUtils.consume(httpResponse.getEntity());
                    }
                    if (offers == null) {
                        log.error("hren' happend");
                        return;
                    }
                    List<Long> existingOffers = new LinkedList<>();
                    for (int i = 0; i < offers.length(); i++) {
                        JSONObject offer = offers.getJSONObject(i);
                        Long id = Long.valueOf(offer.getString("offerId"));
                        existingOffers.add(id);
                    }
                    List<Long> inexistingOffers = ListUtils.subtract(currentIds, existingOffers);
                    for (Long invalidId : inexistingOffers) {
                        String deleteUrl = "http://localhost:36131/favorites/1.0/realty/" + userData.type + "/" + userData.uid + "/" + invalidId;
                        HttpDelete httpDelete = new HttpDelete(deleteUrl);
                        try {
                            HttpResponse deleteResponse = httpClient.execute(httpDelete);
                            String result = IOUtils.readInputStream(deleteResponse.getEntity().getContent());
                            if (deleteResponse.getStatusLine().getStatusCode() != 200) {
                                log.error("wrong response code for delete url: " + deleteUrl);
                            }
                            deleted++;
                        } finally {
                            EntityUtils.consume(httpResponse.getEntity());
                        }
                    }
                    userData.allIds.addAll(currentIds);
                    userData.invalid.addAll(inexistingOffers);
                    userData.activeTasks.decrementAndGet();
                    userData.lock.lock();
                    try {
                        if (userData.finished.get() && userData.activeTasks.get() == 0 && !userData.written.get()) {
                            writeLock.lock();
                            try {
                                outputStream.write(("uid: " + uid + "\n").getBytes());
                                for (Long inexistingId : inexistingOffers) {
                                    outputStream.write(("\t\tcid: " + uid + "\tinexisting:\t" + String.valueOf(inexistingId) + "\n").getBytes());
                                }
                                outputStream.write(("uid: " + uid + "\ttotal: " + userData.allIds.size() + "\tinvalid:" + userData.invalid.size() + "\tgood:" + (userData.allIds.size() - userData.invalid.size()) + "\n").getBytes());
                            } finally {
                                writeLock.unlock();
                            }
                            currentUsers.remove(uid);
                            userData.written.set(true);
                        }
                    } finally {
                        userData.lock.unlock();
                    }
                } catch (IOException | JSONException e) {
                    log.error(e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void processIds(@NotNull List<Long> currentIds, OutputStream outputStream, String uid, String type, boolean finish) {
        if (currentIds.isEmpty()) return;
        UserData userData = currentUsers.get(uid);
        if (userData == null) {
            userData = new UserData(type, uid);
            currentUsers.put(uid, userData);
        }
        userData.tasks.add(new Task(new ArrayList<>(currentIds)));
        userData.activeTasks.incrementAndGet();
        userData.finished.set(finish);
        executorService.submit(new RunnableTask(outputStream, uid));
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
