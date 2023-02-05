package com.yandex.bitbucket.plugin.buildmanager.servlet;

import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public abstract class AbstractServletTest {
    protected static final int REPO_ID = Integer.MAX_VALUE;
    protected static final long PR_ID = Long.MAX_VALUE;
    protected static final String MERGE_HASH = "mergeHash";
    protected static final String PROJECT_KEY = "projectKey";
    protected static final String REPO_SLUG = "repoSlug";

    @Mock
    protected HttpServletRequest mockedReq;

    @Spy
    protected AbstractBaseHttpServletResponse mockedBaseResp;

    @Mock
    protected Repository mockedRepository;

    @Mock
    protected RepositoryService mockedRepositoryService;

    protected void assertErrorIfParameterMissed(String parameter) throws Exception {
        when(mockedReq.getParameter(parameter)).thenReturn(null);

        handleRequest();

        verify(mockedReq).getParameter(parameter);
        checkNoSideEffects();
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, mockedBaseResp.getStatus());
    }

    protected void assertErrorIfPullRequestDoesNotExist(PullRequestService mockedPullRequestService) throws Exception {
        when(mockedPullRequestService.getById(REPO_ID, PR_ID)).thenReturn(null);

        handleRequest();

        checkNoSideEffects();
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, mockedBaseResp.getStatus());
    }

    @Before
    public void baseSetUp() {
        initMocks(this);
        when(mockedRepository.getId()).thenReturn(REPO_ID);
        when(mockedRepositoryService.getBySlug(PROJECT_KEY, REPO_SLUG)).thenReturn(mockedRepository);
        when(mockedReq.getPathInfo()).thenReturn(String.format("/%s/%s", PROJECT_KEY, REPO_SLUG));
        when(mockedReq.getParameter("pullRequestId")).thenReturn(String.valueOf(PR_ID));
    }

    @Test
    public void emptyPathInfo_error() throws Exception {
        when(mockedReq.getPathInfo()).thenReturn("");

        handleRequest();

        verify(mockedReq, atLeastOnce()).getPathInfo();
        checkNoSideEffects();
        assertEquals(HttpServletResponse.SC_NOT_FOUND, mockedBaseResp.getStatus());
    }

    @Test
    public void missedProjectKey_error() throws Exception {
        when(mockedReq.getPathInfo()).thenReturn(String.format("/%s", REPO_SLUG));

        handleRequest();

        verify(mockedReq, atLeastOnce()).getPathInfo();
        checkNoSideEffects();
        assertEquals(HttpServletResponse.SC_NOT_FOUND, mockedBaseResp.getStatus());
    }

    @Test
    public void missedRepoSlug_error() throws Exception {
        when(mockedReq.getPathInfo()).thenReturn(String.format("/%s", PROJECT_KEY));

        handleRequest();

        verify(mockedReq, atLeastOnce()).getPathInfo();
        checkNoSideEffects();
        assertEquals(HttpServletResponse.SC_NOT_FOUND, mockedBaseResp.getStatus());
    }


    protected abstract void handleRequest() throws Exception;

    protected abstract void checkNoSideEffects();

    protected static abstract class AbstractBaseHttpServletResponse implements HttpServletResponse {
        private static final int DEFAULT_STATUS = 0;

        int status = DEFAULT_STATUS;

        @Override
        public void sendError(int code) throws IOException {
            status = code;
        }

        @Override
        public void setStatus(int code) {
            status = code;
        }

        @Override
        public int getStatus() {
            return status;
        }
    }
}
