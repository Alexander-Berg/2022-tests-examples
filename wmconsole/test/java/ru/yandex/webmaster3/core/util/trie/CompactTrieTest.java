package ru.yandex.webmaster3.core.util.trie;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author avhaliullin
 */
public class CompactTrieTest {
    private static void log(String msg) {
        System.out.println(msg);
    }

    @Test
    public void testTrie() {
        for (int i = 1; i <= 50001; i += 100000) {
            long start = System.currentTimeMillis();
            Map<ByteBuffer, String> controlMap = new HashMap<>();
            CompactTrieBuilder<String> trie = new CompactTrieBuilder<>();
            fillRandomData(controlMap, trie, i);
//            fillData(controlMap, trie, "eP");
            long preparedData = System.currentTimeMillis();
            log(i + " prepared data in " + (preparedData - start));
            try {
                CompactTrie<String> cmpTrie = CompactTrie.fromNode(trie, new TrieValueCodec<String>() {
                    @Override
                    public int write(String value, TrieBufferWriter buffer, int offset) {
                        offset = buffer.writeInt(offset, value.length());
                        return buffer.writeBytes(offset, value.getBytes(), 0, value.length());
                    }

                    @Override
                    public String read(TrieBufferReader data) {
                        int len = data.readInt();
                        return new String(data.readBytes(len));
                    }

                    @Override
                    public void skip(TrieBufferReader data) {
                        int len = data.readInt();
                        data.skip(len);
                    }
                });
                long compacted = System.currentTimeMillis();
                log(i + " compacted in " + (compacted - preparedData));
                trie = new CompactTrieBuilder<>();
                fillRandomData(controlMap, trie, i);
//                fillData(controlMap, trie, "fH");
                long prepared2 = System.currentTimeMillis();
                log(i + " prepared2 in " + (prepared2 - compacted));
//                CompactTrie<String> prevTrie = cmpTrie;
                cmpTrie = CompactTrie.fromNode(MergedNodeImpl.createMerged(cmpTrie.getRootNode(), trie.root, (a, b) -> b), cmpTrie.getValueCodec());
                long merged = System.currentTimeMillis();
                log(i + " merged in " + (merged - prepared2));
//                int trieSize = 0;
//                for (Map.Entry<byte[], String> entry : trie) {
//                    String controlValue = controlMap.get(ByteBuffer.wrap(entry.getKey()));
//                    Assert.assertEquals("At " + controlMap, controlValue, entry.getValue());
//                    trieSize++;
//                }
//                Assert.assertEquals(controlMap.size(), trieSize);
                MutableInt size = new MutableInt();
                cmpTrie.foreach((key, keyLen, value) -> {
                    String expValue = controlMap.get(ByteBuffer.wrap(key, 0, keyLen));
                    Assert.assertEquals(expValue, value);
                    size.increment();
                }, null);
                Assert.assertEquals("At " + i + " " + controlMap, controlMap.size(), size.intValue());

                MutableInt size1 = new MutableInt();
                cmpTrie.foreach(null, (key, keyLen) -> {
                    size1.increment();
                });
                Assert.assertEquals("At " + i + " " + controlMap, controlMap.size(), size1.intValue());

                long fullScanned = System.currentTimeMillis();
                log(i + " scanned in " + (fullScanned - merged));
                for (Map.Entry<ByteBuffer, String> entry : controlMap.entrySet()) {
                    String cmctTrieValue = cmpTrie.get(entry.getKey().array());
                    Assert.assertEquals(entry.getValue(), cmctTrieValue);
                }
                log(i + " finished in " + (System.currentTimeMillis() - fullScanned));
            } catch (Throwable e) {
                throw new RuntimeException("At " + i + " " + controlMap, e);
            }
        }
    }

    private static void fillData(Map<ByteBuffer, String> controlMap, CompactTrieBuilder<String> trie, String... data) {
        for (String s : data) {
            byte[] key = s.getBytes(StandardCharsets.US_ASCII);
            controlMap.put(ByteBuffer.wrap(key), s);
            trie.insert(key, s);
        }
    }

    private static void fillRandomData(Map<ByteBuffer, String> controlMap, CompactTrieBuilder<String> trie, int size) {
        for (int i = 0; i < size; i++) {
            String s = RandomStringUtils.randomAlphanumeric(1, 45);
            byte[] key = s.getBytes(StandardCharsets.US_ASCII);
            controlMap.put(ByteBuffer.wrap(key), s);
            trie.insert(key, s);
        }
    }

    private TrieValueCodec<String> driver = new TrieValueCodec<String>() {
        @Override
        public int write(String value, TrieBufferWriter buffer, int offset) {
            offset = buffer.writeInt(offset, value.length());
            return buffer.writeBytes(offset, value.getBytes(), 0, value.length());
        }

        @Override
        public String read(TrieBufferReader data) {
            int len = data.readInt();
            return new String(data.readBytes(len));
        }

        @Override
        public void skip(TrieBufferReader data) {
            int len = data.readInt();
            data.skip(len);
        }
    };

    @Test
    public void testHoles1() {
        String value1 = "1";
        String str1 = "aa";
        String value2 = "2";
        String str2 = "ac";
        String value3 = "3";
        String str3 = "ae";
        CompactTrieBuilder<String> bor = new CompactTrieBuilder<>();
        bor.insert(str1.getBytes(), value1);
        bor.insert(str2.getBytes(), value2);
        bor.insert(str3.getBytes(), value3);
        CompactTrie<String> trie = CompactTrie.fromNode(bor, driver);
        Assert.assertEquals("1", trie.get("aa".getBytes()));
        Assert.assertEquals("2", trie.get("ac".getBytes()));
        Assert.assertEquals(null, trie.get("ab".getBytes()));
        Assert.assertEquals("3", trie.get("ae".getBytes()));
        Assert.assertEquals(null, trie.get("ad".getBytes()));
    }

    @Test
    public void testHoles2() {
        String value1 = "1";
        String str1 = "zsdjddfsakjasjdjdjsdlsssdaaa";
        String value2 = "2";
        String str2 = "zsdjddfsakjasjdjdjsdlsssdaac";
        String value3 = "3";
        String str3 = "zsdjddfsakjasjdjdjsdlsssdaae";
        String value4 = "4";
        String str4 = "zsdjddfsakjasjdjdjsdlsssdaag";
        CompactTrieBuilder<String> bor = new CompactTrieBuilder<>();
        bor.insert(str1.getBytes(), value1);
        bor.insert(str2.getBytes(), value2);
        bor.insert(str3.getBytes(), value3);
        bor.insert(str4.getBytes(), value4);
        CompactTrie<String> trie = CompactTrie.fromNode(bor, driver);
        Assert.assertEquals("1", trie.get(str1.getBytes()));
        Assert.assertEquals("2", trie.get(str2.getBytes()));
        Assert.assertEquals(null, trie.get("zaab".getBytes()));
        Assert.assertEquals("3", trie.get(str3.getBytes()));
        Assert.assertEquals(null, trie.get("zaad".getBytes()));
        Assert.assertEquals("4", trie.get(str4.getBytes()));
    }

    @Test
    public void testHolesWithMerge() {
        String value1 = "1";
        String str1 = "zsdjddfsakjasjdjdjsdlsssdaaa";
        String value2 = "2";
        String str2 = "zsdjddfsakjasjdjdjsdlsssdaac";
        String value3 = "3";
        String str3 = "zsdjddfsakjasjdjdjsdlsssdaae";
        String value4 = "4";
        String str4 = "zsdjddfsakjasjdjdjsdlsssdaag";
        CompactTrieBuilder<String> bor = new CompactTrieBuilder<>();
        bor.insert(str1.getBytes(), value1);
        bor.insert(str2.getBytes(), value2);
        CompactTrieBuilder<String> bor1 = new CompactTrieBuilder<>();
        bor1.insert(str3.getBytes(), value3);
        bor1.insert(str4.getBytes(), value4);
        CompactTrie<String> trie = CompactTrie.fromNode(bor, driver);
        CompactTrie<String> trie1 = CompactTrie.fromNode(MergedNodeImpl.createMerged(trie.getRootNode(), bor1.getRoot(), (a, b) -> b), driver);
        Assert.assertEquals("1", trie1.get(str1.getBytes()));
        Assert.assertEquals("2", trie1.get(str2.getBytes()));
        Assert.assertEquals(null, trie1.get("zaab".getBytes()));
        Assert.assertEquals("3", trie1.get(str3.getBytes()));
        Assert.assertEquals(null, trie1.get("zaad".getBytes()));
        Assert.assertEquals("4", trie1.get(str4.getBytes()));
    }

    @Test
    public void test2() {
        String value1 = "1";
        String str1 = "bab";
        String value2 = "2";
        String str2 = "fab";
        String value3 = "3";
        String str3 = "gaz";
        String value4 = "4";
        String str4 = "gb";
        String value5 = "5";
        String str5 = "h";
        CompactTrieBuilder<String> bor = new CompactTrieBuilder<>();
        bor.insert(str4.getBytes(), value4);
        bor.insert(str2.getBytes(), value2);
        bor.insert(str5.getBytes(), value5);
        CompactTrieBuilder<String> bor1 = new CompactTrieBuilder<>();
        bor1.insert(str3.getBytes(), value3);
        bor1.insert(str1.getBytes(), value1);
        CompactTrie<String> trie = CompactTrie.fromNode(bor, driver);
        CompactTrie<String> trie1 = CompactTrie.fromNode(MergedNodeImpl.createMerged(trie.getRootNode(), bor1.getRoot(), (a, b) -> b), driver);
        Assert.assertEquals("1", trie1.get(str1.getBytes()));
        Assert.assertEquals("2", trie1.get(str2.getBytes()));
        Assert.assertEquals("3", trie1.get(str3.getBytes()));
        Assert.assertEquals("4", trie1.get(str4.getBytes()));
        Assert.assertEquals("5", trie1.get(str5.getBytes()));
    }

    @Test
    public void test3() {
        String value1 = "1";
        String str1 = "bab";
        String value2 = "2";
        String str2 = "fab";
        String value3 = "3";
        String str3 = "gaz";
        String value4 = "4";
        String str4 = "gb";
        String value5 = "5";
        String str5 = "h";
        String value6 = "6";
        String str6 = ".";
        CompactTrieBuilder<String> bor = new CompactTrieBuilder<>();
        bor.insert(str4.getBytes(), value4);
        bor.insert(str2.getBytes(), value2);
        bor.insert(str5.getBytes(), value5);
        CompactTrieBuilder<String> bor1 = new CompactTrieBuilder<>();
        bor1.insert(str3.getBytes(), value3);
        bor1.insert(str1.getBytes(), value1);
        CompactTrie<String> trie = CompactTrie.fromNode(bor, driver);
        CompactTrie<String> trie1 = CompactTrie.fromNode(MergedNodeImpl.createMerged(trie.getRootNode(), bor1.getRoot(), (a, b) -> b), driver);
        Assert.assertEquals("1", trie1.get(str1.getBytes()));
        Assert.assertEquals("2", trie1.get(str2.getBytes()));
        Assert.assertEquals("3", trie1.get(str3.getBytes()));
        Assert.assertEquals("4", trie1.get(str4.getBytes()));
        Assert.assertEquals("5", trie1.get(str5.getBytes()));
        CompactTrieBuilder<String> bor2 = new CompactTrieBuilder<>();
        bor2.insert(str6.getBytes(), value6);
        CompactTrie<String> trie2 = CompactTrie.fromNode(MergedNodeImpl.createMerged(trie1.getRootNode(), bor2.getRoot(), (a, b) -> b), driver);
        Assert.assertEquals("1", trie2.get(str1.getBytes()));
        Assert.assertEquals("2", trie2.get(str2.getBytes()));
        Assert.assertEquals("3", trie2.get(str3.getBytes()));
        Assert.assertEquals("4", trie2.get(str4.getBytes()));
        Assert.assertEquals("5", trie2.get(str5.getBytes()));
        Assert.assertEquals("6", trie2.get(str6.getBytes()));
        Assert.assertEquals(false, trie2.contains("ba".getBytes()));
        Assert.assertEquals(false, trie2.contains("b".getBytes()));
        Assert.assertEquals(false, trie2.contains("z".getBytes()));
        Assert.assertEquals(false, trie2.contains("a".getBytes()));
    }

    private static final UUIDDriver UUID_DRIVER = new UUIDDriver();

    @Test
    public void uuidStoreTest() {
        UUID value = UUID.fromString("1f8ffae0-aa73-11e7-9757-cd6951fc7696");
        WebmasterHostId host = new WebmasterHostId(WebmasterHostId.Schema.HTTP,
                "silalesa.ru",
                WebmasterHostId.DEFAULT_HTTP_PORT);
        CompactTrieBuilder<UUID> bor = new CompactTrieBuilder<>();
        byte[] data = hostConverter.toRawData(host);
        bor.insert(data, value);
        CompactTrieBuilder<UUID> bor2 = new CompactTrieBuilder<>();
        WebmasterHostId host2 = new WebmasterHostId(WebmasterHostId.Schema.HTTPS,
                "silalesa.ru",
                WebmasterHostId.DEFAULT_HTTPS_PORT);
        byte[] data2 = hostConverter.toRawData(host2);
        bor2.insert(data2, value);
        CompactTrie<UUID> trie1 = CompactTrie.fromNode(bor, UUID_DRIVER);
        CompactTrieMap<WebmasterHostId, UUID> wrapper1 = new CompactTrieMap<>(trie1, hostConverter);
        CompactTrie<UUID> trie2 = CompactTrie.fromNode(MergedNodeImpl.createMerged(trie1.getRootNode(), bor2.getRoot(), (a, b) -> b), UUID_DRIVER);
        CompactTrieMap<WebmasterHostId, UUID> wrapper2 = new CompactTrieMap<>(trie2, hostConverter);
        Assert.assertEquals("1f8ffae0-aa73-11e7-9757-cd6951fc7696", wrapper1.get(host).toString());
        Assert.assertEquals(null, wrapper1.get(host2));
        Assert.assertEquals("1f8ffae0-aa73-11e7-9757-cd6951fc7696", wrapper2.get(host2).toString());
    }

    private static class UUIDDriver implements TrieValueCodec<UUID> {
        private final int LEN_BYTES = 16;

        @Override
        public int write(UUID value, TrieBufferWriter buffer, int offset) {
            offset = buffer.writeLong(offset, value.getMostSignificantBits());
            return buffer.writeLong(offset, value.getLeastSignificantBits());
        }

        @Override
        public UUID read(TrieBufferReader data) {
            return new UUID(data.readLong(), data.readLong());
        }

        @Override
        public void skip(TrieBufferReader data) {
            data.skip(LEN_BYTES);
        }
    }

    private static IDataConverter<WebmasterHostId> hostConverter = new IDataConverter<WebmasterHostId>() {
        @Override
        public byte[] toRawData(WebmasterHostId hostId) {
            return EncodeHostUtil.hostToByteArray(hostId);
        }

        @Override
        public WebmasterHostId fromRawData(byte[] data, int keyLen) {
            return EncodeHostUtil.fromByteArray(data, keyLen);
        }
    };
}
