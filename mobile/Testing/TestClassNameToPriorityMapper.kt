package com.yandex.frankenstein.testing

interface TestClassNameToPriorityMapper {

    /**
     * Method maps test class name to its priority. Test class with higher priority will run first.
     * For better test execution it is convenient for tests with large execution time to have high priority.
     */
    fun getPriority(testClassName: String): Int
}
