package com.yandex.launcher.search;

import com.yandex.launcher.util.SearchUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Locale;

@RunWith(JUnit4.class)
public class SearchUtilsTest {

    @Test
    public void soundformTestEnglish1() {
        String testString = "CAMERA";

        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
               soundform, "KAMIRA"
        );
    }

    @Test
    public void testPlayMarketEn() {
        final String expectedResult = "PLAI MARKIT";
        String testString = "Play Market";

        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Plai Markit";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Plai Market";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );
    }

    @Test
    public void testTele2() {
        final String expectedResult = "MOI TILI2";
        String testString = "Мой теле2";

        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Мой тели2";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Мои тили2";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Мой тили2";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Moi tele2";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Moi tile2";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Moy tele2";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );
    }

    @Test
    public void testPlayMarketRu() {
        final String expectedResult = "PLAI MARKIT";
        String testString = "Play Market";

        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Плей маркет";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Плей маркет";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Play маркит";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Плей маркит";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Play маркет";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Плей market";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );

        testString = "Плей markit";
        soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, expectedResult
        );
    }

    @Test
    public void soundformTestEnglish3() {
        String testString = "Android Pay";

        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, "ANDROID PAI"
        );
    }

    @Test
    public void soundformTestEnglish4() {
    String testString = "TDD__!@#@(#@#**((9999&&&school";

        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, "TDD9999SUL"
        );
    }

    @Test
    public void soundformTestKazakh1() {
        Locale.setDefault(new Locale("kk", "KZ"));
        String testString = "АаӘәБбВвГгҒғДдЕеЁёЖжЗзИиЙйКкҚқЛлМмНнҢңОоӨөПпРрСсТтУуҰұҮүФфХхҺһЦцЧчШшЩщЪъЫыІіЬьЭэЮюЯя";

        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, "AAAABBVVGGGGDDIIIIZZZZIIIIKKKKLLMMNNNNOOOOPPRRSSTTUUUUUUFFHHHHSSSSSSSSIIIIAAUUAA"
        );
    }

    @Test
    public void soundformTestRussian1() {
        Locale.setDefault(new Locale("ru", "RU"));
        String testString = "Play Маркет";

        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, "PLAI MARKIT"
        );
    }

    @Test
    public void lowercaseTestTurkish() {
        Locale.setDefault(new Locale("tr", "TR"));
        String testString = "ŞşĞğÇçÜüAdssdvsdgaFASFAAEFCZXCXZCFGgzdk";

        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, "SSGGKKUUADSSDVSDGAFASFAAIFKZKSKKSZKFGGZDK"
        );
    }

    @Test
    public void lowercaseTestUkrainian() {
        Locale.setDefault(new Locale("uk", "UK"));
        String testString = "АаБбВвГгҐґДдЕеЄєЖжЗзИиІіЇїЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЬьЮюЯя";
        String soundform = SearchUtils.getSoundForm(testString);
        Assert.assertEquals(
                soundform, "AABBVVGGGGDDIIIIZZZZIIIIIIIIKKLLMMNNOOPPRRSSTTUUFFHHSSSSSSSSUUAA"
        );
    }

    @Test
    public void testYusupov() {
        String testString = "юсупов";

        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "USUPOV"
        );

        testString = "usupov";

        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "USUPOV"
        );

        testString = "jusupov";

        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "USUPOV"
        );

        testString = "усупов";

        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "USUPOV"
        );
    }

    @Test
    public void youtubeRuEnTest() {
        Locale.setDefault(new Locale("ru", "RU"));
        String testString = "ютуб";

        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUB"
        );

        testString = "утуб";
        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUB"
        );

        testString = "ютьюб";
        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUB"
        );

        testString = "ютьуб";
        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUB"
        );

        testString = "утьуб";
        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUB"
        );

        testString = "ЮТУБ";
        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUB"
        );
    }

    @Test
    public void youtubeEnEnTest() {
        Locale.setDefault(new Locale("en", "EN"));
        String testString = "youtube";

        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUBI"
        );

        testString = "utub";
        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUB"
        );

        testString = "yutube";
        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUBI"
        );

        testString = "yutub";
        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUB"
        );

        testString = "jutub";
        Assert.assertEquals(
                SearchUtils.getSoundForm(testString), "UTUB"
        );
    }
}
