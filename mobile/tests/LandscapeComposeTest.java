package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.pages.ComposePage;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FileManagerInterceptorRule;
import com.yandex.mail.rules.FileManagerInterceptorRule.WithFileFromPhone;
import com.yandex.mail.rules.GalleryInterceptorRule;
import com.yandex.mail.rules.GalleryInterceptorRule.WithPhotoFromGallery;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.PrepareGalleryRule;
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
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static com.yandex.mail.DefaultSteps.TIMEOUT_WAIT_FOR_ITEMS;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.fillInTextField;
import static com.yandex.mail.DefaultSteps.setUpLandscape;
import static com.yandex.mail.DefaultSteps.setUpPortrait;
import static com.yandex.mail.DefaultSteps.shouldNotSee;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.DefaultSteps.shouldSeeFocusedField;
import static com.yandex.mail.DefaultSteps.shouldSeeTextOnView;
import static com.yandex.mail.TestUtil.randomText;
import static com.yandex.mail.pages.ComposePage.ADDRESS_TO;
import static com.yandex.mail.pages.ComposePage.LINES_COUNT_IN_FILE_MORE_THAN_25_MB;
import static com.yandex.mail.pages.ComposePage.LINK_TO_SEND_IN_MESSAGE;
import static com.yandex.mail.pages.ComposePage.attachIcon;
import static com.yandex.mail.pages.ComposePage.attachImagesMenu;
import static com.yandex.mail.pages.ComposePage.attachMenu;
import static com.yandex.mail.pages.ComposePage.menuAttachAlbum;
import static com.yandex.mail.pages.ComposePage.menuAttachDisk;
import static com.yandex.mail.pages.ComposePage.menuAttachPhoto;
import static com.yandex.mail.pages.ComposePage.menuTakePhoto;
import static com.yandex.mail.pages.ComposePage.sendButton;
import static com.yandex.mail.pages.ComposePage.subject;
import static com.yandex.mail.pages.ComposePage.toField;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.composeButton;
import static com.yandex.mail.steps.ComposeSteps.getAttachedPhotoName;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.FoldersAndLabelsConst.DRAFTS_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.SENT_FOLDER;
import static java.util.concurrent.TimeUnit.SECONDS;

@RunWith(AndroidJUnit4.class)
public class LandscapeComposeTest {

    private static final String DEFAULT_FILES_FROM_DISK = "Mountains.jpg";

    private static final int WAIT_LARGE_MESSAGE_TIMEOUT = 45;

    @NonNull
    private GalleryInterceptorRule gallery = new GalleryInterceptorRule();

    @NonNull
    private PrepareGalleryRule prepareGallery = new PrepareGalleryRule();

    @NonNull
    private FileManagerInterceptorRule fileManager = new FileManagerInterceptorRule();

    @NonNull
    private ComposeSteps onCompose = new ComposeSteps();

    @NonNull
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    private MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private MessageViewSteps onMessageView = new MessageViewSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new ActivityTestRule<>(StartWizardActivity.class))
            .around(new WaitForAsyncJobsRule())
            .around(gallery)
            .around(prepareGallery)
            .around(fileManager)
            .around(new LoginToAppRule(new Account(USER_LOGIN, USER_PASSWORD)));

    @Before
    public void setUp() throws Exception {
        SetUpFailureHandler.setUpFailureHandler(getClass().getName(), name.getMethodName());
    }

    @Test
    @BusinessLogic
    @WithPhotoFromGallery
    public void shouldSendMessageAfterRotation() {
        String subject = randomText(10);
        clickOn(composeButton());
        shouldSee(ComposePage.toField());
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(subject(), subject);
        fillInTextField(ComposePage.textBody(), LINK_TO_SEND_IN_MESSAGE);
        onCompose.attachPhotoFromGallery();
        String galleryPhotoName = getAttachedPhotoName(gallery.getGalleryInterceptor().getSavedFileUri());
        shouldSeeTextOnView(galleryPhotoName);
        onCompose.attachFilesFromDisk(DEFAULT_FILES_FROM_DISK);

        setUpLandscape();

        clickOn(sendButton());
        setUpPortrait();
        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER)
                .openMessage(subject);

        onMessageView.shouldSeePhotoAttaches(galleryPhotoName)
                .shouldSeeDiskAndFileAttaches(DEFAULT_FILES_FROM_DISK);
    }

    @Test
    @BusinessLogic
    @WithPhotoFromGallery
    public void shouldSaveDraftAfterRotation() {
        String subject = randomText(10);
        String bodyText = randomText(20);

        clickOn(composeButton());
        shouldSee(ComposePage.toField());
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(subject(), subject);
        fillInTextField(ComposePage.textBody(), bodyText);
        onCompose.attachPhotoFromGallery();
        setUpLandscape();
        String galleryPhotoName = getAttachedPhotoName(gallery.getGalleryInterceptor().getSavedFileUri());
        shouldSeeTextOnView(galleryPhotoName);
        onFolderList.goBackToFolderList();
        setUpLandscape();
        onMessageList
                .shouldSeeMessageInFolder(subject, DRAFTS_FOLDER)
                .openMessage(subject);
        onCompose.shouldSeeDraftData(subject, bodyText, galleryPhotoName);
    }


    @Test
    @BusinessLogic
    @WithFileFromPhone(LINES_COUNT_IN_FILE_MORE_THAN_25_MB)
    public void shouldAttachFileInComposeAfterRotation() {
        String subject = randomText(10);

        clickOn(composeButton());
        fillInTextField(toField(), ADDRESS_TO);
        fillInTextField(subject(), subject);

        onCompose.attachFileFromPhone();
        String fileFromPhoneName = getAttachedPhotoName(fileManager.getFileManagerInterceptor().getSavedFileUri());

        setUpLandscape();
        shouldSeeTextOnView(fileFromPhoneName);
    }

    @Test
    @BusinessLogic
    public void shouldSeeFocusedToFieldAfterRotation() {
        setUpLandscape();
        clickOn(composeButton());
        shouldSeeFocusedField(toField(), TIMEOUT_WAIT_FOR_ITEMS, SECONDS);
    }

    @Test
    @BusinessLogic
    public void shouldSeeMenuAttachesAfterRotation() {
        clickOn(composeButton());
        closeSoftKeyboard(); // workaround for bug in library (when animation is disable, attach menu is shown at the top)
        clickOn(attachIcon());
        setUpLandscape();

        shouldSee(attachMenu(), menuAttachDisk(), menuAttachDisk(), menuAttachAlbum(), menuTakePhoto());

        setUpPortrait();
        shouldSee(menuAttachPhoto(), attachImagesMenu(), menuAttachDisk(), menuAttachDisk(), menuAttachAlbum());
        shouldNotSee(menuTakePhoto());
    }
}
