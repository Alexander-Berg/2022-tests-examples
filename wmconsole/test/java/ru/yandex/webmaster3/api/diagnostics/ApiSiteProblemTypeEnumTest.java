package ru.yandex.webmaster3.api.diagnostics;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.webmaster3.api.diagnostics.data.ApiSiteProblemTypeEnum;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum;

/**
 * @author avhaliullin
 */
public class ApiSiteProblemTypeEnumTest {
    @Test
    public void allEnabledCoreProblemsShouldBeMapped() {
        Set<ApiSiteProblemTypeEnum> metProblems = EnumSet.noneOf(ApiSiteProblemTypeEnum.class);
        for (SiteProblemTypeEnum coreProblem : SiteProblemTypeEnum.ENABLED_PROBLEMS) {
            ApiSiteProblemTypeEnum apiProblem = ApiSiteProblemTypeEnum.fromCoreType(coreProblem);
            Assert.assertNotNull("Problem " + coreProblem + " should be mapped on API problem", apiProblem);
            Assert.assertTrue("Problem " + apiProblem + " should be uniquely mapped from core", metProblems.add(apiProblem));
        }
    }

    @Test
    @Ignore
    public void apiProblemsShouldNotMapOnDisabledProblems() {
        for (ApiSiteProblemTypeEnum problemType : ApiSiteProblemTypeEnum.values()) {
            Assert.assertFalse("API problems should be mapped only on enabled core problems. Problem: " + problemType, problemType.getCoreType().isDisabled());
        }
    }
}
