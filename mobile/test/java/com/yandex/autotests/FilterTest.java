package com.yandex.autotests;

import com.yandex.autotests.runner.filter.FilterJUnit4Runner;
import com.yandex.frankenstein.annotations.TestCaseId;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(FilterJUnit4Runner.class)
public class FilterTest {

    @TestCaseId(3)
    @Test
    public void testExcludeBySuiteFilter() {
        assert false;
    }

    @TestCaseId(4)
    @Test
    public void testExcludeByHasBugsFilter() {
        assert false;
    }
}
