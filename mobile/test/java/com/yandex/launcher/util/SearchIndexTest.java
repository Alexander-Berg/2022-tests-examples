package com.yandex.launcher.util;

import android.content.ComponentName;
import android.util.JsonReader;

import com.android.launcher3.AppInfo;
import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.ProgramList;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchIndexTest extends BaseRobolectricTest {

    @Test
    public void testFuzzyContain() {
        String token = "yango";
        assertFalse(SearchIndex.fuzzyContain(token, ""));
        assertTrue(SearchIndex.fuzzyContain(token, "ya"));
        assertTrue(SearchIndex.fuzzyContain(token, "yan"));
        assertTrue(SearchIndex.fuzzyContain(token, "yn"));
        assertTrue(SearchIndex.fuzzyContain(token, "yng"));
        assertTrue(SearchIndex.fuzzyContain(token, "yung"));
        assertFalse(SearchIndex.fuzzyContain(token, "yuung"));
        assertFalse(SearchIndex.fuzzyContain(token, "yi"));
    }

    @Test
    public void testFindQ() {
        SearchIndex<AppInfo> index =  createIndex();
        List<SearchIndex.MatchResultAdvanced<AppInfo>> fuzzyResult =  index.lookupFuzzy("q");
        boolean exists = false;
        for (SearchIndex.MatchResultAdvanced<AppInfo> result : fuzzyResult) {
            if (result.obj.getTitle().toString().toLowerCase().equals("q")) {
                exists = true;
                break;
            }
        }
        assertTrue(exists);
    }

    @Test
    public void testFuzzyVsAdvansed() {
        SearchIndex<AppInfo> index =  createIndex();
        String [] testQueries = {"yng", "tin", "navi", "сбе", "пч"};
        for (String query : testQueries) {
            List<SearchIndex.MatchResultAdvanced<AppInfo>> advancedResult = index.lookupAdvanced(query);
            List<SearchIndex.MatchResultAdvanced<AppInfo>> fuzzyResult = index.lookupFuzzy(query);
            assertTrue(fuzzyResult.size() >= advancedResult.size());
            for (SearchIndex.MatchResultAdvanced<AppInfo> advanced : advancedResult) {
                boolean exists = false;
                for (SearchIndex.MatchResultAdvanced<AppInfo> fuzzy : fuzzyResult) {
                    if (fuzzy.obj.equals(advanced.obj)) {
                        exists = true;
                        break;
                    }
                }
                assertTrue(exists);
            }
        }
    }

    @Test
    public void testFuzzySearchWithQueryStarsFromDelimiter() {
        SearchIndex<AppInfo> index =  createIndex();
        String query;
        List<SearchIndex.MatchResultAdvanced<AppInfo>> fuzzyResult;

        query = "";
        fuzzyResult = index.lookupFuzzy(query);
        assertEquals(0, fuzzyResult.size());

        query = "\n";
        fuzzyResult = index.lookupFuzzy(query);
        assertEquals(0, fuzzyResult.size());

        query = " yng";
        fuzzyResult = index.lookupFuzzy(query);
        assertTrue(fuzzyResult.size() > 0);

        query = "\nсбе";
        fuzzyResult = index.lookupFuzzy(query);
        assertTrue(fuzzyResult.size() > 0);
    }

    private SearchIndex<AppInfo> createIndex() {
        SearchIndex<AppInfo> index = new SearchIndex<>(new ProgramList.Tokenizer());
        try (InputStream in = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("sampleProgramList.txt")) {
            JsonReader reader = new JsonReader(new InputStreamReader(in));
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                AppInfo info = new AppInfo();
                List<String> unigueTitles = new ArrayList<>();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    switch (key) {
                        case "component":
                            reader.beginObject();
                            String packageName = "";
                            String className = "";
                            while (reader.hasNext()) {
                                String componentKey = reader.nextName();
                                switch (componentKey) {
                                    case "packageName":
                                        packageName = reader.nextString();
                                        break;
                                    case "className":
                                        className = reader.nextString();
                                        break;
                                }
                            }
                            reader.endObject();
                            info.componentName = new ComponentName(packageName, className);
                            info.setComponentStr(info.componentName.toShortString());
                            break;
                        case "title":
                            info.title = reader.nextString();
                            break;
                        case "uniqueTitles":
                            reader.beginArray();
                            while (reader.hasNext()) {
                                unigueTitles.add(reader.nextString());
                            }
                            reader.endArray();
                            break;
                    }
                }
                reader.endObject();
                index.add(info, unigueTitles.toArray(new String[unigueTitles.size()]));
            }
            reader.endArray();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return index;
    }
}
