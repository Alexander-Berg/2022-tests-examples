package ru.yandex.intranet.d.errors;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.intranet.d.IntegrationTest;

/**
 * Errors properties test.
 *
 * @author Ruslan Kadriev <aqru@yandex-team.ru>
 */
@IntegrationTest
public class ErrorsTest {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorsTest.class);

    @Test
    public void errorPropertyConsistencyTest() {
        Locale ru = new Locale("ru");
        ResourceBundle ruErrors = ResourceBundle.getBundle("i18n/errors", ru);
        List<String> keysRu = Collections.list(ruErrors.getKeys());
        Locale en = new Locale("en");
        ResourceBundle enErrors = ResourceBundle.getBundle("i18n/errors", en);
        List<String> keysEn = Collections.list(enErrors.getKeys());

        try {
            Assertions.assertEquals(keysRu, keysEn);
        } catch (Exception e) {
            keysRu.stream()
                    .filter(s -> !keysEn.contains(s))
                    .forEach(s -> LOG.error("Not in EN: " + s));
            keysEn.stream()
                    .filter(s -> !keysRu.contains(s))
                    .forEach(s -> LOG.error("Not in RU: " + s));
            throw e;
        }
    }
}
