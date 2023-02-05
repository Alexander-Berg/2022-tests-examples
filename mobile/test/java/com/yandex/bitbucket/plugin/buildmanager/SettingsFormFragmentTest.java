package com.yandex.bitbucket.plugin.buildmanager;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildManagerPluginSettings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static com.yandex.bitbucket.plugin.buildmanager.SettingsFormFragment.MODULE;
import static com.yandex.bitbucket.plugin.buildmanager.SettingsFormFragment.TEMPLATE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SettingsFormFragmentTest {
    private static final String REPOSITORY_KEY = "repository";

    @Mock
    private BuildManagerPluginSettings mockedSettings;

    @Mock
    private SoyTemplateRenderer mockedSoyTemplateRenderer;

    @Mock
    private Appendable mockedAppendable;

    @Mock
    private Map<String, Object> mockedContext;

    @Mock
    private Repository mockedRepository;

    private SettingsFormFragment settingsFormFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockedContext.get(REPOSITORY_KEY)).thenReturn(mockedRepository);
        settingsFormFragment = new SettingsFormFragment(mockedSettings, mockedSoyTemplateRenderer);
    }

    private void testDoViewWithChecked(boolean checked) {
        when(mockedSettings.isEnabled(same(mockedRepository))).thenReturn(checked);

        settingsFormFragment.doView(mockedAppendable, mockedContext);

        InOrder inOrder = inOrder(mockedContext, mockedSoyTemplateRenderer);
        inOrder.verify(mockedContext).get(REPOSITORY_KEY);
        inOrder.verify(mockedContext).put("checked", checked);
        inOrder.verify(mockedSoyTemplateRenderer).render(same(mockedAppendable), eq(MODULE), eq(TEMPLATE),
                same(mockedContext));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testDoViewWithCheckedTrue() {
        testDoViewWithChecked(true);
    }

    @Test
    public void testDoViewWithCheckedFalse() {
        testDoViewWithChecked(false);
    }

    @Test
    public void testDoError() {
        settingsFormFragment.doError(mockedAppendable, mock(Map.class), mock(Map.class), mockedContext);

        InOrder inOrder = inOrder(mockedContext, mockedSoyTemplateRenderer);
        inOrder.verify(mockedContext).put("checked", String.valueOf(false));
        inOrder.verify(mockedSoyTemplateRenderer).render(same(mockedAppendable), eq(MODULE), eq(TEMPLATE),
                same(mockedContext));
        inOrder.verifyNoMoreInteractions();
    }

    private void testExecuteWithChecked(boolean checked) {
        Map<String, String[]> mockedRequestParams = mock(Map.class);
        when(mockedRequestParams.get("prEventListenerCheckbox")).thenReturn(checked ? new String[0] : null);

        settingsFormFragment.execute(mockedRequestParams, mockedContext);

        InOrder inOrder = inOrder(mockedContext, mockedRequestParams, mockedSettings);
        inOrder.verify(mockedContext).get(REPOSITORY_KEY);
        inOrder.verify(mockedRequestParams).get("prEventListenerCheckbox");
        inOrder.verify(mockedSettings).setEnabled(eq(mockedRepository), eq(checked));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecuteWithCheckedTrue() {
        testExecuteWithChecked(true);
    }

    @Test
    public void testExecuteWithCheckedFalse() {
        testExecuteWithChecked(false);
    }
}
