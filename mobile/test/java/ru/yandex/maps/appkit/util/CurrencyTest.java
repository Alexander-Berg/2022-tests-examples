package ru.yandex.maps.appkit.util;

import org.junit.Test;

import ru.yandex.yandexmaps.common.locale.Currency;

import static org.junit.Assert.assertEquals;

public class CurrencyTest {

    @Test
    public void RubTextReplaceTest() {
        String incomingString = "20 руб./ч, 40руб, 40Руб, 30 Руб.";
        String expectedString = "20 ₽/ч, 40₽, 40₽, 30 ₽";
        String resultString = Currency.replaceRubStringToSymbol(incomingString);
        assertEquals(expectedString, resultString);
    }

    @Test
    public void RTextReplaceTest() {
        String incomingString = "20 р./ч, 40р, 40Р, 30 Р.";
        String expectedString = "20 ₽/ч, 40₽, 40₽, 30 ₽";
        String resultString = Currency.replaceRubStringToSymbol(incomingString);
        assertEquals(expectedString, resultString);
    }

    @Test
    public void NothingElseReplaceTest() {
        String incomingString = "Тор. рок. рагнарёк, рубли";
        String expectedString = "Тор. рок. рагнарёк, рубли";
        String resultString = Currency.replaceRubStringToSymbol(incomingString);
        assertEquals(expectedString, resultString);
    }
}
