package com.yandex.mail.tools

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.yandex.mail.R
import com.yandex.mail.container.AccountInfoContainer
import com.yandex.mail.dialog.ConfirmationDialog
import com.yandex.mail.entity.Folder
import com.yandex.mail.feedback.FeedbackImprovement
import com.yandex.mail.feedback.FeedbackProblem
import com.yandex.mail.feedback.FeedbackSurvey
import com.yandex.mail.notifications.ChannelSettings
import com.yandex.mail.settings.DismissCallback
import com.yandex.mail.settings.SettingsActivityView
import com.yandex.mail.settings.SettingsFragmentsInvoker
import com.yandex.mail.settings.entry_settings.EntrySettingsFragment.EntrySettingsFragmentCallbacks
import com.yandex.mail.settings.folders.FolderChooserFragment.FolderChooserFragmentCallback
import com.yandex.mail.settings.support.ConnectionTypeSelectionFragment.ConnectionTypeSelectionFragmentCallback
import com.yandex.mail.settings.support.ImprovementFragmentCallback
import com.yandex.mail.settings.support.ProblemFragment.ProblemFragmentCallback
import com.yandex.mail.settings.support.SupportSettingsFragment.SupportSettingsFragmentsCallback

open class TestSettingsActivity :
    TestFragmentActivity(),
    SettingsActivityView,
    SettingsFragmentsInvoker,
    FolderChooserFragmentCallback,
    DismissCallback,
    EntrySettingsFragmentCallbacks,
    SupportSettingsFragmentsCallback,
    ProblemFragmentCallback,
    ImprovementFragmentCallback,
    ConnectionTypeSelectionFragmentCallback,
    ConfirmationDialog.Callback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.YaTheme_Preference_Light)
        setSupportActionBar(Toolbar(this))
    }

    override fun dismissLastBackstackFragment() {}

    override fun onAccountIsCurrentChecked(isCurrent: Boolean, intent: Intent?) {}

    override fun showRingtonePicker() {}

    override fun showAddAccountScreen(accountInfoContainers: MutableList<AccountInfoContainer>) {}

    override fun showNotificationSystemSettings(channelDesciption: ChannelSettings?) {}

    override fun showUnsubscribeMenu(uid: Long) {}

    override fun onConfirmedByDialog() {}

    override fun sendIssueFeedback(uid: Long, survey: FeedbackSurvey) {}

    override fun onProblemSelected(
        uid: Long,
        selectedProblem: FeedbackProblem,
        parentProblem: FeedbackProblem?
    ) {
    }

    override fun startProblemFeedback(uid: Long) {}

    override fun startImprovementFeedback(uid: Long) {}

    override fun openFAQLink() {}

    override fun onUnlistedImprovementSelected(uid: Long, improvement: FeedbackImprovement) {}

    override fun onCommonImprovementSent() {}

    override fun dismissFolderChooser() {}

    override fun onFolderChosen(folder: Folder?) {}

    override fun onCacheCleared() {}

    override fun onAvatarChanged() {}

    override fun startAccountSettingsFragment(uid: Long, focusKey: String?) {}

    override fun onNoMoreDisabledLanguages() {}

    override fun showDisabledLanguagesChooserFragment() {}

    override fun showDefaultTargetLanguageChooserFragment() {}

    override fun onVoiceControlChanged() {}

    override fun onVoiceLanguageChanged() {}

    override fun onShowUboxChanged() {}
}
