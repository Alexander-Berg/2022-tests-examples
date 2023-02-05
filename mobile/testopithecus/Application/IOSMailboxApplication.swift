//
// Created by Fedor Amosov on 2019-08-02.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public class IOSMailboxApplication: App {
    private let oauthService: OauthService
    public init(_ oauthService: OauthService) {
        self.oauthService = oauthService
    }

    public static let staticSupportedFeatures: YSArray<FeatureID> = [
        MarkableReadFeature.get.name,
        MarkableImportantFeature.get.name,
        MessageListDisplayFeature.get.name,
        YandexLoginFeature.get.name,
        FolderNavigatorFeature.get.name,
        GroupModeFeature.get.name,
        MessageViewerFeature.get.name,
        DeleteMessageFeature.get.name,
        ShortSwipeFeature.get.name,
        ArchiveMessageFeature.get.name,
        ContextMenuFeature.get.name,
        GeneralSettingsFeature.get.name,
        IosGeneralSettingsFeature.get.name,
        RootSettingsFeature.get.name,
        AccountSettingsFeature.get.name,
        IosAccountSettingsFeature.get.name,
        AboutSettingsFeature.get.name,
        RotatableFeature.get.name,
        LabelNavigatorFeature.get.name,
        FilterNavigatorFeature.get.name,
        LongSwipeFeature.get.name,
        SearchFeature.get.name,
        ZeroSuggestFeature.get.name,
        MultiAccountFeature.get.name,
        UndoFeature.get.name,
        PinFeature.get.name,
        ApplicationRunningStateFeature.get.name,
        ManageableFolderFeature.get.name,
        ManageableLabelFeature.get.name,
        AccountsListFeature.get.name,
        CustomMailServiceLoginFeature.get.name,
        IOSRootSettingsFeature.get.name,
        ThreadViewNavigatorFeature.get.name,
        IOSRootSettingsFeature.get.name,
        TranslatorBarFeature.get.name,
        TranslatorLanguageListFeature.get.name,
        TranslatorLanguageListSearchFeature.get.name,
        TranslatorSettingsFeature.get.name,
        QuickReplyFeature.get.name,
        SmartReplyFeature.get.name,
        MoveToFolderFeature.get.name,
        ApplyLabelFeature.get.name,
        ClearFolderInFolderListFeature.get.name,
        TabBarFeature.get.name,
        TabBarIOSFeature.get.name,
        ShtorkaFeature.get.name,
        ShtorkaIOSFeature.get.name,
        SnapshotValidatingFeature.get.name,
        ComposeRecipientFieldsFeature.get.name,
        ComposeRecipientSuggestFeature.get.name,
        ComposeSenderSuggestFeature.get.name,
        ComposeSubjectFeature.get.name,
        ComposeBodyFeature.get.name,
        ComposeFeature.get.name,
        FiltersListFeature.get.name
    ]

    public var supportedFeatures: YSArray<FeatureID> = IOSMailboxApplication.staticSupportedFeatures

    public func getFeature(_ feature: FeatureID) -> Any {
        FeatureRegistry()
                .register(MarkableReadFeature.get, MarkableApplication())
                .register(MarkableImportantFeature.get, MarkableImportantApplication())
                .register(MessageListDisplayFeature.get, MessageListDisplayApplication())
                .register(YandexLoginFeature.get, YandexLoginApplication())
                .register(FolderNavigatorFeature.get, FolderNavigatorApplication())
                .register(GroupModeFeature.get, GroupModeApplication())
                .register(MessageViewerFeature.get, MessageNavigatorApplication())
                .register(DeleteMessageFeature.get, DeleteMessageApplication())
                .register(ShortSwipeFeature.get, ShortSwipeApplication())
                .register(LongSwipeFeature.get, LongSwipeApplication())
                .register(ArchiveMessageFeature.get, ArchiveMessageApplication())
                .register(ContextMenuFeature.get, ContextMenuApplication())
                .register(GeneralSettingsFeature.get, GeneralSettingsApplication())
                .register(IosGeneralSettingsFeature.get, GeneralSettingsApplication())
                .register(AccountSettingsFeature.get, AccountSettingsApplication())
                .register(IosAccountSettingsFeature.get, AccountSettingsApplication())
                .register(RootSettingsFeature.get, RootSettingsApplication())
                .register(IOSRootSettingsFeature.get, RootSettingsApplication())
                .register(AboutSettingsFeature.get, AboutSettingsApplication())
                .register(RotatableFeature.get, RotatableApplication())
                .register(LabelNavigatorFeature.get, LabelNavigatorApplication())
                .register(FilterNavigatorFeature.get, FilterNavigatorApplication())
                .register(SearchFeature.get, SearchApplication())
                .register(ZeroSuggestFeature.get, ZeroSuggestApplication())
                .register(MultiAccountFeature.get, AccountSwitcherApplication())
                .register(UndoFeature.get, UndoApplication())
                .register(PinFeature.get, PinApplication())
                .register(ApplicationRunningStateFeature.get, ApplicationRunningStateApplication())
                .register(ManageableFolderFeature.get, ManageFoldersApplication())
                .register(ManageableLabelFeature.get, ManageLabelsApplication())
                .register(AccountsListFeature.get, AccountsListApplication())
                .register(CustomMailServiceLoginFeature.get, CustomMailServiceLoginApplication())
                .register(ThreadViewNavigatorFeature.get, ThreadNavigatorApplication())
                .register(TranslatorBarFeature.get, TranslatorApplication())
                .register(TranslatorLanguageListFeature.get, TranslatorApplication())
                .register(TranslatorLanguageListSearchFeature.get, TranslatorApplication())
                .register(TranslatorSettingsFeature.get, TranslatorApplication())
                .register(QuickReplyFeature.get, QuickReplyApplication())
                .register(SmartReplyFeature.get, QuickReplyApplication())
                .register(MoveToFolderFeature.get, MoveToFolderApplication())
                .register(ApplyLabelFeature.get, ApplyLabelApplication())
                .register(ClearFolderInFolderListFeature.get, ClearFolderApplication())
                .register(TabBarFeature.get, TabBarApplication())
                .register(TabBarIOSFeature.get, TabBarApplication())
                .register(ShtorkaFeature.get, ShtorkaApplication())
                .register(ShtorkaIOSFeature.get, ShtorkaApplication())
                .register(SnapshotValidatingFeature.get, SnapshotValidatingApplication())
                .register(ComposeRecipientFieldsFeature.get, ComposeApplication())
                .register(ComposeRecipientSuggestFeature.get, ComposeApplication())
                .register(ComposeSenderSuggestFeature.get, ComposeApplication())
                .register(ComposeSubjectFeature.get, ComposeApplication())
                .register(ComposeBodyFeature.get, ComposeApplication())
                .register(ComposeFeature.get, ComposeApplication())
                .register(FiltersListFeature.get, FiltersApplication())
                .get(feature)
    }

    public func dump(_ model: App) -> String {
        ""
    }
}
