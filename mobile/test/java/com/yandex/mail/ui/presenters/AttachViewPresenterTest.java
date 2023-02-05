package com.yandex.mail.ui.presenters;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.compose.ComposeAttachMode;
import com.yandex.mail.model.AttachViewModel;
import com.yandex.mail.model.MediaStoreImage;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.ui.presenters.configs.AttachViewPresenterConfig;
import com.yandex.mail.ui.views.AttachView;
import com.yandex.mail.util.BaseIntegrationTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import io.reactivex.Single;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.TestUtils.serializeAndDeserializeState;
import static io.reactivex.schedulers.Schedulers.trampoline;
import static kotlin.collections.SetsKt.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@SuppressWarnings("unchecked")
@RunWith(IntegrationTestRunner.class)
public class AttachViewPresenterTest extends BaseIntegrationTest {

    @SuppressWarnings("NullableProblems")
    @NonNull
    private AttachViewPresenter presenter;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private AttachView attachView;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private List<MediaStoreImage> list;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private Menu menu;

    @Before
    public void beforeEachTest() {
        attachView = mock(AttachView.class);
        list = getTestImageList();
        menu = mock(Menu.class);
        presenter = createPresenter();
    }

    @NonNull
    private List<MediaStoreImage> getTestImageList() {
        List<MediaStoreImage> mediaStoreImages = new LinkedList<>();
        mediaStoreImages.add(new MediaStoreImage(createTestUri("path1"), 100));
        mediaStoreImages.add(new MediaStoreImage(createTestUri("path2"), 103));
        return mediaStoreImages;
    }

    @Test
    public void loadImages_imageListReceived() {
        presenter.onBindView(attachView);
        presenter.loadImages(ComposeAttachMode.FILE);

        shadowOf(getMainLooper()).idle();

        verify(attachView).showImages(list);
        verify(attachView).showCameraItem();
    }

    @Test
    public void setImageChecked_checkSeveralImages() {
        presenter.onBindView(attachView);

        presenter.setImageChecked(createTestUri("path1"), 100);
        verify(attachView).showConfirmUi(1, 100);

        presenter.setImageChecked(createTestUri("path2"), 103);
        verify(attachView).showConfirmUi(2, 203);
    }

    @Test
    public void setImageChecked_uncheckImages() {
        presenter.onBindView(attachView);

        presenter.setImageChecked(createTestUri("path1"), 100);
        verify(attachView).showConfirmUi(1, 100);

        presenter.setImageChecked(createTestUri("path2"), 103);
        verify(attachView).showConfirmUi(2, 203);

        presenter.setImageUnchecked(createTestUri("path1"));
        verify(attachView).showConfirmUi(1, 103);

        presenter.setImageUnchecked(createTestUri("path2"));
        verify(attachView).showDismissUi();
    }

    @Test
    public void uncheckImages() {
        presenter.onBindView(attachView);

        presenter.setImageChecked(createTestUri("path1"), 100);
        presenter.setImageChecked(createTestUri("path2"), 100);

        presenter.uncheckImages();

        verify(attachView).showDismissUi();
    }

    @Test
    public void pressConfirm_severalUris() {
        presenter.onBindView(attachView);

        presenter.setImageChecked(createTestUri("path1"), 100);
        presenter.setImageChecked(createTestUri("path2"), 103);

        presenter.confirm();

        Set<Uri> set = new HashSet<>();
        set.add(createTestUri("path1"));
        set.add(createTestUri("path2"));

        verify(attachView).onConfirm(set);
    }

    // it's impossible, but who knows...
    @Test
    public void pressConfirm_nothing() {
        presenter.onBindView(attachView);

        presenter.confirm();

        verify(attachView, never()).onConfirm(emptySet());
    }

    @Test
    public void pressDismiss_withSelection() {
        presenter.onBindView(attachView);

        presenter.setImageChecked(createTestUri("path1"), 100);
        presenter.setImageChecked(createTestUri("path2"), 103);

        presenter.dismiss();

        verify(attachView, never()).onConfirm(any());
        verify(attachView).onDismiss();
    }

    @Test
    public void pressDismiss_nothing() {
        presenter.onBindView(attachView);

        presenter.dismiss();

        verify(attachView).onDismiss();
    }

    @Test
    public void restorePresenterState_setStateAfterBinding() {
        AttachViewPresenter oldPresenter = createPresenter();

        oldPresenter.onBindView(attachView);
        oldPresenter.loadImages(ComposeAttachMode.FILE);

        oldPresenter.setImageChecked(createTestUri("path1"), 100);
        verify(attachView).showConfirmUi(1, 100);

        oldPresenter.setImageChecked(createTestUri("path2"), 103);
        verify(attachView).showConfirmUi(2, 203);

        Bundle state = oldPresenter.getState();
        oldPresenter.onUnbindView(attachView);

        AttachViewPresenter newPresenter = createPresenter();

        newPresenter.onBindView(attachView);
        newPresenter.loadImages(ComposeAttachMode.FILE);

        newPresenter.setState(state);

        Set<Uri> uris = new LinkedHashSet<>();
        uris.add(createTestUri("path1"));
        uris.add(createTestUri("path2"));

        shadowOf(getMainLooper()).idle();

        verify(attachView).setCheckedItems(uris);
    }

    @Test
    public void restorePresenterState_setStateBeforeBinding() {
        AttachViewPresenter oldPresenter = createPresenter();

        oldPresenter.onBindView(attachView);
        oldPresenter.loadImages(ComposeAttachMode.FILE);

        oldPresenter.setImageChecked(createTestUri("path1"), 100);
        verify(attachView).showConfirmUi(1, 100);

        oldPresenter.setImageChecked(createTestUri("path2"), 103);
        verify(attachView).showConfirmUi(2, 203);

        Bundle state = oldPresenter.getState();
        oldPresenter.onUnbindView(attachView);

        AttachViewPresenter newPresenter = createPresenter();

        newPresenter.setState(state);

        newPresenter.onBindView(attachView);
        newPresenter.loadImages(ComposeAttachMode.FILE);

        Set<Uri> uris = new LinkedHashSet<>();
        uris.add(createTestUri("path1"));
        uris.add(createTestUri("path2"));

        shadowOf(getMainLooper()).idle();
        verify(attachView).setCheckedItems(uris);
    }

    @Test
    public void restorePresenterState_afterDeserialization() {
        presenter.onBindView(attachView);
        presenter.loadImages(ComposeAttachMode.FILE);

        presenter.setImageChecked(createTestUri("path1"), 100);
        presenter.setImageChecked(createTestUri("path2"), 103);

        Bundle state = presenter.getState();
        Bundle recreatedState = serializeAndDeserializeState(state);

        presenter.setState(recreatedState);

        Set<Uri> uris = new LinkedHashSet<>();
        uris.add(createTestUri("path1"));
        uris.add(createTestUri("path2"));

        shadowOf(getMainLooper()).idle();

        verify(attachView).setCheckedItems(uris);
    }

    @Test
    public void setState_notCrashWithEmptyState() {
        presenter.onBindView(attachView);

        presenter.setState(new Bundle());
    }

    @NonNull
    private AttachViewPresenter createPresenter() {
        BaseMailApplication application = mock(BaseMailApplication.class);
        AttachViewModel attachViewModel = mock(AttachViewModel.class);
        AttachViewPresenterConfig presenterConfig = new AttachViewPresenterConfig(
                trampoline(), emptySet(), 10
        );
        AttachViewPresenter presenter = new AttachViewPresenter(application, attachViewModel, presenterConfig);

        Single<List<MediaStoreImage>> single = Single.just(list);
        when(attachViewModel.getImageListFromGallery(10, emptySet())).thenReturn(single);

        return presenter;
    }

    @NonNull
    private Uri createTestUri(@NonNull String path) {
        return new Uri.Builder().path(path).build();
    }
}
