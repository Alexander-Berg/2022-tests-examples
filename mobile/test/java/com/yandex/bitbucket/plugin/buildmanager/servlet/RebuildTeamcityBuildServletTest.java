package com.yandex.bitbucket.plugin.buildmanager.servlet;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import com.yandex.bitbucket.plugin.utils.api.TeamcityApiHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RebuildTeamcityBuildServletTest extends AbstractServletTest {
    private static final String BUILD_TYPE_ID = "buildTypeId";

    @Mock
    private Commit mockedCommit;

    @Mock
    private CommitService mockedCommitService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PullRequest mockedPullRequest;

    @Mock
    private PullRequestService mockedPullRequestService;

    @Mock
    private TeamcityApiHelper mockedTeamcityApiHelper;

    private RebuildTeamcityBuildServlet rebuildTeamcityBuildServlet;

    @Override
    protected void handleRequest() throws Exception {
        rebuildTeamcityBuildServlet.doPost(mockedReq, mockedBaseResp);
    }

    @Override
    protected void checkNoSideEffects() {
        verify(mockedTeamcityApiHelper, never()).startTeamcityBuild(eq(ConstantUtils.makeShortMergeBranch(PR_ID)),
                eq(BUILD_TYPE_ID), same(mockedPullRequest), eq(MERGE_HASH));
    }

    @Before
    public void setUp() {
        when(mockedReq.getParameter("buildTypeId")).thenReturn(BUILD_TYPE_ID);
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

        rebuildTeamcityBuildServlet = new RebuildTeamcityBuildServlet(mockedCommitService, mockedPullRequestService,
                mockedRepositoryService, mockedTeamcityApiHelper);
    }

    @Test
    public void missedPullRequestId_error() throws Exception {
        assertErrorIfParameterMissed("pullRequestId");
    }

    @Test
    public void missedBuildTypeId_error() throws Exception {
        assertErrorIfParameterMissed("buildTypeId");
    }

    @Test
    public void pullRequestDoesNotExist_error() throws Exception {
        assertErrorIfPullRequestDoesNotExist(mockedPullRequestService);
    }

    @Test
    public void validParameters_startsBuild() throws Exception {
        handleRequest();

        verify(mockedReq).getParameter("buildTypeId");
        verify(mockedPullRequestService).getById(REPO_ID, PR_ID);
        verify(mockedTeamcityApiHelper).startTeamcityBuild(eq(ConstantUtils.makeShortMergeBranch(PR_ID)),
                eq(BUILD_TYPE_ID), same(mockedPullRequest), eq(MERGE_HASH));
        assertEquals(HttpServletResponse.SC_NO_CONTENT, mockedBaseResp.getStatus());
    }
}
