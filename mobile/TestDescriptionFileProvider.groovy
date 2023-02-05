package com.yandex.frankenstein.description

class TestDescriptionFileProvider {

    File getTestRunFile(final File reportDir) {
        return new File(getTestRunDir(reportDir), 'test_run.txt')
    }

    File getTestRunDir(final File reportDir) {
        final File dir = new File(getTestInfoDir(reportDir), 'test_run')
        dir.mkdirs()
        return dir
    }

    File getIgnoredTestInfoFile(final File reportDir) {
        return new File(getIgnoredTestInfoDir(reportDir), 'ignored_cases.txt')
    }

    File getIgnoredTestInfoDir(final File reportDir) {
        final File dir = new File(getTestInfoDir(reportDir), 'ignored_cases')
        dir.mkdirs()
        return dir
    }

    File getFailedTestInfoFile(final File reportDir) {
        return new File(getTestInfoDir(reportDir), 'failed.txt')
    }

    private File getTestInfoDir(final File reportDir) {
        final File dir = new File(reportDir, 'test_descriptions')
        dir.mkdirs()
        return dir
    }
}
