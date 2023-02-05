package com.yandex.frankenstein.results

import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class TasksDurationParserTest {

    final File tempDir = File.createTempDir()
    final Logger dummyLogger = [:] as Logger
    final TaskDurationFileProvider mTaskDurationFileProvider = new TaskDurationFileProvider(tempDir)

    @Test
    void testParseDurations() {

        final File durationsFile = mTaskDurationFileProvider.getTasksDurationFile()
        durationsFile << "string1 : 1"
        TasksDurationParser tasksDurationParser = new TasksDurationParser(dummyLogger, mTaskDurationFileProvider)
        Map<String, Long> durations = tasksDurationParser.parseDurations()
        assertThat(durations).isEqualTo(["string1": 1L])
    }

    @Test
    void testParseDurationsWithEmptyFile() {
        TasksDurationParser tasksDurationParser = new TasksDurationParser(dummyLogger, mTaskDurationFileProvider)
        Map<String, Long> durations = tasksDurationParser.parseDurations()
        assertThat(durations).isEmpty()
    }

    @Test
    void testCallTasksDurationFileProvider() {
        final TaskDurationFileProvider taskDurationFileProvider = new TaskDurationFileProvider(tempDir) {
            String getFileName() {
                return "different_file.txt"
            }
        }
        final File durationsFile = taskDurationFileProvider.getTasksDurationFile()
        durationsFile << "string1 : 1"
        TasksDurationParser tasksDurationParser = new TasksDurationParser(dummyLogger, taskDurationFileProvider)
        Map<String, Long> durations = tasksDurationParser.parseDurations()
        assertThat(durations).isEqualTo(["string1": 1L])
    }

    @Test
    void testParseWrongData() {
        final File durationsFile = mTaskDurationFileProvider.getTasksDurationFile()
        durationsFile << "string1 : 1\nstring2 : 2 : 3"
        TasksDurationParser tasksDurationParser = new TasksDurationParser(dummyLogger, mTaskDurationFileProvider)
        Map<String, Long> durations = tasksDurationParser.parseDurations()
        assertThat(durations).isEqualTo(["string1": 1L])
    }

    @Test
    void testParseDurationsWithoutFile() {
        final TaskDurationFileProvider taskDurationFileProvider = new TaskDurationFileProvider(tempDir) {
            File getTasksDurationFile() {
                return new File(fileName)
            }
        }
        assertThat(taskDurationFileProvider.getTasksDurationFile().exists()).isFalse()
        TasksDurationParser tasksDurationParser = new TasksDurationParser(dummyLogger, taskDurationFileProvider)
        Map<String, Long> durations = tasksDurationParser.parseDurations()
        assertThat(durations).isEmpty()
    }
}