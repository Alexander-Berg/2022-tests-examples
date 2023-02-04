package ru.yandex.partner.core.entity.user.type.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.entity.user.model.CommonUser;
import ru.yandex.partner.core.entity.user.model.User;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonUserValidatorProviderTest {

    private final CommonUserValidatorProvider commonUserValidatorProvider = new CommonUserValidatorProvider();
    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new User()
                .withHasRsya(true)
                .withEmail("test-email@yandex.ru");
    }

    @Test
    void validateEmail() {
        ValidationResult<CommonUser, Defect> validationResult = commonUserValidatorProvider
                .validator()
                .apply(
                        validUser.withEmail("test-email-1@yandex.ru, test-email-2@atorus.ru, test-email-2@atorus.ru")
                );

        assertFalse(validationResult.hasAnyErrors());

        validationResult = commonUserValidatorProvider
                .validator()
                .apply(
                        validUser.withEmail("test-email-1/yandex.ru")
                );

        assertTrue(validationResult.hasAnyErrors());

    }
}
