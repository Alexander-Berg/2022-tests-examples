package com.yandex.bitbucket.plugin.buildmanager.entity;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import com.yandex.bitbucket.plugin.buildmanager.event.SettingsChangedEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BuildManagerPluginSettingsTest {
    @Mock
    private ApplicationEventPublisher mockedApplicationEventPublisher;

    @Mock
    private PluginSettingsFactory mockedPluginSettingsFactory;

    @Mock
    private Repository mockedRepository;

    private BuildManagerPluginSettings settings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PluginSettings pluginSettings = new PluginSettings() {
            Map<String, Object> map = new HashMap<>();

            @Override
            public Object get(String key) {
                return map.get(key);
            }

            @Override
            public Object put(String key, Object value) {
                Object result = map.remove(key);
                if (value != null) {
                    map.put(key, value);
                }
                return result;
            }

            @Override
            public Object remove(String key) {
                return map.remove(key);
            }
        };
        when(mockedPluginSettingsFactory.createSettingsForKey(ConstantUtils.SETTINGS_NAME)).thenReturn(pluginSettings);
        when(mockedRepository.getId()).thenReturn(1);
        settings = new BuildManagerPluginSettings(mockedApplicationEventPublisher, mockedPluginSettingsFactory);
    }

    @Test
    public void notEnabledForNewRepository() {
        assertFalse(settings.isEnabled(mockedRepository));
    }

    @Test
    public void isEnabledReturnsLastSetEnabledParameter() {
        Repository mockedAnotherRepository = mock(Repository.class);
        when(mockedAnotherRepository.getId()).thenReturn(2);

        setEnabledWithAssert(mockedRepository, false);
        setEnabledWithAssert(mockedRepository, true);
        setEnabledWithAssert(mockedRepository, true);
        setEnabledWithAssert(mockedRepository, false);
        setEnabledWithAssert(mockedAnotherRepository, true);
        assertFalse(settings.isEnabled(mockedRepository));
        setEnabledWithAssert(mockedRepository, true);
        assertTrue(settings.isEnabled(mockedAnotherRepository));
    }

    private void setEnabledWithAssert(Repository repository, boolean enabled) {
        settings.setEnabled(repository, enabled);
        assertEquals(enabled, settings.isEnabled(repository));
    }

    @Test
    public void publishEventOnlyOnChanges() {
        settings.setEnabled(mockedRepository, true);
        clearInvocations(mockedApplicationEventPublisher);
        int changes = 0;

        settings.setEnabled(mockedRepository, true);
        verify(mockedApplicationEventPublisher, never()).publishEvent(any(SettingsChangedEvent.class));
        settings.setEnabled(mockedRepository, false);
        verify(mockedApplicationEventPublisher, times(++changes)).publishEvent(any(SettingsChangedEvent.class));
        settings.setEnabled(mockedRepository, true);
        verify(mockedApplicationEventPublisher, times(++changes)).publishEvent(any(SettingsChangedEvent.class));
        settings.setEnabled(mockedRepository, false);
        verify(mockedApplicationEventPublisher, times(++changes)).publishEvent(any(SettingsChangedEvent.class));
        settings.setEnabled(mockedRepository, false);
        verify(mockedApplicationEventPublisher, times(changes)).publishEvent(any(SettingsChangedEvent.class));
    }
}
