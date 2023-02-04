package ru.yandex.auto.core.trie;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.date.TimerUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static ru.yandex.common.util.RandomUtils.nextIntInRange;
import static ru.yandex.common.util.collections.CollectionFactory.newArrayList;

/**
 * @author Sergey Kopeliovich (burunduk1@yandex-team.ru)
 */
public class TrieTest {
    private final static Logger log = Logger.getLogger(TrieTest.class);
    private final static long FIXED_SEED = 239017;
    private final static Random randomWithFixedSeed = new Random(FIXED_SEED);

    private class TrieTester
    {
        private SubstringsFinder<String> indTrie = null;
        private SubstringsFinder<String> indNaive = null;

        public TrieTester(List<Pair<int[], String>> input) {
            indTrie = new NaiveSubstringsFinder<String>(input);
            indNaive = new Trie<String>(input);
        }

        public void processTexts(List<int[]> texts) {
            for (int[] t : texts) {
                String resTrie = indTrie.find(t);
                String resNaive = indNaive.find(t);
                Assert.assertTrue("<found> vs <not found>: " + Arrays.toString(t), ((resTrie == null) == (resNaive == null)));
                if (resTrie != null) {
                    //Assert.assertTrue(resTrie + " vs " + resNaive + ": " + t, resTrie.compareTo(resNaive) == 0);
                }
            }
            //log.debug("Number of tests with not null result = " + count + " of " + texts.size());
        }
    }

    @Test
    public void test1() {
        List<Pair<int[], String>> input = Arrays.asList(
                new Pair<int[], String>(new int[] {1, 0, 1}, "First"),
                new Pair<int[], String>(new int[] {0, 0}, "Second")
        );
        List<int[]> texts = Arrays.asList(
                new int[] {1, 0, 1},
                new int[] {0, 0},
                new int[] {1, 1, 1, 0},
                new int[] {0, 1, 1, 1},
                new int[] {0, 0, 1, 1, 1},
                new int[] {1, 1, 1, 0, 0},
                new int[] {1, 1, 0, 1, 1}
        );

        TrieTester tester = new TrieTester(input);
        tester.processTexts(texts);
    }

    private void randomTest( final int alphabetSize, final int numberOfTexts,
                             final int minimalLength, final int maximalLength ) {
        List<Pair<int[], String>> input = newArrayList();
        List<int[]> texts = newArrayList();

        int numberOfStrings = nextIntInRange(randomWithFixedSeed, 3, 10);
        int[] alphabet = new int[alphabetSize];
        for (int j = 0; j < alphabetSize; j++) {
            alphabet[j] = randomWithFixedSeed.nextInt();
        }
        for (int i = 0; i < numberOfStrings; i++) {
            int length = nextIntInRange(randomWithFixedSeed, minimalLength, maximalLength);
            int[] string = new int[length];
            for (int j = 0; j < length; j++)
                string[j] = alphabet[randomWithFixedSeed.nextInt(alphabetSize)];

            String data = "";
            for (int j = 0; j < 5; j++) {
                data += (char)(nextIntInRange(randomWithFixedSeed, 'a', 'z'));
            }

            input.add(Pair.of(string, data));
        }

        for (int i = 0; i < numberOfTexts; i++) {
            for (int length = 10; length <= 100; length *= 3) {
                int[] text = new int[length];
                for (int j = 0; j < length; j++) {
                    text[j] = alphabet[randomWithFixedSeed.nextInt(alphabetSize)];
                }
                texts.add(text);
            }
        }

        TrieTester tester = new TrieTester(input);
        tester.processTexts(texts);
    }

    @Test
    public void test2() {
        randomTest(2, 3, 1, 2);
        for (int t = 0; t < 20; t++) {
            randomTest(2, 10, 3, 8);
            randomTest(2, 10, 5, 10);
        }
        for (int t = 0; t < 10; t++) {
            randomTest(3, 10, 3, 6);
        }
    }

    /**
     * Maximal test. Input : text's length = 10^6, string's length = 30.
     */
//    @Test
    public void test3() {
        long start;
        
        int n = 30;
        int[] str = new int[n];
        Arrays.fill(str, 0);
        List<Pair<int[], String>> input = Arrays.asList(Pair.of(str, "Data"));

        Trie<String> ind = new Trie<String>(input);
        String res;

        int m = (int)1e6;
        int[] text = new int[m];
        Arrays.fill(text, 0);
        for (int i = m - n - 1; i >= 0; i -= n) {
            text[i] = 1;
        }

        start = System.currentTimeMillis();
        res = ind.find(text);
        log.debug("Time = " + TimerUtils.pastMillisWithMetric(start));
        Assert.assertTrue(res != null);
        log.debug(res);

        text[m - 1] = 1;
        start = System.currentTimeMillis();
        res = ind.find(text);
        log.debug("Time = " + TimerUtils.pastMillisWithMetric(start));
        Assert.assertTrue(res == null);
        log.debug(res);

        log.debug("Test is passed. textsTotalLength = " + ind.getStat().getTextsTotalLength() + ", verticesCount = " + ind.getStat().getVerticesCount());
    }

    /**
     * Another one maximal test. Just build big trie. Alphabet's size = 10^6, summary length = 10^6.
     */
//    @Test
    public void test4() {
        List<Pair<int[], Integer>> input = newArrayList();
        Random r = new Random();
        final int n = 1000;
        final int len = 1000;
        final int alphabetSize = (int)1e6;

        for (int i = 0; i < n; i++) {
            int[] s = new int[len];
            for (int j = 0; j < len; j++) {
                s[j] = r.nextInt(alphabetSize);
            }
            input.add(Pair.of(s, i));
        }

        new Trie<Integer>(input);
    }

    /**
     * The most naive implementation for <code>SubstringFinder</code>
     *
     * @param <T> type of data, stored for strings
     */
    private class NaiveSubstringsFinder<T> implements SubstringsFinder<T> {
        List<Pair<int[], T>> mem = newArrayList();

        public NaiveSubstringsFinder(List<Pair<int[], T>> input) {
            mem = newArrayList(input);
        }

        public T find(int[] text) {
            for (Pair<int[], T> item : mem) {
                int[] string = item.getFirst();
                for (int i = 0; i + string.length <= text.length; i++) {
                    int j = 0;
                    while (j < string.length && text[i + j] == string[j]) {
                        j++;
                    }
                    if (j == string.length) {
                        return item.getSecond();
                    }
                }
            }
            return null;
        }
    }
}
