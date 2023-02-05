package com.yandex.mail.tests;

import com.yandex.mail.pages.Account;
import com.yandex.mail.pages.ComposePage;
import com.yandex.mail.pages.ItemList;
import com.yandex.mail.rules.CameraInterceptorRule;
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule;
import com.yandex.mail.rules.FakeServerRule;
import com.yandex.mail.rules.FileManagerInterceptorRule;
import com.yandex.mail.rules.FileManagerInterceptorRule.WithFileFromPhone;
import com.yandex.mail.rules.GalleryInterceptorRule;
import com.yandex.mail.rules.GalleryInterceptorRule.WithPhotoFromGallery;
import com.yandex.mail.rules.LoginToAppRule;
import com.yandex.mail.rules.PlayWithCamera;
import com.yandex.mail.rules.PrepareGalleryRule;
import com.yandex.mail.rules.PrepareGalleryRule.CreatePhotoInGallery;
import com.yandex.mail.rules.RealAccount;
import com.yandex.mail.rules.WaitForAsyncJobsRule;
import com.yandex.mail.startupwizard.StartWizardActivity;
import com.yandex.mail.steps.ComposeSteps;
import com.yandex.mail.steps.FolderSteps;
import com.yandex.mail.steps.MessageListSteps;
import com.yandex.mail.steps.MessageViewSteps;
import com.yandex.mail.suites.BusinessLogic;
import com.yandex.mail.tests.data.ComposeFakeDataRule;
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static com.yandex.mail.DefaultSteps.clickOn;
import static com.yandex.mail.DefaultSteps.fillInTextField;
import static com.yandex.mail.DefaultSteps.shouldNotSee;
import static com.yandex.mail.DefaultSteps.shouldNotSeeTextOnView;
import static com.yandex.mail.DefaultSteps.shouldSee;
import static com.yandex.mail.DefaultSteps.shouldSeeText;
import static com.yandex.mail.DefaultSteps.shouldSeeTextOnView;
import static com.yandex.mail.TestUtil.randomText;
import static com.yandex.mail.espresso.ViewActions.actionOnItemViewAtPosition;
import static com.yandex.mail.pages.ComposePage.ADDRESS_TO;
import static com.yandex.mail.pages.ComposePage.ANOTHER_ADDRESS_TO;
import static com.yandex.mail.pages.ComposePage.ANOTHER_DEFAULT_FILE_NAME_FROM_NEW_USER_DISK;
import static com.yandex.mail.pages.ComposePage.DEFAULT_FILE_NAME_FROM_NEW_USER_DISK;
import static com.yandex.mail.pages.ComposePage.LINES_COUNT_IN_FILE_LESS_THAN_25_MB;
import static com.yandex.mail.pages.ComposePage.attachIcon;
import static com.yandex.mail.pages.ComposePage.attachImagesMenu;
import static com.yandex.mail.pages.ComposePage.confirmAttach;
import static com.yandex.mail.pages.ComposePage.imageCheckboxId;
import static com.yandex.mail.pages.ComposePage.textBody;
import static com.yandex.mail.pages.MessageListPage.MessageListTopBar.composeButton;
import static com.yandex.mail.pages.MessageViewPage.PREFIX_FWD_SUBJECT;
import static com.yandex.mail.pages.MessageViewPage.PREFIX_RE_SUBJECT;
import static com.yandex.mail.pages.TopBarBlock.upButton;
import static com.yandex.mail.steps.ComposeSteps.getAttachedPhotoName;
import static com.yandex.mail.tests.data.ComposeFakeDataRule.MESSAGE_REPLY_FORWARD_DRAFT;
import static com.yandex.mail.util.AccountsConst.FAKE_USER;
import static com.yandex.mail.util.AccountsConst.USER_LOGIN;
import static com.yandex.mail.util.AccountsConst.USER_PASSWORD;
import static com.yandex.mail.util.CameraInterceptor.Behavior.TAKE_FAKE_PHOTO_AND_SET_RESULT_SAVED;
import static com.yandex.mail.util.FoldersAndLabelsConst.DRAFTS_FOLDER;
import static com.yandex.mail.util.FoldersAndLabelsConst.SENT_FOLDER;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.FORWARD_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.REPLY_ALL_MENU;
import static com.yandex.mail.util.OperationsConst.ForOperationNames.REPLY_TO_MENU;
import static java.lang.String.format;

@RunWith(AndroidJUnit4.class)
public class DraftsTest {

    private static final int IMAGE_POSITION_IN_ATTACH_MENU = 1;

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
    private FolderSteps onFolderList = new FolderSteps();

    @NonNull
    private ItemList messageList = new ItemList();

    @NonNull
    private static MessageListSteps onMessageList = new MessageListSteps();

    @NonNull
    private static MessageViewSteps onMessageView = new MessageViewSteps();

    @Rule
    public TestName name = new TestName();

    @Rule
    public RuleChain chainRule = RuleChain.emptyRuleChain()
            .around(new FakeServerRule())
            .around(new ClearAppDataBeforeEachTestRule())
            .around(new ComposeFakeDataRule(FAKE_USER))
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
    //TODO: https://github.yandex-team.ru/mobmail/mobile-yandex-mail-client-android/issues/2792
    //@Acceptance
    @RealAccount
    public void shouldSaveAndSendDraft() {
        clickOn(composeButton());
        String subject = randomText(10);
        String bodyText = randomText(20);
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(ComposePage.subject(), subject);
        fillInTextField(textBody(), bodyText);
        onCompose
                .attachFilesFromDisk(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK)
                .shouldSeeAttachedFile(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);

        onFolderList.goBackToFolderList();
        onMessageList
                .shouldSeeMessageInFolder(subject, DRAFTS_FOLDER)
                .openMessage(subject);
        onCompose.shouldSeeDraftData(subject, bodyText, DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);
        clickOn(ComposePage.sendButton());
        clickOn(upButton());
        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER);
    }

    @Test
    @BusinessLogic
    public void shouldNotSaveEmptyDraft() {
        onFolderList.goBackToFolderList()
                .chooseFolderOrLabel(DRAFTS_FOLDER);

        clickOn(composeButton());
        clickOn(upButton());
        shouldNotSee(messageList.list());
    }

    @Test
    @BusinessLogic
    public void shouldNotSaveEmptyReplyDraft() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_REPLY_FORWARD_DRAFT)
                .openMessage(MESSAGE_REPLY_FORWARD_DRAFT);
        onMessageView.makeOperationFromMenuForExpandedMessage(REPLY_TO_MENU);

        onFolderList.goBackToFolderList();
        onFolderList.chooseFolderOrLabel(DRAFTS_FOLDER);
        shouldNotSee(messageList.list());
    }

    @Test
    @BusinessLogic
    public void shouldNotSaveEmptyReplyAllDraft() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_REPLY_FORWARD_DRAFT)
                .openMessage(MESSAGE_REPLY_FORWARD_DRAFT);
        onMessageView.makeOperationFromMenuForExpandedMessage(REPLY_ALL_MENU);

        onFolderList.goBackToFolderList();
        onFolderList.chooseFolderOrLabel(DRAFTS_FOLDER);
        shouldNotSee(messageList.list());
    }

    @Test
    @BusinessLogic
    public void shouldNotSaveEmptyForwardDraft() {
        onMessageList.shouldSeeMessageInCurrentFolder(MESSAGE_REPLY_FORWARD_DRAFT)
                .openMessage(MESSAGE_REPLY_FORWARD_DRAFT);
        onMessageView.makeOperationFromMenuForExpandedMessage(FORWARD_MENU);

        onFolderList.goBackToFolderList();
        onFolderList.chooseFolderOrLabel(DRAFTS_FOLDER);
        shouldNotSee(messageList.list());
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSaveDraftWithEditedFieldTo() {
        clickOn(upButton());
        onFolderList.chooseFolderOrLabel(DRAFTS_FOLDER);

        clickOn(composeButton());
        String subject = randomText(10);
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(ComposePage.subject(), subject);

        clickOn(upButton());
        onMessageList.openMessage(subject);

        fillInTextField(ComposePage.toField(), ANOTHER_ADDRESS_TO);

        clickOn(upButton());
        onMessageList.openMessage(subject);

        shouldSee(ComposePage.yableText(ANOTHER_ADDRESS_TO));
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSaveDraftWithRemovedAttach() {
        clickOn(upButton());
        onFolderList.chooseFolderOrLabel(DRAFTS_FOLDER);

        clickOn(composeButton());
        String subject = randomText(10);
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(ComposePage.subject(), subject);
        onCompose.attachFilesFromDisk(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);

        clickOn(upButton());
        onMessageList.openMessage(subject);

        onCompose.removeAttachWithName(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);

        clickOn(upButton());
        onMessageList.openMessage(subject);

        shouldNotSeeTextOnView(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSaveDraftWithEditedSubject() {
        clickOn(upButton());
        onFolderList.chooseFolderOrLabel(DRAFTS_FOLDER);

        clickOn(composeButton());
        String oldSubject = randomText(10);
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(ComposePage.subject(), oldSubject);

        clickOn(upButton());
        onMessageList.openMessage(oldSubject);

        String newSubject = randomText(10);
        fillInTextField(ComposePage.subject(), newSubject);

        clickOn(upButton());
        onMessageList.openMessage(newSubject);

        shouldSeeText(ComposePage.subject(), newSubject);
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSaveDraftWithAddedAttach() {
        clickOn(upButton());
        onFolderList.chooseFolderOrLabel(DRAFTS_FOLDER);

        clickOn(composeButton());
        String subject = randomText(10);
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(ComposePage.subject(), subject);

        clickOn(upButton());
        onMessageList.openMessage(subject);

        onCompose.attachFilesFromDisk(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);

        clickOn(upButton());
        onMessageList.openMessage(subject);

        shouldSeeText(ComposePage.attachPreviewTitle(), DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSaveDraftWithRemovedAndAddedAttach() {
        clickOn(upButton());
        onFolderList.chooseFolderOrLabel(DRAFTS_FOLDER);

        clickOn(composeButton());
        String subject = randomText(10);
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(ComposePage.subject(), subject);
        onCompose.attachFilesFromDisk(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);

        clickOn(upButton());
        onMessageList.openMessage(subject);

        onCompose.removeAttachWithName(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);
        onCompose.attachFilesFromDisk(ANOTHER_DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);

        clickOn(upButton());
        onMessageList.openMessage(subject);

        shouldSeeText(ComposePage.attachPreviewTitle(), ANOTHER_DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);
    }

    @Test
    @BusinessLogic
    @CreatePhotoInGallery()
    //TODO: https://github.yandex-team.ru/mobmail/mobile-yandex-mail-client-android/issues/2623
    public void shouldSaveDraftWithAttachWhenReplyTo() {
        String bodyText = randomText(20);

        onMessageList
                .shouldSeeMessageInCurrentFolder(MESSAGE_REPLY_FORWARD_DRAFT)
                .openMessage(MESSAGE_REPLY_FORWARD_DRAFT);
        onMessageView.makeOperationFromMenuForExpandedMessage(REPLY_TO_MENU);

        Espresso.closeSoftKeyboard(); // workaround for bug in library (when animation is disable, attach menu is shown at the top)
        clickOn(attachIcon());

        shouldSee(attachImagesMenu());
        onView(attachImagesMenu()).perform(actionOnItemViewAtPosition(IMAGE_POSITION_IN_ATTACH_MENU, imageCheckboxId(), click()));
        clickOn(confirmAttach());

        fillInTextField(textBody(), bodyText);

        onFolderList.goBackToFolderList();
        String reSubject = format(PREFIX_RE_SUBJECT, MESSAGE_REPLY_FORWARD_DRAFT);
        onMessageList.shouldSeeMessageInFolder(reSubject, DRAFTS_FOLDER)
                .openMessage(reSubject);

        onCompose.shouldSeeDraftData(reSubject, bodyText, getAttachedPhotoName(prepareGallery.getSavedFileUri()));
    }

    @Test
    @BusinessLogic
    @WithFileFromPhone(LINES_COUNT_IN_FILE_LESS_THAN_25_MB)
    //TODO: https://github.yandex-team.ru/mobmail/mobile-yandex-mail-client-android/issues/2623
    public void shouldSaveDraftWithAttachWhenForward() {
        String bodyText = randomText(20);

        onMessageList
                .shouldSeeMessageInCurrentFolder(MESSAGE_REPLY_FORWARD_DRAFT)
                .openMessage(MESSAGE_REPLY_FORWARD_DRAFT);
        onMessageView.makeOperationFromMenuForExpandedMessage(FORWARD_MENU);

        onCompose.attachFileFromPhone();
        fillInTextField(textBody(), bodyText);

        onFolderList.goBackToFolderList();
        String fwdSubject = format(PREFIX_FWD_SUBJECT, MESSAGE_REPLY_FORWARD_DRAFT);
        onMessageList.shouldSeeMessageInFolder(fwdSubject, DRAFTS_FOLDER)
                .openMessage(fwdSubject);

        onCompose.shouldSeeDraftData(fwdSubject, bodyText, getAttachedPhotoName(fileManager.getFileManagerInterceptor().getSavedFileUri()));
    }

    @Test
    @BusinessLogic
    @RealAccount
    public void shouldSendDraftWithAttachAfterEditing() {
        String subject = randomText(10);
        String bodyText = randomText(20);

        clickOn(upButton());
        onFolderList.chooseFolderOrLabel(DRAFTS_FOLDER);

        clickOn(composeButton());
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(ComposePage.subject(), subject);
        fillInTextField(textBody(), bodyText);

        clickOn(upButton());
        onMessageList.openMessage(subject);

        onCompose.attachFilesFromDisk(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);
        clickOn(ComposePage.sendButton());

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(subject, SENT_FOLDER).openMessage(subject);

        onMessageView.shouldSeeSentMessageData(subject, bodyText, DEFAULT_FILE_NAME_FROM_NEW_USER_DISK);
    }

    @Test
    @BusinessLogic
    @PlayWithCamera(TAKE_FAKE_PHOTO_AND_SET_RESULT_SAVED)
    @WithPhotoFromGallery
    @WithFileFromPhone(LINES_COUNT_IN_FILE_LESS_THAN_25_MB)
    @RealAccount
    public void shouldSaveDraftWithAllTypesOfAttaches() {
        String subject = randomText(10);

        clickOn(composeButton());
        fillInTextField(ComposePage.toField(), ADDRESS_TO);
        fillInTextField(ComposePage.subject(), subject);

        onCompose.attachFilesFromDisk(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK)
                .attachPhotoFromCamera()
                .attachPhotoFromGallery()
                .attachFileFromPhone();

        String cameraPhotoName = getAttachedPhotoName(camera.getCameraInterceptor().getSavedFileUri()),
                galleryPhotoName = getAttachedPhotoName(gallery.getGalleryInterceptor().getSavedFileUri()),
                fileFromPhoneName = getAttachedPhotoName(fileManager.getFileManagerInterceptor().getSavedFileUri());

        onFolderList.goBackToFolderList();
        onMessageList.shouldSeeMessageInFolder(subject, DRAFTS_FOLDER)
                .openMessage(subject);

        shouldSeeTextOnView(DEFAULT_FILE_NAME_FROM_NEW_USER_DISK, cameraPhotoName, galleryPhotoName, fileFromPhoneName);
    }
}
