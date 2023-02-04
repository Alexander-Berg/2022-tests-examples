package ru.yandex.realty;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static ru.yandex.realty.ShortDescriptionBuilder.*;

/**
 * Created by Natalia Staroverova <nstaroverova@yandex-team.ru> on 24.05.18
 */
public class OfferDocumentBuilderBaseParseDescriptionTest {

    private static final String RESULT = "ПРЯМАЯ ПРОДАЖА. ПАРАМЕТРЫ КВАРТИРЫ - трехкомнатная в панельном доме на 7(+12) 'этаже', «» общей \"площадью\" 53 кв.м. Год постройки 1975. В КВАРТИРЕ сделан (ёЁ, йЙ)";
    private static final String STRING_WITH_HTML = "<h1>ПРЯМАЯ ПРОДАЖА.</h1> &lt;h2&gt; ПАРАМЕТРЫ КВАРТИРЫ </h2> - трехкомнатная в панельном доме на 7(+12) 'этаже', «» общей \"площадью\" 53 кв.м. Год постройки 1975. В КВАРТИРЕ сделан (ёЁ, йЙ)";
    private static final String STRING_WITH_SPECIAL_SYMBOLS = "ПРЯМАЯ ПРОДАЖА. ПАРАМЕТРЫ КВАРТИРЫ - трехкомнатная в панельном доме на 7(+12) 'этаже', «» общей \"площадью\" 53  кв.м. \r\tГод постройки  \n1975. В КВАРТИРЕ сделан (ёЁ, йЙ)";
    private static final String STRING_WITH_LONG_STRANGE_LINE = "ПРЯМАЯ ПРОДАЖА. ПАРАМЕТРЫ КВАРТИРЫ - трехкомнатная в панельном доме на 7(+12) 'этаже'-----------------, «» общей \"площадью\" 53 кв.м. ==========Год постройки  1975. ___________В КВАРТИРЕ сделан (ёЁ, йЙ)";
    private static final String CRAZY_STRING = "ПРЯМАЯ &lt;p&gt; ПРОДАЖА. ПАРАМЕТРЫ КВАРТИРЫ - трехкомнатная в панельном ---------------доме на 7(+12) 'этаже', «»=================  общей \"площадью\" 53 кв.м. _____________Год постройки 1975. \n" +
            "В КВАРТИРЕ \n" +
            "\tсделан <p></p> <a> (ёЁ, йЙ)";

    //We do not use constructor parameters in this test
    private OfferDocumentBuilderBase builder = new OfferDocumentBuilderBase(null, null,
            null, null, null, null, null, null);

    @Test
    public void testString() {
        assertEquals(RESULT, transformDescription(STRING_WITH_HTML));
        assertEquals(RESULT, transformDescription(STRING_WITH_SPECIAL_SYMBOLS));
        assertEquals(RESULT, transformDescription(STRING_WITH_LONG_STRANGE_LINE));
        assertEquals(RESULT, transformDescription(CRAZY_STRING));
    }

}
