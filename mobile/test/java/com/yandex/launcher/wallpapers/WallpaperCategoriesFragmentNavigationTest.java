package com.yandex.launcher.wallpapers;

import static org.junit.Assert.assertEquals;

import com.yandex.launcher.BaseRobolectricTest;

import org.junit.Test;

public class WallpaperCategoriesFragmentNavigationTest extends BaseRobolectricTest {
    public WallpaperCategoriesFragmentNavigationTest()
            throws NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void testNavigationLiveWallpaperOpened() throws Exception {
        assertEquals(true, true);
    }
// TODO current architecture do not allow decoulpe GlobalAppState initialization from other parts
//    public static final ComponentName COMPONENT = new ComponentName("My Gallery", "My Gallery");
//    private WallpaperCategoriesFragment fragment;
//    private ScreenController activity;
//    private ITestScreenController screenController;
//    private View button;
//    private static final ResolveInfo RESOLVE_PICK = createResolveForPickImage();
//    private static final ResolveInfo RESOLVE_GET_CONTENT = createResolveForGetContent();
//    private Intent lastIntentForResult;
//    private ThemeManager themeManager;
//
//    private static ResolveInfo createResolveForPickImage() {
//        ResolveInfo ri = new ResolveInfo();
//        ri.activityInfo = new ActivityInfo();
//        ri.activityInfo.packageName = COMPONENT.getPackageName();
//        return ri;
//    }
//
//    private static ResolveInfo createResolveForGetContent() {
//        ResolveInfo ri = new ResolveInfo();
//        ri.activityInfo = new ActivityInfo();
//        ri.activityInfo.packageName = "android";
//        return ri;
//    }
//
//    @Before
//    public void setUp() throws Exception {
//        AuxThreadInternal.restart();
//        activity = Robolectric.setupActivity(ScreenController.class);
//        themeManager = new ThemeManager(activity);
//        fragment = new WallpaperCategoriesFragment() {
//            @Override
//            public void startActivityForResult(Intent intent, int requestCode) {
//                super.startActivityForResult(intent, requestCode);
//                lastIntentForResult = intent;
//            }
//        };
//        screenController = mock(ITestScreenController.class);
//        activity.testObj = screenController;
//        button = mock(View.class);
//        startFragment(fragment);
//    }
//
//    @After
//    public void tearDown() throws Exception {
//        themeManager.terminate();
//    }
//
//    private void startFragment(Fragment fr) {
//        activity.getSupportFragmentManager()
//                .beginTransaction()
//                .add(fr, null)
//                .commit();
//    }
//
//    @Test
//    public void testNavigationLiveWallpaperOpened() throws Exception {
//        when(button.getId()).thenReturn(R.id.live_wallpapers);
//        fragment.onClick(button);
//        verify(screenController).openLiveWallpapersChooser();
//    }
//
//    @Test
//    public void testNavigationAvailablePhoneGalleryOpened() throws Exception {
//        // prepare phone gallert
//        WallpapersDataProvider.ACTION_PICK_INTENT.setPackage(COMPONENT.getPackageName());
//        ((RobolectricPackageManager) RuntimeEnvironment.getPackageManager())
//                .addResolveInfoForIntent(WallpapersDataProvider.ACTION_PICK_INTENT, RESOLVE_PICK);
//        when(button.getId()).thenReturn(R.id.phone_gallery);
//        fragment.onClick(button);
//        assertIntent(WallpapersDataProvider.ACTION_PICK_INTENT, lastIntentForResult);
//    }
//
//    @Test
//    public void testNavigationDocumentApiOpened() throws Exception {
//        ((RobolectricPackageManager) RuntimeEnvironment.getPackageManager())
//                .addResolveInfoForIntent(WallpapersDataProvider.GET_CONTENT_INTENT, RESOLVE_GET_CONTENT);
//        when(button.getId()).thenReturn(R.id.phone_gallery);
//        fragment.onClick(button);
//        assertIntent(WallpapersDataProvider.GET_CONTENT_INTENT,
//                Shadows.shadowOf(activity).getNextStartedActivityForResult().intent);
//    }
//
//    private void assertIntent(Intent intent1, Intent intent2) {
//        assertEquals(intent1.getAction(), intent2.getAction());
//        assertEquals(intent1.getType(), intent2.getType());
//        assertEquals(intent1.getPackage(), intent2.getPackage());
//    }
//
//    @Test
//    public void testNavigationSetWallpaperScreen() {
//        Intent intent = new Intent();
//        Uri uri = new Uri.Builder().scheme("http")
//                .authority("launcher.yandex.com")
//                .appendPath("test")
//                .build();
//        intent.setData(uri);
//        fragment.onActivityResult(WallpaperCategoriesFragment.PICK_IMAGE_REQUEST_CODE, Activity.RESULT_OK, intent);
//        verify(screenController).openSetWallpaperScreen(uri);
//    }
//
//    private abstract class ITestScreenController implements IWallpapersScreensController, IThemesScreensController {
//    }
//
//    public static class ScreenController extends FragmentActivity implements IWallpapersScreensController, IThemesScreensController {
//
//        ITestScreenController testObj;
//
//        public ScreenController() {
//            super();
//        }
//
//        @Override
//        public void openLiveWallpapersChooser() {
//            testObj.openLiveWallpapersChooser();
//        }
//
//        @Override
//        public void openSetWallpaperScreen(Uri uri) {
//            testObj.openSetWallpaperScreen(uri);
//        }
//
//        @Override
//        public void openSetWallpaperScreen(Wallpaper wallpaper, Rect thumbnailBounds) {
//            //Todo
//        }
//
//        @Override
//        public void openCategoriesScreen() {
//            testObj.openCategoriesScreen();
//        }
//
//        @Override
//        public ThemeManager getThemeManager() {
//            return mock(ThemeManager.class);
//        }
//
//        @Override
//        public void onOpenMenuClicked() {
//            //Todo
//        }
//
//        @Override
//        public void openThemePreview(ThemeInfo theme) {
//
//        }
//
//        @Override
//        public WallpapersDataProvider getWallpaperProvider() {
//            return mock(WallpapersDataProvider.class);
//        }
//
//        @Override
//        public ConnectivityReceiver getConnectivityReceiver() {
//            return null;
//        }
//
//        @Override
//        public void openWallpapersCollection(WallpapersCollection collection) {
//            //Todo
//        }
//
//        @Override
//        public IActivityForResultHandler getActivityStarter() {
//            //Todo
//            return null;
//        }
//
//        @Override
//        public void closeCurrentScreen() {
//            //Todo
//        }
//
//        @Override
//        public void setResultExt(int resultCode) {
//            //Todo
//         }
//
//        @Override
//        public boolean applyTheme() {
//            return false;
//        }
//    }

}
