//TODO: fix
// package ru.yandex.webmaster3.worker.sitetree;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang3.tuple.Triple;
//import org.easymock.EasyMock;
//import org.junit.Assert;
//import org.junit.Test;
//
//import ru.yandex.webmaster3.storage.user.dao.UserSiteTreeCDao;
//import ru.yandex.webmaster3.worker.yt.WebmasterYtService;
//
///**
// * User: azakharov
// * Date: 08.10.14
// * Time: 15:11
// */
//public class UploadUserNodesTaskTest {
//
//    @Test
//    public void testEmptyTable() throws Exception {
//        UploadUserNodesTask task = new UploadUserNodesTask();
//
//        TableWriterMatcher matcher = new TableWriterMatcher() {
//            @Override
//            public String matches(WebmasterYtService.TableContentWriter tableWriter) {
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                tableWriter.writeTo(out);
//                if (out.size() == 0) {
//                    return OK;
//                } else {
//                    return "Unexpected non-empty output found";
//                }
//            }
//        };
//        MockUserSiteTreeYtService mockUserSiteTreeYtService = new MockUserSiteTreeYtService(matcher);
//        task.setWebmasterYtService(mockUserSiteTreeYtService);
//
//        UserSiteTreeCDao mockUserSiteTreeCDao = EasyMock.createMock(UserSiteTreeCDao.class);
//        EasyMock.expect(mockUserSiteTreeCDao.getBatch(Long.MIN_VALUE, 10000))
//                .andReturn(Collections.<Triple<Long, String, String>>emptyList());
//        task.setUserSiteTreeCDao(mockUserSiteTreeCDao);
//
//        EasyMock.replay(mockUserSiteTreeCDao);
//
//        task.run(UUID.randomUUID());
//
//        Assert.assertEquals(mockUserSiteTreeYtService.getResult(), OK, mockUserSiteTreeYtService.getResult());
//    }
//
//    @Test
//    public void testOneHostInTable() throws Exception {
//        UploadUserNodesTask task = new UploadUserNodesTask();
//
//        TableWriterMatcher matcher = new TableWriterMatcher() {
//            @Override
//            public String matches(WebmasterYtService.TableContentWriter tableWriter) {
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                tableWriter.writeTo(out);
//                ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//                try {
//                    String s = IOUtils.toString(in);
//                    String[] lines = s.split("\\n");
//                    if (lines.length != 1) {
//                        return "Too many lines in output " + lines.length;
//                    }
//                    String[] columns = getColumns(lines[0]);
//                    if (columns.length != 3) {
//                        return "Unexpected number of columns " + columns.length;
//                    }
//                    if (!columns[0].equals("http://lenta.ru")) {
//                        return "Bad host name " + columns[0];
//                    }
//                    if (!columns[1].equals("/foo")) {
//                        return "Bad column value. Expected foo but was " + columns[1];
//                    }
//                    return OK;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return "Unexpected exception in IOUtils.toString " + e.getMessage();
//                }
//            }
//        };
//        MockUserSiteTreeYtService mockUserSiteTreeYtService = new MockUserSiteTreeYtService(matcher);
//        task.setWebmasterYtService(mockUserSiteTreeYtService);
//
//        UserSiteTreeCDao mockUserSiteTreeCDao = EasyMock.createMock(UserSiteTreeCDao.class);
//        List<Triple<Long, String, String>> batch = new ArrayList<>(3);
//        batch.add(Triple.of(100500l, "http:lenta.ru:80", "foo"));
//        batch.add(Triple.of(100500l, "http:lenta.ru:80", "bar"));
//        EasyMock.expect(mockUserSiteTreeCDao.getBatch(Long.MIN_VALUE, 10000))
//                .andReturn(batch);
//        task.setUserSiteTreeCDao(mockUserSiteTreeCDao);
//
//        EasyMock.replay(mockUserSiteTreeCDao);
//
//        task.run(UUID.randomUUID());
//
//        Assert.assertEquals(mockUserSiteTreeYtService.getResult(), OK, mockUserSiteTreeYtService.getResult());
//    }
//
//    @Test
//    public void testLessThanBatchSize() throws Exception {
//        UploadUserNodesTask task = new UploadUserNodesTask();
//
//        TableWriterMatcher matcher = new TableWriterMatcher() {
//            @Override
//            public String matches(WebmasterYtService.TableContentWriter tableWriter) {
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                tableWriter.writeTo(out);
//                ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//                try {
//                    String s = IOUtils.toString(in);
//                    String[] lines = s.split("\\n");
//                    if (lines.length != 2) {
//                        return "Unexpected amount of lines in output " + lines.length;
//                    }
//
//                    String[] columns0 = getColumns(lines[0]);
//                    if (columns0.length != 3) {
//                        return "Unexpected number of columns " + columns0.length;
//                    }
//                    if (!columns0[0].equals("http://lenta.ru")) {
//                        return "Bad host name " + columns0[0];
//                    }
//                    if (!columns0[1].equals("/foo")) {
//                        return "Bad column value. Expected foo but was " + columns0[1];
//                    }
//                    if (!columns0[2].equals("/bar")) {
//                        return "Bad column value. Expected bar but was " + columns0[2];
//                    }
//
//                    String[] columns1 = getColumns(lines[1]);
//                    if (columns1.length != 3) {
//                        return "Unexpected number of columns " + columns1.length;
//                    }
//                    if (!columns1[0].equals("http://www.afisha.ru")) {
//                        return "Bad host name " + columns1[0];
//                    }
//                    if (!columns1[1].equals("/baz")) {
//                        return "Bad column value. Expected foo but was " + columns1[1];
//                    }
//                    if (!columns1[2].equals("/bla")) {
//                        return "Bad column value. Expected bar but was " + columns1[2];
//                    }
//                    return OK;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return "Unexpected exception in IOUtils.toString " + e.getMessage();
//                }
//            }
//        };
//        MockUserSiteTreeYtService mockUserSiteTreeYtService = new MockUserSiteTreeYtService(matcher);
//        task.setWebmasterYtService(mockUserSiteTreeYtService);
//
//        UserSiteTreeCDao mockUserSiteTreeCDao = EasyMock.createMock(UserSiteTreeCDao.class);
//        List<Triple<Long, String, String>> batch0 = new ArrayList<>(4);
//        batch0.add(Triple.of(100500l, "http:lenta.ru:80", "foo"));
//        batch0.add(Triple.of(100500l, "http:lenta.ru:80", "bar"));
//        batch0.add(Triple.of(123123l, "http:www.afisha.ru:80", "baz"));
//        batch0.add(Triple.of(123123l, "http:www.afisha.ru:80", "bla"));
//        EasyMock.expect(mockUserSiteTreeCDao.getBatch(Long.MIN_VALUE, 10000))
//                .andReturn(batch0);
//        List<Triple<Long, String, String>> batch1 = new ArrayList<>(2);
//        batch1.add(Triple.of(123123l, "http:www.afisha.ru:80", "baz"));
//        batch1.add(Triple.of(123123l, "http:www.afisha.ru:80", "bla"));
//        EasyMock.expect(mockUserSiteTreeCDao.getBatch(EasyMock.eq(123123l), EasyMock.eq(10000)))
//                .andReturn(batch1);
//        task.setUserSiteTreeCDao(mockUserSiteTreeCDao);
//
//        EasyMock.replay(mockUserSiteTreeCDao);
//
//        task.run(UUID.randomUUID());
//
//        Assert.assertEquals(mockUserSiteTreeYtService.getResult(), OK, mockUserSiteTreeYtService.getResult());
//    }
//
//    @Test
//    public void testOneTokenTwoHosts() throws Exception {
//        UploadUserNodesTask task = new UploadUserNodesTask();
//
//        TableWriterMatcher matcher = new TableWriterMatcher() {
//            @Override
//            public String matches(WebmasterYtService.TableContentWriter tableWriter) {
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                tableWriter.writeTo(out);
//                ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//                try {
//                    String s = IOUtils.toString(in);
//                    String[] lines = s.split("\\n");
//                    if (lines.length != 2) {
//                        return "Unexpected amount of lines in output " + lines.length;
//                    }
//
//                    String[] columns0 = getColumns(lines[0]);
//                    if (columns0.length != 3) {
//                        return "Unexpected number of columns " + columns0.length;
//                    }
//                    if (!columns0[0].equals("http://lenta.ru")) {
//                        return "Bad host name " + columns0[0];
//                    }
//                    if (!columns0[1].equals("/foo")) {
//                        return "Bad column value. Expected foo but was " + columns0[1];
//                    }
//                    if (!columns0[2].equals("/bar")) {
//                        return "Bad column value. Expected bar but was " + columns0[2];
//                    }
//
//                    String[] columns1 = getColumns(lines[1]);
//                    if (columns1.length != 3) {
//                        return "Unexpected number of columns " + columns1.length;
//                    }
//                    if (!columns1[0].equals("http://www.afisha.ru")) {
//                        return "Bad host name " + columns1[0];
//                    }
//                    if (!columns1[1].equals("/baz")) {
//                        return "Bad column value. Expected foo but was " + columns1[1];
//                    }
//                    if (!columns1[2].equals("/bla")) {
//                        return "Bad column value. Expected bar but was " + columns1[2];
//                    }
//                    return OK;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return "Unexpected exception in IOUtils.toString " + e.getMessage();
//                }
//            }
//        };
//        MockUserSiteTreeYtService mockUserSiteTreeYtService = new MockUserSiteTreeYtService(matcher);
//        task.setWebmasterYtService(mockUserSiteTreeYtService);
//
//        UserSiteTreeCDao mockUserSiteTreeCDao = EasyMock.createMock(UserSiteTreeCDao.class);
//        List<Triple<Long, String, String>> batch0 = new ArrayList<>(4);
//        batch0.add(Triple.of(100500l, "http:lenta.ru:80", "foo"));
//        batch0.add(Triple.of(100500l, "http:lenta.ru:80", "bar"));
//        batch0.add(Triple.of(100500l, "http:www.afisha.ru:80", "baz"));
//        batch0.add(Triple.of(100500l, "http:www.afisha.ru:80", "bla"));
//        EasyMock.expect(mockUserSiteTreeCDao.getBatch(Long.MIN_VALUE, 10000))
//                .andReturn(batch0);
//        task.setUserSiteTreeCDao(mockUserSiteTreeCDao);
//
//        EasyMock.replay(mockUserSiteTreeCDao);
//
//        task.run(UUID.randomUUID());
//
//        Assert.assertEquals(mockUserSiteTreeYtService.getResult(), OK, mockUserSiteTreeYtService.getResult());
//    }
//
//    @Test
//    public void testBatchSize() throws Exception {
//        UploadUserNodesTask task = new UploadUserNodesTask();
//
//        TableWriterMatcher matcher = new TableWriterMatcher() {
//            @Override
//            public String matches(WebmasterYtService.TableContentWriter tableWriter) {
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                tableWriter.writeTo(out);
//                ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//                try {
//                    String s = IOUtils.toString(in);
//                    String[] lines = s.split("\\n");
//                    if (lines.length != 10000) {
//                        return "Unexpected amount of lines in output " + lines.length;
//                    }
//                    for (int i = 0; i < 10000; i++) {
//                        String[] columns = getColumns(lines[i]);
//                        if (columns.length != 2) {
//                            return "Unexpected number of columns " + columns.length;
//                        }
//                        if (!columns[0].equals("http://site"+i+".ru")) {
//                            return "Bad host name " + columns[0];
//                        }
//                        if (!columns[1].equals("/node"+i)) {
//                            return "Bad node name " + columns[1] + ". Expected /node"+i;
//                        }
//                    }
//                    return OK;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return "Unexpected exception in IOUtils.toString " + e.getMessage();
//                }
//            }
//        };
//        MockUserSiteTreeYtService mockUserSiteTreeYtService = new MockUserSiteTreeYtService(matcher);
//        task.setWebmasterYtService(mockUserSiteTreeYtService);
//
//        UserSiteTreeCDao mockUserSiteTreeCDao = EasyMock.createMock(UserSiteTreeCDao.class);
//        List<Triple<Long, String, String>> batch0 = new ArrayList<>(10000);
//        for (int i = 0; i < 10000; i++) {
//            String hostName = "http:site"+i+".ru:80";
//            batch0.add(Triple.of(Long.valueOf(hostName.hashCode()), hostName, "node" + i));
//        }
//        EasyMock.expect(mockUserSiteTreeCDao.getBatch(Long.MIN_VALUE, 10000))
//                .andReturn(batch0);
//        long lastToken = Long.valueOf("http:site9999.ru:80".hashCode());
//        List<Triple<Long, String, String>> batch1 = new ArrayList<>(10000);
//        batch1.add(Triple.of(lastToken, "http:site9999.ru:80", "node9999"));
//        EasyMock.expect(mockUserSiteTreeCDao.getBatch(lastToken, 10000))
//                .andReturn(batch1);
//        task.setUserSiteTreeCDao(mockUserSiteTreeCDao);
//
//        EasyMock.replay(mockUserSiteTreeCDao);
//
//        task.run(UUID.randomUUID());
//
//        Assert.assertEquals(mockUserSiteTreeYtService.getResult(), OK, mockUserSiteTreeYtService.getResult());
//    }
//
//    @Test
//    public void testGreaterThanBatchSize() throws Exception {
//        UploadUserNodesTask task = new UploadUserNodesTask();
//
//        TableWriterMatcher matcher = new TableWriterMatcher() {
//            @Override
//            public String matches(WebmasterYtService.TableContentWriter tableWriter) {
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                tableWriter.writeTo(out);
//                ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//                try {
//                    String s = IOUtils.toString(in);
//                    String[] lines = s.split("\\n");
//                    if (lines.length != 10000) {
//                        return "Unexpected amount of lines in output " + lines.length;
//                    }
//                    for (int i = 0; i < 10000-1; i++) {
//                        String[] columns = getColumns(lines[i]);
//                        if (columns.length != 2) {
//                            return "Unexpected number of columns " + columns.length;
//                        }
//                        if (!columns[0].equals("http://site"+i+".ru")) {
//                            return "Bad host name " + columns[0];
//                        }
//                        if (!columns[1].equals("/node"+i)) {
//                            return "Bad node name " + columns[1] + ". Expected node"+i;
//                        }
//                    }
//
//                    String[] columns = getColumns(lines[9999]);
//                    if (columns.length != 3) {
//                        return "Unexpected number of columns " + columns.length;
//                    }
//                    if (!columns[0].equals("http://site9999.ru")) {
//                        return "Bad host name " + columns[0];
//                    }
//                    if (!columns[1].equals("/node9999")) {
//                        return "Bad node name " + columns[1] + ". Expected node9999";
//                    }
//                    if (!columns[2].equals("/node10000")) {
//                        return "Bad node name " + columns[2] + ". Expected node10000";
//                    }
//
//                    return OK;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return "Unexpected exception in IOUtils.toString " + e.getMessage();
//                }
//            }
//        };
//        MockUserSiteTreeYtService mockUserSiteTreeYtService = new MockUserSiteTreeYtService(matcher);
//        task.setWebmasterYtService(mockUserSiteTreeYtService);
//
//        UserSiteTreeCDao mockUserSiteTreeCDao = EasyMock.createMock(UserSiteTreeCDao.class);
//        List<Triple<Long, String, String>> batch0 = new ArrayList<>(10000);
//        for (int i = 0; i < 10000; i++) {
//            String hostName = "http:site"+i+".ru:80";
//            batch0.add(Triple.of(Long.valueOf(hostName.hashCode()), hostName, "node" + i));
//        }
//        EasyMock.expect(mockUserSiteTreeCDao.getBatch(Long.MIN_VALUE, 10000))
//                .andReturn(batch0);
//        long lastToken = Long.valueOf("http:site9999.ru:80".hashCode());
//        List<Triple<Long, String, String>> batch1 = new ArrayList<>(10000);
//        batch1.add(Triple.of(lastToken, "http:site9999.ru:80", "node9999"));
//        batch1.add(Triple.of(lastToken, "http:site9999.ru:80", "node10000"));
//        EasyMock.expect(mockUserSiteTreeCDao.getBatch(lastToken, 10000))
//                .andReturn(batch1);
//        task.setUserSiteTreeCDao(mockUserSiteTreeCDao);
//
//        EasyMock.replay(mockUserSiteTreeCDao);
//
//        task.run(UUID.randomUUID());
//
//        Assert.assertEquals(mockUserSiteTreeYtService.getResult(), OK, mockUserSiteTreeYtService.getResult());
//    }
//
//    public static class MockUserSiteTreeYtService extends WebmasterYtService {
//
//        private final TableWriterMatcher matcher;
//        private String result;
//
//        public MockUserSiteTreeYtService(TableWriterMatcher matcher) {
//            this.matcher = matcher;
//        }
//
//        @Override
//        public void create(String tableName) {
//            // do nothing
//        }
//
//        @Override
//        public void remove(String tableName) {
//        }
//
//        @Override
//        public void write(String tableName, TableContentWriter tableWriter) {
//            result = matcher.matches(tableWriter);
//        }
//
//        public String getResult() {
//            return result;
//        }
//    }
//
//    public static interface TableWriterMatcher {
//        String matches(WebmasterYtService.TableContentWriter tableWriter);
//    }
//
//    public static String[] getColumns(String line) {
//        String[] columns = line.split("\\t");
//        if (columns.length != 2) {
//            return columns;
//        }
//
//        columns[0] = columns[0].startsWith("key=") ? columns[0].substring("key=".length()) : columns[0];
//        columns[1] = columns[1].startsWith("value=") ? columns[1].substring("value=".length()) : columns[1];
//
//        List<String> v = Arrays.asList(columns[1].split("\\\\t"));
//        ArrayList<String> res = new ArrayList<>(v.size() + 1);
//        res.add(columns[0]);
//        res.addAll(v);
//        return res.toArray(new String[v.size() + 1]);
//    }
//
//    public final static String OK = "ok";
//}
