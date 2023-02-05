package com.yandex.bitbucket.plugin.utils;

import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.content.NoSuchPathException;
import com.atlassian.bitbucket.i18n.KeyedMessage;
import com.atlassian.bitbucket.io.TypeAwareOutputSupplier;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import ru.yandex.bitbucket.plugin.configprocessor.util.FileReader;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class FileReaderImplTest {
    @Mock
    private ContentService contentService;

    @Mock
    private PullRequest pullRequest;

    @Mock
    private Repository repository;

    @Mock
    private PullRequestRef ref;

    private FileReader fileReader;

    private static final String PATH = "test_path/test_file.xml";
    private static final String FAKE_PATH = "fake";
    private static final long ID = 1;
    private static final String BRANCH = ConstantUtils.makeMergeBranch(ID);
    private static final String RESULT = "result";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(pullRequest.getId()).thenReturn(ID);
        when(pullRequest.getToRef()).thenReturn(ref);
        when(ref.getRepository()).thenReturn(repository);
        fileReader = new FileReaderImpl(contentService, pullRequest);

        doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            OutputStream outputStream = ((TypeAwareOutputSupplier)args[3]).getStream("");
            outputStream.write(RESULT.getBytes(StandardCharsets.UTF_8));
            outputStream.close();
            return null;
        }).when(contentService).streamFile(eq(repository), eq(BRANCH), eq(PATH), any(TypeAwareOutputSupplier.class));

        doThrow(new NoSuchPathException(new KeyedMessage("", "", ""), FAKE_PATH, ""))
                .when(contentService).streamFile(eq(repository), eq(BRANCH), eq(FAKE_PATH), any(TypeAwareOutputSupplier.class));
    }

    @Test
    public void readFile() throws FileNotFoundException {
        String result = fileReader.readFile(PATH);
        assertEquals(RESULT, result);
    }

    @Test(expected = FileNotFoundException.class)
    public void readNonExistedFile() throws FileNotFoundException {
        fileReader.readFile(FAKE_PATH);
    }
}
