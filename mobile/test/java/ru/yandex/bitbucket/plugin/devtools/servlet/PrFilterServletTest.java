package ru.yandex.bitbucket.plugin.devtools.servlet;

import com.atlassian.bitbucket.avatar.AvatarService;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageImpl;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PrFilterServletTest {

    @Mock
    HttpServletRequest mockRequest;

    @Mock
    HttpServletResponse mockResponse;

    @Mock
    RepositoryService repositoryService;

    @Mock
    PullRequestService pullRequestService;

    @Mock
    SoyTemplateRenderer soyTemplateRenderer;

    @Mock
    AvatarService avatarService;

    @Mock
    CacheManager cacheManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBadResponses() throws IOException, ServletException {
        when(mockRequest.getPathInfo()).thenReturn("/");
        PrFilterServlet servlet = new PrFilterServlet(repositoryService, pullRequestService, soyTemplateRenderer, avatarService, cacheManager);
        servlet.doGet(mockRequest, mockResponse);
        verify(mockResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testCache() throws IOException, ServletException {
        // todo: test path and mergedOnly, too
        when(mockRequest.getPathInfo()).thenReturn("repository/projectKey/repoSlug");
//        when(mockRequest.getParameter("path")).thenReturn("mail/android");

        Cache<String, Boolean> cache = spy(new MemoryCacheManager().getCache("spy"));
        when(cacheManager.getCache(PrFilterServlet.class.getName())).then((Answer<Cache<String, Boolean>>) invocation -> cache);
        PrFilterServlet servlet = new PrFilterServlet(repositoryService, pullRequestService, soyTemplateRenderer, avatarService, cacheManager);

        Repository repository = mock(Repository.class);
        when(repositoryService.getBySlug("projectKey", "repoSlug")).thenReturn(repository);

        PageRequest pageRequest = mock(PageRequest.class);
        PullRequest pullRequest = mock(PullRequest.class, RETURNS_DEEP_STUBS);
        when(pullRequest.getFromRef().getRepository().getId()).thenReturn(1);
        when(pullRequest.getFromRef().getLatestCommit()).thenReturn("from");
        when(pullRequest.getToRef().getRepository().getId()).thenReturn(2);
        when(pullRequest.getToRef().getLatestCommit()).thenReturn("to");
        when(pullRequest.getId()).thenReturn(1L);
        Page<PullRequest> page = new PageImpl<>(pageRequest, Collections.singletonList(pullRequest), true);
        when(pullRequestService.search(any(), any())).thenReturn(page);

        InOrder inOrder = inOrder(cache);
        servlet.doGet(mockRequest, mockResponse);
        inOrder.verify(cache).get("1/from-2/to-");
        inOrder.verify(cache).put("1/from-2/to-", false);
        servlet.doGet(mockRequest, mockResponse);
        inOrder.verify(cache).get("1/from-2/to-");
        inOrder.verify(cache, never()).put(any(), any());
    }
}
