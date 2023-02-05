package com.yandex.mail.testopithecus.feature

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.feature.impl.AboutSettingsImpl
import com.yandex.mail.testopithecus.feature.impl.AccountSettingsImpl
import com.yandex.mail.testopithecus.feature.impl.AdvancedSearchImpl
import com.yandex.mail.testopithecus.feature.impl.AndroidRootSettingsImpl
import com.yandex.mail.testopithecus.feature.impl.ApplicationRunningStateImpl
import com.yandex.mail.testopithecus.feature.impl.ApplyLabelImpl
import com.yandex.mail.testopithecus.feature.impl.ArchiveMessageShortSwipeMenuImpl
import com.yandex.mail.testopithecus.feature.impl.ClearFolderImpl
import com.yandex.mail.testopithecus.feature.impl.ComposeImpl
import com.yandex.mail.testopithecus.feature.impl.CustomMailServiceLoginImpl
import com.yandex.mail.testopithecus.feature.impl.DeleteMessageImpl
import com.yandex.mail.testopithecus.feature.impl.FilterNavigatorImpl
import com.yandex.mail.testopithecus.feature.impl.FolderNavigatorImpl
import com.yandex.mail.testopithecus.feature.impl.GeneralSettingsImpl
import com.yandex.mail.testopithecus.feature.impl.GroupModeImpl
import com.yandex.mail.testopithecus.feature.impl.LabelNavigatorImpl
import com.yandex.mail.testopithecus.feature.impl.LongSwipeImpl
import com.yandex.mail.testopithecus.feature.impl.ManageableFolderImpl
import com.yandex.mail.testopithecus.feature.impl.ManageableLabelImpl
import com.yandex.mail.testopithecus.feature.impl.MarkableImportantImpl
import com.yandex.mail.testopithecus.feature.impl.MarkableReadImpl
import com.yandex.mail.testopithecus.feature.impl.MessageListDisplayImpl
import com.yandex.mail.testopithecus.feature.impl.MessageViewerAndroidImpl
import com.yandex.mail.testopithecus.feature.impl.MessageViewerImpl
import com.yandex.mail.testopithecus.feature.impl.MoveToFolderImpl
import com.yandex.mail.testopithecus.feature.impl.MultiAccountImpl
import com.yandex.mail.testopithecus.feature.impl.PinImpl
import com.yandex.mail.testopithecus.feature.impl.RootSettingsImpl
import com.yandex.mail.testopithecus.feature.impl.RotatableImpl
import com.yandex.mail.testopithecus.feature.impl.SearchImpl
import com.yandex.mail.testopithecus.feature.impl.ShortSwipeMenuImpl
import com.yandex.mail.testopithecus.feature.impl.SnapshotValidatingImpl
import com.yandex.mail.testopithecus.feature.impl.SpamMessageShortSwipeMenuImpl
import com.yandex.mail.testopithecus.feature.impl.TabsImpl
import com.yandex.mail.testopithecus.feature.impl.TranslatorBarImpl
import com.yandex.mail.testopithecus.feature.impl.TranslatorImpl
import com.yandex.mail.testopithecus.feature.impl.TranslatorLanguageListImpl
import com.yandex.mail.testopithecus.feature.impl.TranslatorLanguageListSearchImpl
import com.yandex.mail.testopithecus.feature.impl.UndoImpl
import com.yandex.mail.testopithecus.feature.impl.ValidationImpl
import com.yandex.mail.testopithecus.feature.impl.YandexLoginImpl
import com.yandex.mail.testopithecus.feature.impl.ZeroSuggestImpl
import com.yandex.xplat.testopithecus.AboutSettingsFeature
import com.yandex.xplat.testopithecus.AccountSettingsFeature
import com.yandex.xplat.testopithecus.AdvancedSearchFeature
import com.yandex.xplat.testopithecus.AndroidGeneralSettingsFeature
import com.yandex.xplat.testopithecus.AndroidRootSettingsFeature
import com.yandex.xplat.testopithecus.ApplicationRunningStateFeature
import com.yandex.xplat.testopithecus.ApplyLabelFeature
import com.yandex.xplat.testopithecus.ArchiveMessageFeature
import com.yandex.xplat.testopithecus.ClearFolderInFolderListFeature
import com.yandex.xplat.testopithecus.ComposeFeature
import com.yandex.xplat.testopithecus.ContextMenuFeature
import com.yandex.xplat.testopithecus.CustomMailServiceLoginFeature
import com.yandex.xplat.testopithecus.DeleteMessageFeature
import com.yandex.xplat.testopithecus.FilterNavigatorFeature
import com.yandex.xplat.testopithecus.FolderNavigatorFeature
import com.yandex.xplat.testopithecus.GeneralSettingsFeature
import com.yandex.xplat.testopithecus.GroupModeFeature
import com.yandex.xplat.testopithecus.LabelNavigatorFeature
import com.yandex.xplat.testopithecus.LongSwipeFeature
import com.yandex.xplat.testopithecus.ManageableFolderFeature
import com.yandex.xplat.testopithecus.ManageableLabelFeature
import com.yandex.xplat.testopithecus.MarkableImportantFeature
import com.yandex.xplat.testopithecus.MarkableReadFeature
import com.yandex.xplat.testopithecus.MessageListDisplayFeature
import com.yandex.xplat.testopithecus.MessageViewerAndroidFeature
import com.yandex.xplat.testopithecus.MessageViewerFeature
import com.yandex.xplat.testopithecus.MoveToFolderFeature
import com.yandex.xplat.testopithecus.MultiAccountFeature
import com.yandex.xplat.testopithecus.PinFeature
import com.yandex.xplat.testopithecus.RootSettingsFeature
import com.yandex.xplat.testopithecus.RotatableFeature
import com.yandex.xplat.testopithecus.SearchFeature
import com.yandex.xplat.testopithecus.ShortSwipeFeature
import com.yandex.xplat.testopithecus.SnapshotValidatingFeature
import com.yandex.xplat.testopithecus.SpamableFeature
import com.yandex.xplat.testopithecus.TabsFeature
import com.yandex.xplat.testopithecus.TranslatorBarFeature
import com.yandex.xplat.testopithecus.TranslatorLanguageListFeature
import com.yandex.xplat.testopithecus.TranslatorLanguageListSearchFeature
import com.yandex.xplat.testopithecus.TranslatorSettingsFeature
import com.yandex.xplat.testopithecus.UndoFeature
import com.yandex.xplat.testopithecus.ValidatorFeature
import com.yandex.xplat.testopithecus.YandexLoginFeature
import com.yandex.xplat.testopithecus.ZeroSuggestFeature
import com.yandex.xplat.testopithecus.common.App
import com.yandex.xplat.testopithecus.common.FeatureID
import com.yandex.xplat.testopithecus.common.FeatureRegistry

class AndroidFeaturesRegistry(private val device: UiDevice) : App {
    override var supportedFeatures = Companion.supportedFeatures

    companion object {
        val supportedFeatures = mutableListOf(
            YandexLoginFeature.get.name,
            MarkableReadFeature.get.name,
            MessageViewerFeature.get.name,
            MessageViewerAndroidFeature.get.name,
            RotatableFeature.get.name,
//            StoriesBlockFeature.get.name,
            GroupModeFeature.get.name,
            FolderNavigatorFeature.get.name,
            MessageListDisplayFeature.get.name,
            MultiAccountFeature.get.name,
            ArchiveMessageFeature.get.name,
            SpamableFeature.get.name,
            ContextMenuFeature.get.name,
            MarkableImportantFeature.get.name,
            LongSwipeFeature.get.name,
            DeleteMessageFeature.get.name,
            LongSwipeFeature.get.name,
            ContextMenuFeature.get.name,
            RootSettingsFeature.get.name,
            GeneralSettingsFeature.get.name,
            AndroidGeneralSettingsFeature.get.name,
            AccountSettingsFeature.get.name,
            AboutSettingsFeature.get.name,
            LabelNavigatorFeature.get.name,
            ShortSwipeFeature.get.name,
            FilterNavigatorFeature.get.name,
            SearchFeature.get.name,
            TabsFeature.get.name,
            SearchFeature.get.name,
            AdvancedSearchFeature.get.name,
            UndoFeature.get.name,
            AndroidRootSettingsFeature.get.name,
            ZeroSuggestFeature.get.name,
            ValidatorFeature.get.name,
            TranslatorLanguageListFeature.get.name,
            TranslatorBarFeature.get.name,
            MoveToFolderFeature.get.name,
            ApplyLabelFeature.get.name,
            TranslatorLanguageListSearchFeature.get.name,
            TranslatorSettingsFeature.get.name,
            ClearFolderInFolderListFeature.get.name,
            ComposeFeature.get.name,
            ManageableFolderFeature.get.name,
            ManageableLabelFeature.get.name,
            CustomMailServiceLoginFeature.get.name,
            PinFeature.get.name,
            ApplicationRunningStateFeature.get.name,
            SnapshotValidatingFeature.get.name
        )
    }

    override fun getFeature(feature: FeatureID): Any {
        return FeatureRegistry()
            .register(YandexLoginFeature.get, YandexLoginImpl(device))
            .register(ArchiveMessageFeature.get, ArchiveMessageShortSwipeMenuImpl(device))
            .register(SpamableFeature.get, SpamMessageShortSwipeMenuImpl(device))
            .register(MarkableReadFeature.get, MarkableReadImpl(device))
            .register(MessageViewerFeature.get, MessageViewerImpl(device))
            .register(MessageViewerAndroidFeature.get, MessageViewerAndroidImpl(device))
            .register(GroupModeFeature.get, GroupModeImpl(device))
            .register(FolderNavigatorFeature.get, FolderNavigatorImpl(device))
            .register(RotatableFeature.get, RotatableImpl(device))
//            .register(StoriesBlockFeature.get, StoriesBlockImpl(device))
            .register(MessageListDisplayFeature.get, MessageListDisplayImpl(device))
            .register(MultiAccountFeature.get, MultiAccountImpl(device))
            .register(ContextMenuFeature.get, ShortSwipeMenuImpl(device))
            .register(MarkableImportantFeature.get, MarkableImportantImpl(device))
            .register(LongSwipeFeature.get, LongSwipeImpl(device))
            .register(LabelNavigatorFeature.get, LabelNavigatorImpl(device))
            .register(DeleteMessageFeature.get, DeleteMessageImpl(device))
            .register(RootSettingsFeature.get, RootSettingsImpl(device))
            .register(GeneralSettingsFeature.get, GeneralSettingsImpl(device))
            .register(AndroidGeneralSettingsFeature.get, GeneralSettingsImpl(device))
            .register(AccountSettingsFeature.get, AccountSettingsImpl(device))
            .register(AboutSettingsFeature.get, AboutSettingsImpl(device))
            .register(FilterNavigatorFeature.get, FilterNavigatorImpl(device))
            .register(ShortSwipeFeature.get, ShortSwipeMenuImpl(device))
            .register(SearchFeature.get, SearchImpl(device))
            .register(AdvancedSearchFeature.get, AdvancedSearchImpl(device))
            .register(TabsFeature.get, TabsImpl(device))
            .register(UndoFeature.get, UndoImpl(device))
            .register(AndroidRootSettingsFeature.get, AndroidRootSettingsImpl(device))
            .register(ZeroSuggestFeature.get, ZeroSuggestImpl(device))
            .register(ValidatorFeature.get, ValidationImpl(device))
            .register(TranslatorLanguageListFeature.get, TranslatorLanguageListImpl(device))
            .register(TranslatorBarFeature.get, TranslatorBarImpl(device))
            .register(MoveToFolderFeature.get, MoveToFolderImpl(device))
            .register(ApplyLabelFeature.get, ApplyLabelImpl(device))
            .register(TranslatorLanguageListSearchFeature.get, TranslatorLanguageListSearchImpl(device))
            .register(TranslatorSettingsFeature.get, TranslatorImpl(device))
            .register(ClearFolderInFolderListFeature.get, ClearFolderImpl(device))
            .register(ComposeFeature.get, ComposeImpl(device))
            .register(ManageableFolderFeature.get, ManageableFolderImpl(device))
            .register(ManageableLabelFeature.get, ManageableLabelImpl(device))
            .register(CustomMailServiceLoginFeature.get, CustomMailServiceLoginImpl(device))
            .register(PinFeature.get, PinImpl(device))
            .register(ApplicationRunningStateFeature.get, ApplicationRunningStateImpl(device))
            .register(SnapshotValidatingFeature.get, SnapshotValidatingImpl(device))
            .get(feature)
    }

    override fun dump(model: App): String {
        return ""
    }
}
