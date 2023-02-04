package ru.yandex.realty.dictionary;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.mockito.Mockito;
import ru.yandex.extdata.core.lego.Provider;
import ru.yandex.realty.context.ProviderAdapter;
import ru.yandex.realty.storage.verba.VerbaAlias;
import ru.yandex.realty.storage.verba.VerbaStorage;
import ru.yandex.verba2.model.Term;

import java.util.*;

/**
 * User: daedra
 * Date: 27.02.14
 * Time: 19:13
 */
public class StringIndexDictionaryTest extends TestCase {
    private Map<String, Set<Term>> containsMap = new HashMap<>();
    private StringIndexDictionary stringIndexDictionary;

    public void testStringIndexDictionary() {
        Term t1 = new Term(1, "1", "t1", 1, "test", DateTime.now(), DateTime.now());
        Term t2 = new Term(2, "2", "t2", 1, "test", DateTime.now(), DateTime.now());
        Term t3 = new Term(3, "3", "t3", 1, "test", DateTime.now(), DateTime.now());
        Term t4 = new Term(4, "4", "t4", 1, "test", DateTime.now(), DateTime.now());
        containsMap.put("test 1", new HashSet<>(Arrays.asList(t1)));
        containsMap.put("1", new HashSet<>(Arrays.asList(t2, t4)));
        containsMap.put("1 test", new HashSet<>(Arrays.asList(t3)));

        String description = "test 1 test";
        StringIndexContainer sic = stringIndexDictionary.getValuesByField(VerbaAlias.SITES);

        final int[] indexedDescription = DescriptionIndexer.indexString(sic.getDic(), description);
        Set<Set<String>> entry = sic.getTrie().find(indexedDescription);

        Set<String> result = new HashSet<>();
        for (Set<String> e : entry) {
            result.addAll(e);
        }
        Assert.assertTrue(result.contains("1") && result.contains("2") && result.contains("3"));
    }

    public void testQuotes() {
        Term t1 = new Term(1, "1", "test", 1, "test", DateTime.now(), DateTime.now());
        containsMap.put("test", new HashSet<>(Arrays.asList(t1)));

        String description = "aaa \"“ test”\"";
        StringIndexContainer sic = stringIndexDictionary.getValuesByField(VerbaAlias.SITES);

        final int[] indexedDescription = DescriptionIndexer.indexString(sic.getDic(), description);
        Set<Set<String>> entry = sic.getTrie().find(indexedDescription);

        Set<String> result = new HashSet<>();
        for (Set<String> e : entry) {
            result.addAll(e);
        }
        Assert.assertTrue(result.contains("1"));
    }


    @Override
    public void setUp() throws Exception {
        VerbaStorage verbaStorage = Mockito.mock(VerbaStorage.class);
        Provider<VerbaStorage> verbaProvider = ProviderAdapter.create(verbaStorage);

        Mockito.when(verbaStorage.getAliasTerms(Mockito.any(VerbaAlias.class))).thenReturn(containsMap);
        stringIndexDictionary = new StringIndexDictionary();
        stringIndexDictionary.setVerbaProvider(verbaProvider);
    }
}
