package ru.yandex.vertis.releaseissuenotifier;

import org.junit.Test;
import ru.yandex.vertis.releaseissuenotifier.bean.StarTrackTag;
import ru.yandex.vertis.releaseissuenotifier.startrack.FilterBuilder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;


public class StarTrackFilterBuilderTest {

    private FilterBuilder filterBuilder = new FilterBuilder();

    @Test
    public void shouldReturnEmptyFilter() {
        String filter = filterBuilder.getFilter();
        assertThat("should return empty string", filter, isEmptyString());
    }

    @Test
    public void shouldReturnFilterWithQueue() {
        String filter = filterBuilder.addQueue("TESTQUEUE").getFilter();
        assertThat("should return string with queue", filter, is("Queue: TESTQUEUE"));
    }

    @Test
    public void shouldReturnFilterWithFixVersion() {
        String filter = filterBuilder.addFixVersions("version_1.2.3").getFilter();
        assertThat("should return string with fix version", filter, is("\"Fix Version\": version_1.2.3"));
    }

    @Test
    public void shouldReturnFilterWithSingleTag() {
        String filter = filterBuilder.addTag(StarTrackTag.RELEASE_FORMING).getFilter();
        assertThat("should return string with tag", filter, is("Tags: release_forming"));
    }

    @Test
    public void shouldReturnFilterWithMultipleFilters() {
        String filter = filterBuilder
                .addTag(StarTrackTag.RELEASE_FORMING)
                .addFixVersions("version_1.2.3")
                .addQueue("TESTQUEUE")
                .getFilter();
        assertThat(
                "should return  string with multiple filters",
                filter,
                is("Tags: release_forming \"Fix Version\": version_1.2.3 Queue: TESTQUEUE")
        );
    }
}
