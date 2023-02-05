package com.yandex.bitbucket.plugin.buildmanager.servlet;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.comment.AddCommentRequest;
import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.yandex.bitbucket.plugin.buildmanager.entity.RepoPrCompositeKey;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SkipChecksOnceServletTest extends AbstractServletTest {
    @Mock
    private AuthenticationContext mockedAuthenticationContext;

    @Mock
    private CommentService mockedCommentService;

    @Mock
    private CommitService mockedCommitService;

    @Mock
    private Commit mockedCommit;

    @Mock
    private PullRequestService mockedPullRequestService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PullRequest mockedPullRequest;

    private CacheManager cacheManager;

    private SkipChecksOnceServlet skipChecksOnceServlet;

    @Override
    protected void handleRequest() throws Exception {
        skipChecksOnceServlet.doPost(mockedReq, mockedBaseResp);
    }

    @Override
    protected void checkNoSideEffects() {
        assertTrue(ConstantUtils.getSkipChecksOnceCache(cacheManager).getKeys().isEmpty());
    }

    @Before
    public void setUp() {
        when(mockedAuthenticationContext.isAuthenticated()).thenReturn(true);

        when(mockedReq.getParameter("pullRequestId")).thenReturn(Long.toString(PR_ID));
        when(mockedReq.getMethod()).thenReturn("POST");
        when(mockedPullRequest.getToRef().getRepository()).thenReturn(mockedRepository);
        when(mockedPullRequest.getId()).thenReturn(PR_ID);
        when(mockedPullRequestService.getById(REPO_ID, PR_ID)).thenReturn(mockedPullRequest);

        when(mockedCommit.getId()).thenReturn(MERGE_HASH);
        when(mockedCommitService.getCommit(any(CommitRequest.class))).thenAnswer(invocationOnMock -> {
            CommitRequest commitRequest = invocationOnMock.getArgument(0);
            if (commitRequest.getRepository() == mockedRepository && commitRequest.getCommitId().equals(
                    ConstantUtils.makeMergeBranch(PR_ID))) {
                return mockedCommit;
            }
            return null;
        });
        cacheManager = new MemoryCacheManager();
        skipChecksOnceServlet = new SkipChecksOnceServlet(mockedAuthenticationContext, cacheManager,
                mockedCommentService, mockedCommitService, mockedPullRequestService, mockedRepositoryService);
    }

    @Test
    public void unauthorized_error() throws Exception {
        when(mockedAuthenticationContext.isAuthenticated()).thenReturn(false);

        handleRequest();

        verify(mockedAuthenticationContext).isAuthenticated();
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, mockedBaseResp.getStatus());
        checkNoSideEffects();
    }

    @Test
    public void missedPullRequestId_error() throws Exception {
        assertErrorIfParameterMissed("pullRequestId");
    }

    @Test
    public void pullRequestDoesNotExist_error() throws Exception {
        assertErrorIfPullRequestDoesNotExist(mockedPullRequestService);
    }

    @Test
    public void validParameters_updatesCache() throws Exception {
        handleRequest();

        verify(mockedReq).getParameter("pullRequestId");
        Cache<RepoPrCompositeKey, String> cache = ConstantUtils.getSkipChecksOnceCache(cacheManager);
        RepoPrCompositeKey key = new RepoPrCompositeKey(REPO_ID, PR_ID);
        assertTrue(cache.containsKey(key));
        assertEquals(MERGE_HASH, cache.get(key));
        verify(mockedCommentService).addComment(any(AddCommentRequest.class));
        assertEquals(HttpServletResponse.SC_NO_CONTENT, mockedBaseResp.getStatus());
    }
}
