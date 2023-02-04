package ru.yandex.webmaster3.api.verification.data;

import junit.framework.TestCase;
import ru.yandex.webmaster3.core.host.verification.VerificationType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * @author akhazhoyan 06/2018
 */
public class ApiExplicitVerificationTypeTest extends TestCase {

    public void testFromVerificationTypesDoesNotThrowNpe() {
        Collection<VerificationType> allVerificationTypes = Arrays.asList(VerificationType.values());
        Set<ApiExplicitVerificationType> apiExplicitVerificationTypes =
                ApiExplicitVerificationType.fromVerificationTypes(allVerificationTypes);
        assertEquals("The set must contain all the enum's instances",
                     ApiExplicitVerificationType.values().length, apiExplicitVerificationTypes.size());
    }
}