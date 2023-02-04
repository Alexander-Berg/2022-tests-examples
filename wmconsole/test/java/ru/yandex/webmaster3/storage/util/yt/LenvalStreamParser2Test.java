package ru.yandex.webmaster3.storage.util.yt;

/**
 * @author aherman
 */
public class LenvalStreamParser2Test {
//    @Test
//    public void testSaveLoad() throws Exception {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        for (int i = 0; i < 100; i++) {
//            byte[] data = new byte[4];
//            // Key
//            ByteStreamUtil.writeIntLE(data, 0, i);
//            LenvalSaver.writeBytes(baos, data);
//            // Value
//            ByteStreamUtil.writeIntLE(data, 0, i * 10);
//            LenvalSaver.writeBytes(baos, data);
//        }
//
//        LenvalStreamParser2<Integer> parser2 = new LenvalStreamParser2<>(new ByteArrayInputStream(baos.toByteArray()),
//                new YtRowMapper<Integer>() {
//                    int k;
//                    int v;
//                    int l = 0;
//
//                    @Override
//                    public void nextField(String name, InputStream data) throws YtException, InterruptedException {
//                        int value;
//                        try {
//                            byte[] bytes = IOUtils.toByteArray(data);
//                            value = ByteStreamUtil.readIntLE(bytes, 0);
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                        if("key".equals(name)) {
//                            k = value;
//                        } if ("value".equals(name)) {
//                            v = value;
//                        }
//                    }
//
//                    @Override
//                    public Integer rowEnd() throws YtException, InterruptedException {
//                        Assert.assertEquals(l, k);
//                        Assert.assertEquals(l * 10, v);
//                        k = v = 0;
//                        l++;
//                        return l;
//                    }
//                });
//        int l = 0;
//        for (int i = 0; i < 100; i++) {
//            Assert.assertTrue(parser2.hasNext());
//            Assert.assertEquals(Integer.valueOf(i + 1), parser2.next());
//        }
//        Assert.assertFalse(parser2.hasNext());
//    }
}
