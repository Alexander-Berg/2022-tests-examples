package ru.yandex.autotests.mobile.disk.android.notes

import com.google.inject.Inject
import ru.yandex.autotests.mobile.disk.android.steps.*

abstract class NotesTestBase {
    //var BODY_PLACEHOLDER =
    //    "Write something" TODO Add check placeholder for body, after repare it https://st.yandex-team.ru/MOBDISK-15306

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onNotes: NotesSteps

    @Inject
    lateinit var onNotesApi: NotesApiSteps

    @Inject
    lateinit var onMobile: CommonsSteps

    @Inject
    lateinit var onPhotos: PhotosSteps

    @Inject
    lateinit var onPreview: PreviewSteps

    @Inject
    lateinit var onGroupMode: GroupModeSteps
}
