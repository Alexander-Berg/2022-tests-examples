package ru.yandex.disk.remote;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.photoslice.ItemChange;
import ru.yandex.disk.test.TestUnits;
import rx.Single;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static ru.yandex.disk.remote.RemoteRepoTestHelper.load;

public class RemoteRepoListExperimentsMethodTest extends BaseRemoteRepoMethodTest {
    private static final Collector<ItemChange> STUB = new Collector<>();

    static {
        TestUnits.setDefaultCharsetLikeOnTeamcity();
    }

    @Test
    public void shouldBuilderCorrectRequest() {
        fakeOkHttpInterceptor.addResponse(200, load("experiments_response.json"));

        Single<ExperimentsApi.Config> experiments = remoteRepo.listExperiments();
        experiments.subscribe(response -> assertTrue(response != null));

        final Request request = fakeOkHttpInterceptor.getRequest();
        assertThat(request.method(), equalTo("GET"));
        assertThat(request.url().url().getPath(), equalTo("/v1/disk/experiments"));
        assertThat(request.headers("User-agent"), not(empty()));
        assertThat(request.headers("User-agent").get(0), startsWith("ok"));
    }

    @Test
    public void shouldBuildCorrectGetRequest() {
        fakeOkHttpInterceptor.addResponse(200, load("experiments_response.json"));

        Single<ExperimentsApi.Config> experiments = remoteRepo.listExperiments();
        experiments.subscribe(response -> {
                    assertTrue(response != null);
                    List<ExperimentsApi.Experiment> items = response.getItems();
                    assertTrue(items != null);
                    assertThat(items.size(), equalTo(3));
                    {
                        ExperimentsApi.Experiment experiment = items.get(0);
                        assertThat(experiment.getHandler(), equalTo("DISK"));
                        ExperimentsApi.Context context = experiment.getContext();
                        assertTrue(context != null);
                        ExperimentsApi.DiskContext diskContext = context.getDiskContext();
                        assertTrue(diskContext != null);
                        assertTrue(context.getAllContext() == null);
                        assertThat(diskContext.getFlags(), equalTo(Collections.singletonList("new_tuning_btn_pro")));
                        assertThat(diskContext.getTestIds(), equalTo(Collections.singletonList("77859")));
                    }
                    {
                        ExperimentsApi.Experiment experiment = items.get(1);
                        assertThat(experiment.getHandler(), equalTo("ALL"));
                        ExperimentsApi.Context context = experiment.getContext();
                        assertTrue(context != null);
                        ExperimentsApi.AllContext allContext = context.getAllContext();
                        assertTrue(context.getDiskContext() == null);
                        assertTrue(allContext != null);
                        assertTrue(allContext.getFlags() == null);
                        assertThat(allContext.getTestid(), equalTo(Arrays.asList("76859", "76858", "76857")));
                    }
                    {
                        ExperimentsApi.Experiment experiment = items.get(2);
                        assertThat(experiment.getHandler(), equalTo("DISK"));
                        ExperimentsApi.Context context = experiment.getContext();
                        assertTrue(context != null);
                        ExperimentsApi.DiskContext diskContext = context.getDiskContext();
                        assertTrue(diskContext != null);
                        assertTrue(context.getAllContext() == null);
                        assertThat(diskContext.getFlags(), equalTo(Arrays.asList("exp1", "exp2", "exp3")));
                        assertThat(diskContext.getTestIds(), equalTo(Collections.singletonList("10001")));
                    }
                }
        );

        final Request request = fakeOkHttpInterceptor.getRequest();
        assertThat(request.url().url().getPath(), equalTo("/v1/disk/experiments"));
    }
}

