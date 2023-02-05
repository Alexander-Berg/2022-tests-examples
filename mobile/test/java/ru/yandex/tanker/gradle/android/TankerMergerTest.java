package ru.yandex.tanker.gradle.android;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ru.yandex.tanker.gradle.android.types.StringValue;
import ru.yandex.tanker.gradle.android.types.Value;
import static org.fest.assertions.api.Assertions.assertThat;

public class TankerMergerTest {

    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    @Test
    public void testMatrix() throws Exception {
        Class<TankerMerger> tankerMergerClass = TankerMerger.class;
        Method method = tankerMergerClass.getDeclaredMethod("initKeyData", Collection.class);
        method.setAccessible(true);

        Set<TankerMerger.Language> set = new HashSet<>();
        TankerMerger tankerMerger = new TankerMerger("en", "ru", "strings.xml", false);
        TankerMerger.Language langEn = tankerMerger.new Language("en");
        TankerMerger.Language langRu = tankerMerger.new Language("ru");
        TankerMerger.Language langBe = tankerMerger.new Language("be");

        langEn.add("widget_name", new StringValue("buggy widget"), "test");
        langRu.add("widget_name", new StringValue("виджет"), "test");
        langEn.add("text_search", new StringValue("search"), "test");
        langRu.add("text_about", new StringValue("о приложении"), "test");
        langBe.add("like", new StringValue("падабайка"), "test");
        langEn.add("yandex", new StringValue("yandex"), "test");
        langBe.add("yandex", new StringValue("яндекс"), "test");
        set.add(langEn);
        set.add(langRu);
        set.add(langBe);

        //noinspection unchecked
        Map<String, TankerMerger.KeyData> map =
                (Map<String, TankerMerger.KeyData>) method.invoke(tankerMerger, set);

        TankerMerger.KeyData keyData = map.get("widget_name");
        Assert.assertEquals(new StringValue("buggy widget"), keyData.getValue("en"));
        Assert.assertEquals(new StringValue("виджет"), keyData.getValue("ru"));
        Assert.assertEquals(new StringValue("виджет"), keyData.getValue("be"));
        keyData = map.get("text_search");
        Assert.assertEquals(new StringValue("search"), keyData.getValue("en"));
        Assert.assertEquals(new StringValue("search"), keyData.getValue("ru"));
        Assert.assertEquals(new StringValue("search"), keyData.getValue("be"));
        keyData = map.get("text_about");
        Assert.assertEquals(new StringValue("о приложении"), keyData.getValue("en"));
        Assert.assertEquals(new StringValue("о приложении"), keyData.getValue("ru"));
        Assert.assertEquals(new StringValue("о приложении"), keyData.getValue("be"));
        keyData = map.get("like");
        Assert.assertEquals(new StringValue("падабайка"), keyData.getValue("en"));
        Assert.assertEquals(new StringValue("падабайка"), keyData.getValue("ru"));
        Assert.assertEquals(new StringValue("падабайка"), keyData.getValue("be"));
        keyData = map.get("yandex");
        Assert.assertEquals(new StringValue("yandex"), keyData.getValue("en"));
        Assert.assertEquals(new StringValue("yandex"), keyData.getValue("ru"));
        Assert.assertEquals(new StringValue("яндекс"), keyData.getValue("be"));
    }

    @Test
    public void checkDeleteWarnings() throws IOException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

        Class<TankerMerger> tankerMergerClass = TankerMerger.class;
        Method method = tankerMergerClass.getDeclaredMethod("warnIfDeleteStrings", String.class,
                Collection.class);
        method.setAccessible(true);
        final PrintStream out = new PrintStream(outputStream);
        System.setOut(out);

        //noinspection ResultOfMethodCallIgnored
        new File("res/values/").mkdirs();
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("res/values/strings.xml"));
        writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <string name=\"widget_name\">widget</string>\n" +
                "</resources>");
        writer.flush();
        writer.close();
        //noinspection ResultOfMethodCallIgnored
        new File("res/values-ru/").mkdirs();
        File file = new File("res/values-ru/strings.xml");
        writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <string name=\"widget_name\">виджет</string>\n" +
                "    <string name=\"search\">поиск</string>\n" +
                "</resources>");
        writer.flush();
        writer.close();
        TankerMerger tankerMerger = new TankerMerger("en", "ru", "strings.xml", false);
        Set<TankerMerger.Language> set = new HashSet<>();
        set.add(tankerMerger.new Language("en"));
        set.add(tankerMerger.new Language("ru"));
        method.invoke(tankerMerger, "res/", set);
        String warning = String.format("Warning: string 'search' will be removed from file %s\n",
                file.getAbsolutePath());
        assertThat(outputStream.toString()).isEqualTo(warning);
        removeDirectory("res/");
    }

    private void removeDirectory(String directoryPath) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        File file = new File(directoryPath);
        if (!file.exists()) {
            return;
        }
        Assert.assertTrue(file.isDirectory());
        FileUtils.deleteDirectory(file);
    }

    @Test
    public void testException() throws IOException {
        removeDirectory("res/");
        boolean exception = false;
        try {
            TankerMerger tankerMerger = new TankerMerger("en", "ru", "strings.xml", false);
            tankerMerger.write("res/", false);
        } catch (AssertionError error) {
            exception = true;
        }
        assertThat(exception).isTrue();
    }

    @Test
    public void testMergerWithNewLine() throws IOException {
        testMergerWithInput("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <!-- Widget block -->\n" +
                "    <string name=\"widget\">widget</string>\n" +
                "    <string name=\"search\">123</string>\n" +
                "    <string name=\"cdata\"><![CDATA[Включить сбор статистики по правилам <a href=\"%1$s\">Лицензионного соглашения</a> и <a href=\"%2$s\">Политики конфиденциальности Яндекса</a>]]></string>\n" +
                "</resources>\n");
    }

    @Test
    public void testMergerWithoutNewLine() throws IOException {
        testMergerWithInput("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <!-- Widget block -->\n" +
                "    <string name=\"widget\">widget</string>\n" +
                "    <string name=\"search\">123</string>\n" +
                "    <string name=\"cdata\"><![CDATA[Включить сбор статистики по правилам <a href=\"%1$s\">Лицензионного соглашения</a> и <a href=\"%2$s\">Политики конфиденциальности Яндекса</a>]]></string>\n" +
                "</resources>");
    }

    private void testMergerWithInput(String inputXmlString) throws IOException {
        removeDirectory("res/");
        //noinspection ResultOfMethodCallIgnored
        new File("res/values").mkdirs();
        File file = new File("res/values/strings.xml");
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write(inputXmlString);
        writer.flush();
        writer.close();
        TankerMerger tankerMerger = new TankerMerger("en", "ru", "strings.xml", false);
        Map<String, Value> map = new HashMap<>();
        map.put("widget", new StringValue("виджет"));
        map.put("search", new StringValue("пошук"));
        map.put("cdata", new StringValue("<![CDATA[Включить сбор статистики по правилам <a href=\"%1$s\">Лицензионного соглашения</a> и <a href=\"%2$s\">Политики конфиденциальности Яндекса</a>]]>"));
        tankerMerger.add("be", map.entrySet(), "test");
        map.clear();
        map.put("widget", new StringValue("виджет"));
        map.put("search", new StringValue("поиск"));
        map.put("cdata", new StringValue("<![CDATA[Включить сбор статистики по правилам <a href=\"%1$s\">Лицензионного соглашения</a> и <a href=\"%2$s\">Политики конфиденциальности Яндекса</a>]]>"));
        tankerMerger.add("ru", map.entrySet(), "test");
        map.clear();
        map.put("widget", new StringValue("widget"));
        map.put("search", new StringValue("search"));
        map.put("cdata", new StringValue("<![CDATA[Включить сбор статистики по правилам <a href=\"%1$s\">Лицензионного соглашения</a> и <a href=\"%2$s\">Политики конфиденциальности Яндекса</a>]]>"));
        tankerMerger.add("en", map.entrySet(), "test");
        tankerMerger.write("res", false);

        String en = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <!-- Widget block -->\n" +
                "    <string name=\"widget\">widget</string>\n" +
                "    <string name=\"search\">search</string>\n" +
                "    <string name=\"cdata\"><![CDATA[Включить сбор статистики по правилам <a href=\"%1$s\">Лицензионного соглашения</a> и <a href=\"%2$s\">Политики конфиденциальности Яндекса</a>]]></string>\n" +
                "</resources>\n";

        String ru = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <!-- Widget block -->\n" +
                "    <string name=\"widget\">виджет</string>\n" +
                "    <string name=\"search\">поиск</string>\n" +
                "    <string name=\"cdata\"><![CDATA[Включить сбор статистики по правилам <a href=\"%1$s\">Лицензионного соглашения</a> и <a href=\"%2$s\">Политики конфиденциальности Яндекса</a>]]></string>\n" +
                "</resources>\n";

        String be = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <!-- Widget block -->\n" +
                "    <string name=\"widget\">виджет</string>\n" +
                "    <string name=\"search\">пошук</string>\n" +
                "    <string name=\"cdata\"><![CDATA[Включить сбор статистики по правилам <a href=\"%1$s\">Лицензионного соглашения</a> и <a href=\"%2$s\">Политики конфиденциальности Яндекса</a>]]></string>\n" +
                "</resources>\n";

        Assert.assertEquals(en, getFileContent("res/values/strings.xml"));
        Assert.assertEquals(ru, getFileContent("res/values-ru/strings.xml"));
        Assert.assertEquals(be, getFileContent("res/values-be/strings.xml"));
        removeDirectory("res/");
    }

    private String getFileContent(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
}
