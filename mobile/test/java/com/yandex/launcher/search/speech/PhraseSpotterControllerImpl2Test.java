package com.yandex.launcher.search.speech;

import android.content.Context;
import android.util.Log;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import ru.yandex.speechkit.PhraseSpotter;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PhraseSpotter.class, Context.class, Log.class})
@Ignore //TODO: PHONE-2835
public class PhraseSpotterControllerImpl2Test {
//
//    public static final String TEST_PACKAGE_NAME = "com.test.pacakge";
//
//    @Mock
//    private VoiceActivationDelegate mVoiceActivationDelegate;
//    @Mock
//    private PhraseSpotterController.SpotterEnabledStateProvider mSpotterEnabledStateProvider;
//    @Mock
//    private PhraseSpotterStarter mPhraseSpotterStarter;
//    @Mock
//    private TestProvider<PhraseSpotter> mPhraseSpotterProvider;
//    @Mock
//    private TestProvider<PhraseSpotterStarter> mPhraseSpotterStarterProvider;
//    @Mock
//    private PhraseSpotterListener mSpotterListener;
//
//    private PhraseSpotter mPhraseSpotter;
//    private Context mAppContext;
//    private PhraseSpotterControllerImpl mController;
//
//    @Before
//    public void setUp() throws Exception {
//        MockitoAnnotations.initMocks(this);
//        mPhraseSpotter = PowerMockito.mock(PhraseSpotter.class);
//        mAppContext = PowerMockito.mock(Context.class);
//        when(mPhraseSpotterProvider.get()).thenReturn(mPhraseSpotter);
//        when(mPhraseSpotterStarterProvider.get()).thenReturn(mPhraseSpotterStarter);
//        PhraseSpotterControllerImplForTesting.mMockedSpotterProvider = mPhraseSpotterProvider;
//        PhraseSpotterControllerImplForTesting.mMockedSpotterStarterProvider = mPhraseSpotterStarterProvider;
//        mController = new PhraseSpotterControllerImplForTesting(mAppContext, mVoiceActivationDelegate, mSpotterEnabledStateProvider);
//
//        PowerMockito.mockStatic(Log.class, new DoNothingAnswer());
//
//        assumeThatContextAndSpotterNotNull();
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        mAppContext = null;
//        mPhraseSpotter = null;
//    }
//
//    @Test
//    public void testMockPhraseSpotter() {
//        PhraseSpotter spotter = PowerMockito.mock(PhraseSpotter.class);
//
//        spotter.prepare();
//
//        verify(spotter, times(1)).prepare();
//    }
//
//    //region --------------------------- Lifecycle callback tests
//
//    @Test
//    public void prepareController_spotterNotEnabled_spotterAndStarterNotCreated() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//
//        mController.prepare();
//
//        verifyZeroInteractions(mPhraseSpotterProvider);
//        verifyZeroInteractions(mPhraseSpotterStarterProvider);
//    }
//
//    @Test
//    public void prepareController_spotterNotEnabled_spotterNotPrepared() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//
//        mController.prepare();
//
//        verifyZeroInteractions(mPhraseSpotter);
//    }
//
//    @Test
//    public void prepareController_spotterEnabled_spotterAndStarterCreated() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.prepare();
//
//        verify(mPhraseSpotterProvider, times(1)).get();
//        verify(mPhraseSpotterStarterProvider, times(1)).get();
//    }
//
//    @Test
//    public void prepareControllerTwoTimes_spotterEnabled_spotterAndStarterCreatedOnce() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.prepare();
//        mController.prepare();
//
//        verify(mPhraseSpotterProvider, times(1)).get();
//        verify(mPhraseSpotterStarterProvider, times(1)).get();
//    }
//
//    @Test
//    public void prepareController_spotterEnabled_starterReadToStart() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.prepare();
//
//        verify(mPhraseSpotterStarter, times(1)).setReadyToStart(true);
//    }
//
//    @Test
//    public void prepareController_spotterNotEnabled_starterNotReadyToStart() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//
//        mController.prepare();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterEnabled_controllerNotPrepared_controllerStartIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.start();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterNotEnabled_controllerPrepared_controllerStartIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//        mController.prepare();
//
//        mController.start();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterEnabled_controllerPrepared_controllerStrted() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//        mController.prepare();
//
//        mController.start();
//
//        verify(mPhraseSpotterStarter, times(1)).start();
//    }
//
//    @Test
//    public void spotterEnabled_controllerNotPrepared_controllerStopIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.stop();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterNotEnabled_controllerPrepared_controllerStopIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//        mController.prepare();
//
//        mController.stop();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterEnabled_controllerPrepared_controllerStopped() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//        mController.prepare();
//
//        mController.stop();
//
//        verify(mPhraseSpotterStarter, times(1)).stop();
//    }
//
//    @Test
//    public void spotterEnabled_controllerNotPrepared_spotterDestroyIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.destroy();
//
//        verifyZeroInteractions(mPhraseSpotter);
//    }
//
//    @Test
//    public void spotterNotEnabled_controllerPrepared_spotterDestroyIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//        mController.prepare();
//
//        mController.destroy();
//
//        verifyZeroInteractions(mPhraseSpotter);
//    }
//
//    @Test
//    public void spotterEnabled_controllerPrepared_spotterDestroyed() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//        mController.prepare();
//
//        mController.destroy();
//
//        verify(mPhraseSpotter, times(1)).destroy();
//    }
//
//    @Test
//    public void spotterEnabled_controllerNotPrepared_destroyController_starterStopIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.destroy();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterNotEnabled_controllerPrepared_destroyController_starterStopIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//        mController.prepare();
//
//        mController.destroy();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterEnabled_controllerPrepared_destroyController_starterStopped() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//        mController.prepare();
//
//        mController.destroy();
//
//        verify(mPhraseSpotterStarter, times(1)).stop();
//    }
//
//    @Test
//    public void spotterEnabled_controllerPrepared_destroyController_starterSetAsNotReadyToStart() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//        mController.prepare();
//
//        mController.destroy();
//
//        verify(mPhraseSpotterStarter, times(1)).setReadyToStart(false);
//    }
//
//
//    @Test
//    public void spotterEnabled_controllerNotPrepared_openApplication_starterStopIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.onOpenApplication(TEST_PACKAGE_NAME);
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterNotEnabled_controllerPrepared_openApplication_starterStopIgnored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//        mController.prepare();
//
//        mController.onOpenApplication(TEST_PACKAGE_NAME);
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterEnabled_controllerPrepared_openApplication_starterStopped() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//        mController.prepare();
//
//        mController.onOpenApplication(TEST_PACKAGE_NAME);
//
//        verify(mPhraseSpotterStarter, times(1)).stop();
//    }
//
//
//    @Test
//    public void spotterEnabled_controllerNotPrepared_applicationOpened_starterStartNotScheduled() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.onOpenedApplication(TEST_PACKAGE_NAME);
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterNotEnabled_controllerPrepared_applicationOpened_starterStartNotScheduled() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//        mController.prepare();
//
//        mController.onOpenedApplication(TEST_PACKAGE_NAME);
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterEnabled_controllerPrepared_applicationOpened_starterStartScheduled() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//        mController.prepare();
//
//        mController.onOpenedApplication(TEST_PACKAGE_NAME);
//
//        verify(mPhraseSpotterStarter, times(1)).scheduleStart(PhraseSpotterControllerImpl.APP_OPENED_START_DELAY);
//    }
//
//    @Test
//    public void spotterEnabled_controllerNotPrepared_activityStarted_ignored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.onActivityStarted();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterNotEnabled_controllerPrepared_activityStarted_ignored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//        mController.prepare();
//
//        mController.onActivityStarted();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterEnabled_controllerPrepared_activityStarted_starterReadyToStart() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//        mController.prepare();
//        reset(mPhraseSpotterStarter);
//
//        mController.onActivityStarted();
//
//        verify(mPhraseSpotterStarter, times(1)).setReadyToStart(true);
//    }
//
//    @Test
//    public void spotterEnabled_controllerNotPrepared_activityStopped_ignored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//
//        mController.onActivityStopped();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterNotEnabled_controllerPrepared_activityStopped_ignored() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(false);
//        mController.prepare();
//
//        mController.onActivityStopped();
//
//        verifyZeroInteractions(mPhraseSpotterStarter);
//    }
//
//    @Test
//    public void spotterEnabled_controllerPrepared_activityStopped_starterNotReadyToStart() {
//        when(mSpotterEnabledStateProvider.isSpotterEnabled()).thenReturn(true);
//        mController.prepare();
//
//        mController.onActivityStopped();
//
//        verify(mPhraseSpotterStarter, times(1)).setReadyToStart(false);
//    }

    //endregion

    //region --------------------------- Helper methods

    /**
     * During updating the version of Mockito / PowerMock, tests can get broken unexpectedly
     * This check ensures that PowerMock / Mockito is well
     */
    private void assumeThatContextAndSpotterNotNull() {
//        Assume.assumeThat(mPhraseSpotter, notNullValue());
//        Assume.assumeThat(mAppContext, notNullValue());
    }

    //endregion
}
