package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.pages.ComposePage;
import com.yandex.mail.pages.MessageViewPage;
import com.yandex.mail.rules.CameraInterceptorRule;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FileManagerInterceptorRule;
import com.yandex.mail.rules.FileManagerInterceptorRule.WithFileFromPhone;
import com.yandex.mail.rules.GalleryInterceptorRule;
import com.yandex.mail.rules.GalleryInterceptorRule.WithPhotoFromGallery;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.PlayWithCamera;
import com.yandex.mail.rules.PrepareGalleryRule;
import com.yandex.mail.rules.PrepareGalleryRule.CreatePhotoInGallery;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.ComposeSteps;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.util.SetUpFailureHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.espresso.Espresso;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import kotlin.Pair;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static com.yandex.mail.DefaultSteps.TIMEOUT_WAIT_FOR_ITEMS;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.fillInTextField;
import static com.yandex.mail.DefaultSteps.shouldNotSee;
import static com.yandex.mail.DefaultSteps.shouldNotSeeTextOnView;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.DefaultSteps.shouldSeeTextForWebElements;
import static com.yandex.mail.DefaultSteps.shouldSeeTextOnView;
import static com.yandex.mail.TestUtil.randomText;
import static com.yandex.mail.espresso.ViewActions.actionOnItemViewAtPosition;
import static com.yandex.mail.pages.ComposePage.ADDRESS_TO;
import static com.yandex.mail.pages.ComposePage.DEFAULT_FILE_NAME_FROM_NEW_USER_DISK;
import static com.yandex.mail.pages.ComposePage.LINES_COUNT_IN_FILE_LESS_THAN_25_MB;
import static com.yandex.mail.pages.ComposePage.LINES_COUNT_IN_FILE_MORE_THAN_25_MB;
import static com.yandex.mail.pages.ComposePage.attachIcon;
import static com.yandex.mail.pages.ComposePage.attachImagesMenu;
import static com.yandex.mail.pages.ComposePage.attachMenu;
import static com.yandex.mail.pages.ComposePage.attachPreviewIcon;
import static com.yandex.mail.pages.ComposePage.confirmAttach;
import static com.yandex.mail.pages.ComposePage.diskAttachAsLinkText;
import static com.yandex.mail.pages.ComposePage.imageCheckboxId;
import static com.yandex.mail.pages.ComposePage.sendButton;
import static com.yandex.mail.pages.ComposePage.subject;
import static com.yandex.mail.pages.ComposePage.textBody;
import static com.yandex.mail.pages.ComposePage.toField;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.composeButton;
import static com.yandex.mail.pages.MessageViewPage.MessageBody.diskAttachIcon;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.steps.ComposeSteps.getAttachedPhotoName;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.CameraInterceptor.Behavior.DO_NOT_TAKE_FAKE_PHOTO_AND_SET_RESULT_CANCELED;
import static com.yandex.mail.util.CameraInterceptor.Behavior.TAKE_FAKE_PHOTO_AND_SET_RESULT_CANCELED;
import static com.yandex.mail.util.CameraInterceptor.Behavior.TAKE_FAKE_PHOTO_AND_SET_RESULT_SAVED;
import static com.yandex.mail.util.FoldersAndLabelsConst.SENT_FOLDER;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class ComposeAttachesTest {

    private static final String[] DEFAULT_FILES_FROM_DISK = {"Mountains.jpg", "Bears.jpg", "Music"};

    private static final int IMAGE_POSITION_IN_ATTACH_MENU = 1;

    private static final int WAIT_LARGE_MESSAGE_TIMEOUT = 45;

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
    private MessageViewSteps onMessageView = new MessageViewSteps();

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
    @BusinessLogic
    public void shouldSendMessageWithDiskAttach() {
        String subject = randomText(10);
        String bodyText = randomText(20);
        clickOn(composeButton());
        fillInTextField(toField(), ADDRESS_TO);
        fillInTextField(subject(), subject);
        fillInTextField(textBody(), bodyText);
        onCompose
                .attachFilesFromDisk(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK)
                .shouldSeeAttachedFile(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);
        clickOn(sendButton());
        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER)
                .openMessage(subject);
        onMessageView.shouldSeeSentMessageData(subject, bodyText, DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);
    }

    @Test
    @WithPhotoFromGallery
    //@Acceptance
    //todo: WebView Espresso bug https://code.google.com/p/android/issues/detail?id=211947
    //todo: https://github.yandex-team.ru/mobmail/mobile-yandex-mail-client-android/issues/2792
    public void shouldSendMessageWithGalleryImg() {
        String subject = randomText(10);
        String bodyText = randomText(20);
        clickOn(composeButton());
        fillInTextField(toField(), ADDRESS_TO);
        fillInTextField(subject(), subject);
        fillInTextField(textBody(), bodyText);
        onCompose.attachPhotoFromGallery();
        shouldSeeTextOnView(getAttachedPhotoName(gallery.getGalleryInterceptor().getSavedFileUri()));
        String galleryPhotoName = getAttachedPhotoName(gallery.getGalleryInterceptor().getSavedFileUri());
        clickOn(sendButton());
        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER)
                .openMessage(subject);
        shouldSee(5, SECONDS, MessageViewPage.MessageBody.messageSubject());
        shouldSeeTextForWebElements(
                new Pair<>(MessageViewPage.MessageBody.messageSubject(), subject),
                new Pair<>(MessageViewPage.MessageBody.messageText(), bodyText)
        );
        onMessageView.shouldSeePhotoAttaches(galleryPhotoName);
    }

    @Test
    @BusinessLogic
    @PlayWithCamera(TAKE_FAKE_PHOTO_AND_SET_RESULT_SAVED)
    public void shouldAttachCameraImage() {
        clickOn(composeButton());
        onCompose.attachPhotoFromCamera();
        shouldSeeTextOnView(getAttachedPhotoName(camera.getCameraInterceptor().getSavedFileUri()));
    }

    @Test
    @BusinessLogic
    @PlayWithCamera(TAKE_FAKE_PHOTO_AND_SET_RESULT_CANCELED)
    public void shouldNotAttachCameraImageIfTakeAndCancel() {
        clickOn(composeButton());
        onCompose.attachPhotoFromCamera();
        shouldNotSeeTextOnView(getAttachedPhotoName(camera.getCameraInterceptor().getSavedFileUri()));
    }

    @Test
    @BusinessLogic
    @PlayWithCamera(DO_NOT_TAKE_FAKE_PHOTO_AND_SET_RESULT_CANCELED)
    public void shouldNotAttachCameraImageIfDoNotTakeAndCancel() {
        clickOn(composeButton());
        onCompose.attachPhotoFromCamera();
        shouldNotSeeTextOnView(getAttachedPhotoName(camera.getCameraInterceptor().getSavedFileUri()));
    }

    @Test
    @BusinessLogic
    public void shouldSendMessageWithManyDiskAttaches() {
        String subject = randomText(10);

        clickOn(composeButton());
        fillInTextField(toField(), ADDRESS_TO);
        fillInTextField(subject(), subject);
        onCompose.attachFilesFromDisk(DEFAULT_FILES_FROM_DISK);

        clickOn(sendButton());
        clickOn(upButton());

        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER)
                .openMessage(subject);
        onMessageView.shouldSeeDiskAndFileAttaches(DEFAULT_FILES_FROM_DISK);
    }

    @Test
    @BusinessLogic
    @WithPhotoFromGallery
    public void shouldSeeAllElementsOfAttachedPhoto() {
        clickOn(composeButton());
        onCompose.attachPhotoFromGallery()
                .shouldSeeAttachedFile(getAttachedPhotoName(gallery.getGalleryInterceptor().getSavedFileUri()));
    }

    @Test
    @BusinessLogic
    @WithFileFromPhone(LINES_COUNT_IN_FILE_LESS_THAN_25_MB)
    public void shouldSeeAllElementsOfAttachedFileFromPhone() {
        clickOn(composeButton());
        onCompose.attachFileFromPhone()
                .shouldSeeAttachedFile(getAttachedPhotoName(fileManager.getFileManagerInterceptor().getSavedFileUri()));
    }

    @Test
    @BusinessLogic
    public void shouldSeeAllElementsOfDiskAttach() {
        clickOn(composeButton());
        onCompose.attachFilesFromDisk(DEFAULT_FILES_FROM_DISK[0])
                .shouldSeeAttachedFile(DEFAULT_FILES_FROM_DISK[0]);
        shouldSee(diskAttachAsLinkText());
    }

    @Test
    @BusinessLogic
    @PlayWithCamera(TAKE_FAKE_PHOTO_AND_SET_RESULT_SAVED)
    @WithPhotoFromGallery
    @WithFileFromPhone(LINES_COUNT_IN_FILE_LESS_THAN_25_MB)
    public void shouldSendMessageWithAllTypesOfAttaches() {
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

        clickOn(sendButton());
        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER)
                .openMessage(subject);

        onMessageView.shouldSeePhotoAttaches(cameraPhotoName, galleryPhotoName)
                .shouldSeeDiskAndFileAttaches(DEFAULT_FILES_FROM_DISK[0], fileFromPhoneName);
    }

    @Test
    @BusinessLogic
    public void shouldQuitAttachMenuWhenCancel() {
        clickOn(composeButton());
        Espresso.closeSoftKeyboard(); // workaround for bug in library (when animation is disable, attach menu is shown at the top)
        clickOn(attachIcon());
        clickOn(ComposePage.dismissAttach());

        shouldNotSee(attachMenu(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
        shouldNotSee(attachPreviewIcon());
    }

    @Test
    @BusinessLogic
    @CreatePhotoInGallery
    public void shouldAttachImageDirectlyFromMenu() {
        clickOn(composeButton());
        Espresso.closeSoftKeyboard(); // workaround for bug in library (when animation is disable, attach menu is shown at the top)
        clickOn(attachIcon());

        shouldSee(attachImagesMenu());
        onView(attachImagesMenu()).perform(actionOnItemViewAtPosition(IMAGE_POSITION_IN_ATTACH_MENU, imageCheckboxId(), click()));
        clickOn(confirmAttach());

        shouldSeeTextOnView(getAttachedPhotoName(prepareGallery.getSavedFileUri()));
    }

    @Test
    @BusinessLogic
    @WithFileFromPhone(LINES_COUNT_IN_FILE_MORE_THAN_25_MB)
    public void shouldSendLargeAttachAsDiskLink() {
        String subject = randomText(10);
        String bodyText = randomText(20);

        clickOn(composeButton());
        fillInTextField(toField(), ADDRESS_TO);
        fillInTextField(subject(), subject);
        fillInTextField(ComposePage.textBody(), bodyText);

        onCompose.attachFileFromPhone();
        String fileFromPhoneName = getAttachedPhotoName(fileManager.getFileManagerInterceptor().getSavedFileUri());

        shouldSeeTextOnView(fileFromPhoneName);
        clickOn(sendButton());

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER, WAIT_LARGE_MESSAGE_TIMEOUT, SECONDS)
                .openMessage(subject);

        shouldSee(TIMEOUT_WAIT_FOR_ITEMS, SECONDS, diskAttachIcon());
        onMessageView.shouldSeeDiskAndFileAttaches(fileFromPhoneName);
    }
}
