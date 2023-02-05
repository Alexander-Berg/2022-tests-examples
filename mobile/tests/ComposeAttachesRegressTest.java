package com.yandex.mail.tests;

import com.yandex.mail.TestCaseId;
import com.yandex.mail.pages.Account;
import com.yandex.mail.rules.CameraInterceptorRule;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FileManagerInterceptorRule;
import com.yandex.mail.rules.FileManagerInterceptorRule.WithFileFromPhone;
import com.yandex.mail.rules.GalleryInterceptorRule;
import com.yandex.mail.rules.GalleryInterceptorRule.WithPhotoFromGallery;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.PlayWithCamera;
import com.yandex.mail.rules.PrepareGalleryRule;
import com.yandex.mail.rules.RealAccount;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.ComposeSteps;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.util.SetUpFailureHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.fillInTextField;
import static com.yandex.mail.DefaultSteps.shouldNotSeeTextOnView;
import static com.yandex.mail.TestUtil.randomText;
import static com.yandex.mail.pages.ComposePage.ADDRESS_TO;
import static com.yandex.mail.pages.ComposePage.LINES_COUNT_IN_FILE_LESS_THAN_25_MB;
import static com.yandex.mail.pages.ComposePage.subject;
import static com.yandex.mail.pages.ComposePage.toField;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.composeButton;
import static com.yandex.mail.steps.ComposeSteps.getAttachedPhotoName;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.CameraInterceptor.Behavior.TAKE_FAKE_PHOTO_AND_SET_RESULT_SAVED;
import static com.yandex.mail.util.FoldersAndLabelsConst.DRAFTS_FOLDER;

@RunWith(AndroidJUnit4.class)
public class ComposeAttachesRegressTest {

    private static final String[] DEFAULT_FILES_FROM_DISK = {"Mountains.jpg", "Bears.jpg", "Music"};

    @NonNull
    private CameraInterceptorRule camera = new CameraInterceptorRule();

    @NonNull
    private GalleryInterceptorRule gallery = new GalleryInterceptorRule();

    @NonNull
    private PrepareGalleryRule prepareGallery = new PrepareGalleryRule();

    @NonNull
    private FileManagerInterceptorRule fileManager = new FileManagerInterceptorRule();

    @NonNull
    private ComposeSteps onCompose = new ComposeSteps();

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(camera)
            .around(gallery)
            .around(fileManager)
            .around(prepareGallery)
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @RealAccount
    @TestCaseId("1374")
    @PlayWithCamera(TAKE_FAKE_PHOTO_AND_SET_RESULT_SAVED)
    @WithPhotoFromGallery
    @WithFileFromPhone(LINES_COUNT_IN_FILE_LESS_THAN_25_MB)
    public void shouldRemoveAttachesFromCompose() {
        String subject = randomText(10);

        clickOn(composeButton());
        fillInTextField(toField(), ADDRESS_TO);
        fillInTextField(subject(), subject);

        onCompose.attachFilesFromDisk(DEFAULT_FILES_FROM_DISK[0])
                .attachPhotoFromCamera()
                .attachPhotoFromGallery()
                .attachFileFromPhone();

        String cameraPhotoName = getAttachedPhotoName(camera.getCameraInterceptor().getSavedFileUri()),
                galleryPhotoName = getAttachedPhotoName(gallery.getGalleryInterceptor().getSavedFileUri()),
                fileFromPhoneName = getAttachedPhotoName(fileManager.getFileManagerInterceptor().getSavedFileUri());

        onCompose.removeAttachWithName(DEFAULT_FILES_FROM_DISK[0], cameraPhotoName, galleryPhotoName, fileFromPhoneName);
        shouldNotSeeTextOnView(DEFAULT_FILES_FROM_DISK[0], cameraPhotoName, galleryPhotoName, fileFromPhoneName);
    }

    @Test
    @RealAccount
    @TestCaseId("4443")
    @PlayWithCamera(TAKE_FAKE_PHOTO_AND_SET_RESULT_SAVED)
    @WithPhotoFromGallery
    @WithFileFromPhone(LINES_COUNT_IN_FILE_LESS_THAN_25_MB)
    public void shouldRemoveAttachesFromDraft() {
        String subject = randomText(10);

        clickOn(composeButton());
        fillInTextField(toField(), ADDRESS_TO);
        fillInTextField(subject(), subject);

        onCompose.attachFilesFromDisk(DEFAULT_FILES_FROM_DISK[0])
                .attachPhotoFromCamera()
                .attachPhotoFromGallery()
                .attachFileFromPhone();

        String cameraPhotoName = getAttachedPhotoName(camera.getCameraInterceptor().getSavedFileUri()),
                galleryPhotoName = getAttachedPhotoName(gallery.getGalleryInterceptor().getSavedFileUri()),
                fileFromPhoneName = getAttachedPhotoName(fileManager.getFileManagerInterceptor().getSavedFileUri());

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(subject, DRAFTS_FOLDER)
                .openMessage(subject);
        onCompose.removeAttachWithName(DEFAULT_FILES_FROM_DISK[0], cameraPhotoName, galleryPhotoName, fileFromPhoneName);
        shouldNotSeeTextOnView(DEFAULT_FILES_FROM_DISK[0], cameraPhotoName, galleryPhotoName, fileFromPhoneName);
    }
}
