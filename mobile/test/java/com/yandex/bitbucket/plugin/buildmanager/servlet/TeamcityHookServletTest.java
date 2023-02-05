package com.yandex.bitbucket.plugin.buildmanager.servlet;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildResultCacheKey;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildResult;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildStatus;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO: extends AbstractServletTest
public class TeamcityHookServletTest {
    private static final long BUILD_ID = 1;
    private static final TeamcityBuildStatus BUILD_STATUS = TeamcityBuildStatus.FAILURE;
    private static final String STATUS_MESSAGE = "build failed";
    private static final String BUILD_TYPE_ID = "tc://one";
    private static final String MERGE_HASH = "merge_hash";
    private static final String PROJECT_KEY = "mobile";
    private static final long PR_ID = 1;
    private static final String REPOSITORY_SLUG = "monorepo";
    private static final int REPO_ID = 1;

    private static final String JSON =
            " {\"build\": {" +
                    "    \"buildStatus\": \"" + STATUS_MESSAGE + "\"," +
                    "    \"buildResult\": \"" + BUILD_STATUS.toString().toLowerCase() + "\"," +
                    "    \"buildId\": \"" + BUILD_ID + "\"," +
                    "    \"buildTypeId\": \"" + BUILD_TYPE_ID + "\"," +
                    "    \"teamcityProperties\": [" +
                    "      {\"name\": \"teamcity.build.branch\", \"value\": \"" + PR_ID + "/merge\"}," +
                    "      {\"name\": \"vcsroot.url\", \"value\": " +
                    "\"ssh://git@bb.yandex-team.ru/" + PROJECT_KEY + "/" + REPOSITORY_SLUG + ".git\"}," +
                    "      {\"name\": \"build.vcs.number\", \"value\": \"" + MERGE_HASH + "\"}]}}";

    @Mock
    private RepositoryService mockedRepositoryService;

    @Mock
    private HttpServletRequest mockedReq;

    @Mock
    private HttpServletResponse mockedResp;

    @Mock
    private Repository mockedRepository;

    private final CacheManager cacheManager = new MemoryCacheManager();

    private TeamcityHookServlet servlet;

    private ServletInputStream wrapString(String s) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return inputStream.available() <= 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new RuntimeException("ServletInputStream .setReadListener called");
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockedRepository.getId()).thenReturn(REPO_ID);
        when(mockedRepositoryService.getBySlug(PROJECT_KEY, REPOSITORY_SLUG)).thenReturn(mockedRepository);
        servlet = new TeamcityHookServlet(cacheManager, mockedRepositoryService);
    }

    @Test
    public void validJson_putsToCache() throws Exception {
        when(mockedReq.getInputStream()).thenReturn(wrapString(JSON));

        servlet.doPost(mockedReq, mockedResp);

        verify(mockedResp).setStatus(HttpServletResponse.SC_NO_CONTENT);
        Cache<BuildResultCacheKey, TeamcityBuildResult> cache = ConstantUtils.getBuildResultCache(cacheManager);
        BuildResultCacheKey key = new BuildResultCacheKey(REPO_ID, PR_ID, BUILD_TYPE_ID);
        assertTrue(cache.containsKey(key));
        assertEquals(1, cache.getKeys().size());
        assertEquals(new TeamcityBuildResult(BUILD_STATUS, BUILD_ID, MERGE_HASH,
                ConstantUtils.makeFailedBuildStatusMessage(STATUS_MESSAGE)), cache.get(key));
    }

    @Test
    public void invalidJson_error() throws Exception {
        when(mockedReq.getInputStream()).thenReturn(wrapString("invalid json"));

        servlet.doPost(mockedReq, mockedResp);

        verify(mockedResp).sendError(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(ConstantUtils.getBuildResultCache(cacheManager).getKeys().isEmpty());
    }

    private void incorrectData_error(String json) {
        try {
            when(mockedReq.getInputStream()).thenReturn(wrapString(json));

            servlet.doPost(mockedReq, mockedResp);
        } catch (Exception e) {
            fail(e.toString());
        }

        verify(mockedResp).setStatus(HttpServletResponse.SC_NO_CONTENT);
        assertTrue(ConstantUtils.getBuildResultCache(cacheManager).getKeys().isEmpty());
    }

    @Test
    public void incorrectBranchSuffix_error() {
        incorrectData_error(JSON.replace(
                "{\"name\": \"teamcity.build.branch\", \"value\": \"" + PR_ID + "/merge\"}",
                "{\"name\": \"teamcity.build.branch\", \"value\": \"master\"}"
        ));
    }

    @Test
    public void incorrectBranchName_error() {
        incorrectData_error(JSON.replace(
                "{\"name\": \"teamcity.build.branch\", \"value\": \"" + PR_ID + "/merge\"}",
                "{\"name\": \"teamcity.build.branch\", \"value\": \"my_branch/merge\"}"
        ));
    }

    @Test
    public void incorrectVcsRootUrlSuffix_error() {
        incorrectData_error(JSON.replace(
                "\"ssh://git@bb.yandex-team.ru/" + PROJECT_KEY + "/" + REPOSITORY_SLUG + ".git\"}",
                "\"ssh://git@bb.yandex-team.ru/" + PROJECT_KEY + "/" + REPOSITORY_SLUG + "\"}"
        ));
    }

    @Test
    public void vcsRootUrlWithoutProjectAndRepository_error() {
        incorrectData_error(JSON.replace(
                "\"ssh://git@bb.yandex-team.ru/" + PROJECT_KEY + "/" + REPOSITORY_SLUG + ".git\"}",
                "\"ssh://git@bb.yandex-team.git\"}"
        ));
    }

    @Test
    public void incorrectProject_error() {
        incorrectData_error(JSON.replace(
                "\"ssh://git@bb.yandex-team.ru/" + PROJECT_KEY + "/" + REPOSITORY_SLUG + ".git\"}",
                "\"ssh://git@bb.yandex-team.ru/incorrect_key_" + PROJECT_KEY + "/" + REPOSITORY_SLUG + ".git\"}"
        ));
    }

    @Test
    public void incorrectRepository_error() {
        incorrectData_error(JSON.replace(
                "\"ssh://git@bb.yandex-team.ru/" + PROJECT_KEY + "/" + REPOSITORY_SLUG + ".git\"}",
                "\"ssh://git@bb.yandex-team.ru/" + PROJECT_KEY + "/incorrect_slug_" + REPOSITORY_SLUG + ".git\"}"
        ));
    }

    @Test
    public void resultFailureAndMissedBuildStatus_error() {
        incorrectData_error(JSON.replace(
                "\"buildStatus\": \"" + STATUS_MESSAGE + "\",",
                ""
        ));
    }
}
