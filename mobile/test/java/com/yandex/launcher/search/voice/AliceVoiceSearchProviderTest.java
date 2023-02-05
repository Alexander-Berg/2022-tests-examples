package com.yandex.launcher.search.voice;

import com.yandex.launcher.app.TestApplication;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 26, manifest = Config.NONE, packageName = "com.yandex.launcher", application = TestApplication.class)
@Ignore //TODO: PHONE-2835
public class AliceVoiceSearchProviderTest {
//
//    private static final String PACKAGE_NAME = "com.test.package";
//
//    @Mock
//    private PhraseSpotterController mPhraseSpotterController;
//    @Mock
//    private IPreferenceProvider mPreferenceProvider;
//    @Mock
//    private Launcher mLauncher;
//
//    @Before
//    public void setUp() throws Exception {
//        MockitoAnnotations.initMocks(this);
//        PreferencesManager.init(ApplicationProvider.getApplicationContext());
//        PreferencesManager.provider = mPreferenceProvider;
//        AliceVoiceSearchProviderForTesting.mMockController = mPhraseSpotterController;
//        CommonMetricaFacade.setImpl(new NoMetricaImpl());
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        AliceVoiceSearchProviderForTesting.mMockController = null;
//        mPhraseSpotterController = null;
//    }
//
//    //region --------------------------- Lifecycle callback tests
//
//    @Test
//    public void attachAliceProvider_controllerPrepareCalled() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onAttach();
//
//        verify(mPhraseSpotterController).prepare();
//    }
//
//    @Test
//    public void detachAliceProvider_controllerDestroyCalled() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onDetach();
//
//        verify(mPhraseSpotterController).destroy();
//    }
//
//    @Test
//    public void resumeAliceProvider_controllerStartCalled() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onResume();
//
//        verify(mPhraseSpotterController).start();
//    }
//
//    @Test
//    public void pauseAliceProvider_controllerStopCalled() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onPause();
//
//        verify(mPhraseSpotterController).stop();
//    }
//
//    @Test
//    public void onStopAliceProvider_controllerOnActivityStoppedCalled() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onStop();
//
//        verify(mPhraseSpotterController).onActivityStopped();
//    }
//
//    @Test
//    public void onStartAliceProvider_controllerOnActivityStartedCalled() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onStart();
//
//        verify(mPhraseSpotterController).onActivityStarted();
//    }
//
//    @Test
//    public void aliceProviderOnOpenApplication_voiceActivationDisabled_controllerNoInteractions() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//        setVoiceActivationEnabled(false);
//
//        provider.onOpenApplication(PACKAGE_NAME);
//
//        verifyZeroInteractions(mPhraseSpotterController);
//    }
//
//    @Test
//    public void aliceProviderOnOpenApplication_packageNameNull_controllerNoInteractions() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onOpenApplication(null);
//
//        verifyZeroInteractions(mPhraseSpotterController);
//    }
//
//    //TODO: handle only Yandex's packages that uses spotter
//    @Test
//    public void aliceProviderOnOpenApplication_packageNameNotNull_controllerOnOpenApplication() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//        setVoiceActivationEnabled(true);
//
//        provider.onOpenApplication(PACKAGE_NAME);
//
//        verify(mPhraseSpotterController).onOpenApplication(PACKAGE_NAME);
//    }
//
//    @Test
//    public void aliceProviderOnOpenApplication_voiceActivationEnabled_controllerOnOpenApplication() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//        setVoiceActivationEnabled(true);
//
//        provider.onOpenApplication(PACKAGE_NAME);
//
//        verify(mPhraseSpotterController).onOpenApplication(PACKAGE_NAME);
//    }
//
//    @Test
//    public void aliceProviderOnOpenedApplication_packageNameNull_controllerNoInteractions() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onOpenedApplication(null);
//
//        verifyZeroInteractions(mPhraseSpotterController);
//    }
//
//    @Test
//    public void aliceProviderOnOpenedApplication_packageNameNotNull_controllerOnOpenedApplication() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onOpenedApplication(PACKAGE_NAME);
//
//        verify(mPhraseSpotterController).onOpenedApplication(PACKAGE_NAME);
//    }
//
//    @Test
//    public void aliceProviderOnOpenedApplication_controllerOnOpenedApplication() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//
//        provider.onOpenedApplication(PACKAGE_NAME);
//
//        verify(mPhraseSpotterController).onOpenedApplication(PACKAGE_NAME);
//    }
//
//    //endregion
//
//    //region --------------------------- Value providing methods
//
//    @Test
//    public void aliceProviderIsSpotterEnabled_voiceActivationDisabled() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//        setVoiceActivationEnabled(false);
//
//        boolean spotterEnabled = provider.isSpotterEnabled();
//
//        Assert.assertFalse(spotterEnabled);
//    }
//
//    @Test
//    public void aliceProviderIsSpotterEnabled_voiceActivationDisabled_2() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//        setVoiceActivationEnabled(null);
//
//        boolean spotterEnabled = provider.isSpotterEnabled();
//
//        Assert.assertFalse(spotterEnabled);
//    }
//
//    @Test
//    public void aliceProviderIsSpotterEnabled_voiceActivationEnabled() {
//        AliceVoiceSearchProvider provider = new AliceVoiceSearchProviderForTesting(mLauncher);
//        setVoiceActivationEnabled(true);
//
//        boolean spotterEnabled = provider.isSpotterEnabled();
//
//        Assert.assertTrue(spotterEnabled);
//    }
//
//    //endregion
//
//    //region --------------------------- Helping methods
//
//    private void setVoiceActivationEnabled(Boolean isEnabled) {
//        when(mPreferenceProvider.getBoolean(Preference.VOICE_ACTIVATION)).thenReturn(isEnabled);
//    }
//
//    //endregion
}
