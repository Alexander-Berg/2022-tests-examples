package ru.yandex.solomon.alert.api.validators;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

/**
 * @author Oleg Baryshnikov
 */
public class IdValidatorTest {

    @Test(expected = ValidationException.class)
    public void idWithForbiddenSymbolsNotValid() {
        IdValidator.ensureValid("<span>id with {forbidden} symbols</span>", "", true);
    }

    @Test(expected = ValidationException.class)
    public void tooLongIdNotValid() {
        IdValidator.ensureValid(StringUtils.repeat('a', 65), "", true);
    }

    @Test(expected = ValidationException.class)
    public void slashDisabled() {
        IdValidator.ensureValid("0c7193fb-c914-4980-a62a-5199e136b1bb/global/global/1", "", true);
    }

    @Test
    public void dotsAllowed() {
        IdValidator.ensureValid("yc.ydb.slo_1h_total_slo_yc", "", true);
    }
}
