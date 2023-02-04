require('ts-node').register();

const bemPageObject = require('bem-page-object'),
    Entity = bemPageObject.Entity,
    pageObject = {},
    apiVersion = require('../src/configs/testing/ymaps').default.default.url.match(/\d.+/),
    ymaps = 'ymaps-' + apiVersion.toString().replace(/\D/g, '-');

pageObject.tilesLoaded = new Entity('tiles-loaded');

pageObject.group = new Entity({ block: 'nk-form', elem: 'group' });
pageObject.group1 = pageObject.group.nthChild(1);
pageObject.group2 = pageObject.group.nthChild(2);
pageObject.group3 = pageObject.group.nthChild(3);
pageObject.group4 = pageObject.group.nthChild(4);

pageObject.control = new Entity({ block: 'nk-form', elem: 'control' });
pageObject.control1 = pageObject.control.nthChild(1);
pageObject.control2 = pageObject.control.nthChild(2);
pageObject.control3 = pageObject.control.nthChild(3);
pageObject.control4 = pageObject.control.nthChild(4);
pageObject.control5 = pageObject.control.nthChild(5);
pageObject.control6 = pageObject.control.nthChild(6);
pageObject.control7 = pageObject.control.nthChild(7);

pageObject.button = new Entity('button');

pageObject.nkButton = new Entity({ block: 'nk-button' });
pageObject.nkButtonChecked = new Entity({ block: 'nk-button', modName: 'checked' });
pageObject.nkButtonDisabled = new Entity({ block: 'nk-button', modName: 'disabled' });

pageObject.tab = new Entity({ block: 'nk-tabs-bar', elem: 'tab' });

pageObject.nkGrid = new Entity({ block: 'nk-grid' });
pageObject.nkGridCol = new Entity({ block: 'nk-grid', elem: 'col' });

pageObject.input = new Entity('input');
pageObject.textInput = new Entity({ block: 'nk-text-input' });
pageObject.nkTextInputControl = new Entity({ block: 'nk-text-input', elem: 'control' });
pageObject.textInputControl = new Entity({ block: 'nk-form' })
    .descendant(pageObject.nkTextInputControl);
pageObject.textInputFocused = new Entity({ block: 'nk-text-input', modName: 'focused' });
pageObject.textInput.clear = new Entity({ block: 'nk-text-input', elem: 'clear' });
pageObject.textInput.clearVisible = new Entity({ block: 'nk-text-input', elem: 'clear', modName: 'visible' });
pageObject.textareaControl = new Entity({ block: 'nk-text-area', elem: 'control' });

pageObject.submit = new Entity({ block: 'nk-form-submit-view', elem: 'submit' });
pageObject.submitDisabled = pageObject.submit.mix(pageObject.nkButtonDisabled);

pageObject.link = new Entity({ block: 'nk-form' }).descendant(new Entity({ block: 'nk-link' }));

pageObject.suggest = new Entity({ block: 'nk-suggest', elem: 'popup' });
pageObject.suggestItems = new Entity({ block: 'nk-suggest', elem: 'items' });
pageObject.suggestTitle = new Entity({ block: 'nk-suggest', elem: 'group-title' });
pageObject.suggestItem = new Entity({ block: 'nk-suggest', elem: 'item' });

pageObject.checkbox = new Entity({ block: 'checkbox' });
pageObject.checkboxBox = new Entity({ block: 'checkbox', elem: 'box' });
pageObject.checkboxButton = pageObject.checkbox.mods({ type: 'button' });

pageObject.nkCheckbox = new Entity({ block: 'nk-checkbox' });
pageObject.nkCheckboxBox = new Entity({ block: 'nk-checkbox', elem: 'box' });
pageObject.nkCheckboxControl = new Entity({ block: 'nk-checkbox-control' });
pageObject.nkCheckboxControl1 = pageObject.nkCheckboxControl.nthChild(1);
pageObject.nkCheckboxControl2 = pageObject.nkCheckboxControl.nthChild(2);
pageObject.nkCheckboxControl3 = pageObject.nkCheckboxControl.nthChild(3);
pageObject.nkCheckboxControl4 = pageObject.nkCheckboxControl.nthChild(4);
pageObject.nkCheckboxControl5 = pageObject.nkCheckboxControl.nthChild(5);
pageObject.nkCheckboxControl6 = pageObject.nkCheckboxControl.nthChild(6);

pageObject.checkboxGroupItem = new Entity({ block: 'nk-checkbox-group', elem: 'item' });
pageObject.checkboxGroupItem1 = pageObject.checkboxGroupItem.nthChild(1);
pageObject.checkboxGroupItem2 = pageObject.checkboxGroupItem.nthChild(2);
pageObject.checkboxGroupItem3 = pageObject.checkboxGroupItem.nthChild(3);

pageObject.toolbarItem = new Entity ({ block: 'nk-on-map-toolbar', elem: 'item' });

pageObject.icon = new Entity({ block: 'nk-icon' });
pageObject.changeCategoryIcon = pageObject.icon.mods({ id: 'change-category' });
pageObject.closeIcon = pageObject.icon.mods({ id: 'close' });
pageObject.closeIconBig = pageObject.icon.mods({ id: 'close-big' });
pageObject.closeIconSmall = pageObject.icon.mods({ id: 'close-small' });
pageObject.deleteIcon = pageObject.icon.mods({ id: 'delete' });
pageObject.deleteIconSmall = pageObject.icon.mods({ id: 'delete-small' });
pageObject.editIcon = pageObject.icon.mods({ id: 'edit' });
pageObject.feedIcon = pageObject.icon.mods({ id: 'feed' });
pageObject.helpIcon = pageObject.icon.mods({ id: 'help' });
pageObject.linkIcon = pageObject.icon.mods({ id: 'link' });
pageObject.moreIcon = pageObject.icon.mods({ id: 'more' });
pageObject.redrawIcon = pageObject.icon.mods({ id: 'redraw' });
pageObject.redoIcon = pageObject.icon.mods({ id: 'redo' });
pageObject.rulerIcon = pageObject.icon.mods({ id: 'ruler' });
pageObject.splitIcon = pageObject.icon.mods({ id: 'split' });
pageObject.undoIcon = pageObject.icon.mods({ id: 'undo' });

pageObject.pedestrianIcon = pageObject.icon.mods({ id: 'access-1' });
pageObject.busIcon = pageObject.icon.mods({ id: 'access-2' });
pageObject.truckIcon = pageObject.icon.mods({ id: 'access-4' });
pageObject.autoIcon = pageObject.icon.mods({ id: 'access-8' });
pageObject.bikeIcon = pageObject.icon.mods({ id: 'access-16' });
pageObject.taxiIcon = pageObject.icon.mods({ id: 'access-32' });

pageObject.actionExpand = new Entity({ block: 'nk-labeled-list-layout', elem: 'expand-control' })
    .descendant(pageObject.moreIcon);
pageObject.addCenter = pageObject.icon.mods({ id: 'contour-edit-center-mode' });
pageObject.advancedMode = pageObject.icon.mods({ id: 'settings' });
pageObject.advancedModeDisabled = pageObject.nkButtonDisabled.descendant(pageObject.advancedMode);
pageObject.alignAngles = pageObject.icon.mods({ id: 'align-angles' });
pageObject.arrow = pageObject.icon.mods({ id: 'arrow' });
pageObject.circle = pageObject.icon.mods({ id: 'circle' });
pageObject.contourSelectMode = pageObject.icon.mods({ id: 'contour-edit-content-select-mode' });
pageObject.contourSelector = pageObject.icon.mods({ id: 'contour-edit-selector-mode' });
pageObject.editAddExternalContour = pageObject.icon.mods({ id: 'contour-edit-add-external-contour' });
pageObject.editAddExternalContourDisabled = pageObject.nkButtonDisabled.descendant(pageObject.editAddExternalContour);
pageObject.editorHelp = pageObject.icon.mods({ id: 'editor-help' });
pageObject.framing = pageObject.icon.mods({ id: 'framing' });
pageObject.roundAngles = pageObject.icon.mods({ id: 'round-angles' });
pageObject.snappingToGeometry = pageObject.icon.mods({ id: 'snapping-to-geometry' });
pageObject.snappingToRightGeometry = pageObject.icon.mods({ id: 'snapping-to-right-angle' });

pageObject.nkLink = new Entity({ block: 'nk-link' });
pageObject.nkLinkViewAction = new Entity({ block: 'nk-link_view_action' });
pageObject.nkMenuItem = new Entity({ block: 'nk-menu-item' });
pageObject.menuItem = new Entity({ block: 'menu-item' });

pageObject.action = new Entity({ block: 'nk-action' });
pageObject.actionAdd = pageObject.action.mods({ action: 'add' });
pageObject.actionEdit = pageObject.action.mods({ action: 'edit' });
pageObject.actionSubmit = pageObject.action.mods({ action: 'submit' });
pageObject.actionCancel = pageObject.action.mods({ action: 'cancel' });
pageObject.actionRemove = pageObject.action.mods({ action: 'remove' });
pageObject.actionRevert = pageObject.action.mods({ action: 'revert' });
pageObject.actionAlignAngles = pageObject.action.mods({ action: 'align-angles' });
pageObject.actionLink = pageObject.action.mods({ type: 'link' });

pageObject.inputCtrl = new Entity({ block: 'nk-text-input-control' });
pageObject.inputControl = pageObject.inputCtrl.descendant(pageObject.textInput);
pageObject.inputControl.input = pageObject.input.copy();
pageObject.inputCtrl1 = pageObject.inputCtrl.nthChild(1);
pageObject.inputCtrl2 = pageObject.inputCtrl.nthChild(2);
pageObject.inputCtrl3 = pageObject.inputCtrl.nthChild(3);

pageObject.textareaCtrl = new Entity({ block: 'nk-text-area-control' });
pageObject.textarea = pageObject.textareaCtrl.descendant(pageObject.textareaControl);

pageObject.select = new Entity({ block: 'nk-select' });
pageObject.select.button = pageObject.button.copy();
pageObject.select1 = pageObject.select.nthChild(1);
pageObject.select1.button = pageObject.button.copy();
pageObject.select2 = pageObject.select.nthChild(2);
pageObject.select2.button = pageObject.button.copy();
pageObject.select3 = pageObject.select.nthChild(3);
pageObject.select4 = pageObject.select.nthChild(4);
pageObject.select4.button = pageObject.button.copy();
pageObject.select5 = pageObject.select.nthChild(5);
pageObject.select6 = pageObject.select.nthChild(6);
pageObject.select6.button = pageObject.button.copy();

pageObject.ymaps = new Entity('ymaps');
pageObject.ymapsItem0 = new Entity('ymaps[item-index="0"]');
pageObject.ymapsItem1 = new Entity('ymaps[item-index="1"]');
pageObject.ymapsItem2 = new Entity('ymaps[item-index="2"]');
pageObject.ymapsItem3 = new Entity('ymaps[item-index="3"]');
pageObject.ymapsItem4 = new Entity('ymaps[item-index="4"]');
pageObject.ymapsItem5 = new Entity('ymaps[item-index="5"]');
pageObject.ymaps.balloon = new Entity({ block: ymaps + 'balloon' });
pageObject.ymaps.balloon.closeBtn = new Entity({ block: ymaps + 'balloon', elem: 'close-button' });
pageObject.ymaps.rulerDist = new Entity({ block: ymaps + 'ruler', elem: 'dist' });

pageObject.radioGroup = new Entity({ block: 'nk-radio-group-control' });
pageObject.radioButton = new Entity({ block: 'nk-radio', type: 'button' });
pageObject.radioButton1 = pageObject.radioButton.nthChild(1);
pageObject.radioButton2 = pageObject.radioButton.nthChild(2);
pageObject.radioButton3 = pageObject.radioButton.nthChild(3);
pageObject.radioButton4 = pageObject.radioButton.nthChild(4);
pageObject.radioButton5 = pageObject.radioButton.nthChild(5);

pageObject.section = new Entity({ block: 'nk-section' });
pageObject.section1 = pageObject.section.mods({ level: '1' });
pageObject.section2 = pageObject.section.mods({ level: '2' });
pageObject.section2.select = pageObject.select.copy();

pageObject.headerView = new Entity({ block: 'nk-header-view' });

pageObject.listItemGroup = new Entity({ block: 'nk-list-item-group-view' });
pageObject.listItemView = new Entity({ block: 'nk-list-item-view' });
pageObject.listItemExpanded = new Entity({ block: 'nk-list-item-view_expanded' });

pageObject.appView = new Entity({ block: 'nk-app-view' });
pageObject.appView.map = new Entity({ block: 'nk-app-view', elem: 'map' });

pageObject.appBarView = new Entity({ block: 'nk-app-bar-view' });
pageObject.appBarView.btn = new Entity({ block: 'nk-app-bar-view', elem: 'button' });
pageObject.appBarView.create = new Entity({ block: 'nk-category-selector-control-view' });

pageObject.linkPanelView = new Entity({ block: 'nk-link-panel-view' });

pageObject.sidebarView = new Entity({ block: 'nk-sidebar-view' }).not(new Entity('[style]'));
pageObject.sizeObserver = new Entity({ block: 'nk-size-observer' });
pageObject.sidebarView.ctrl = new Entity({ block: 'nk-sidebar-control' });
pageObject.sidebarView.island = new Entity({ block: 'nk-island' });
pageObject.sidebarView.island1 = pageObject.sidebarView.island.nthChild(1);
pageObject.sidebarView.island2 = pageObject.sidebarView.island.nthChild(2);
pageObject.sidebarView.island2.listItemView = pageObject.listItemView.copy();
pageObject.sidebarViewIsland = pageObject.sidebarView.mix(new Entity({ block: 'nk-island' }));
pageObject.sidebarHeader = new Entity({ block: 'nk-sidebar-header-view' });
pageObject.sidebarHeader.close = pageObject.icon.mods({ id: 'close' });
pageObject.sidebarHeaderTitle = new Entity({ block: 'nk-sidebar-header-view', elem: 'title-text' });

pageObject.geoObjViewerView = new Entity({ block: 'nk-geoobject-viewer-view' })
    .descendant(pageObject.sidebarView.island1);
pageObject.scrollable = new Entity({ block: 'nk-scrollable' });
pageObject.geoObjViewerView.container = new Entity({ block: 'nk-scrollable', elem: 'container' });
pageObject.geoObjViewerView.commentsLink = new Entity({ block: 'nk-slide-animation' })
    .descendant(pageObject.linkPanelView);
pageObject.geoObjViewerView.historyLink = pageObject.section1.mix(pageObject.linkPanelView);
pageObject.geoObjViewerView.historyLink.date = new Entity({ block: 'nk-date-time' });
pageObject.geoObjViewerView.notice = new Entity({ block: 'nk-geoobject-viewer-view', elem: 'notice' })
    .mods({ type: 'deleted' });
pageObject.geoObjViewerView.close = pageObject.icon.mods({ id: 'close' });
pageObject.geoObjViewerView.deleted = new Entity({ block: 'nk-geoobject-notice-view' }).mods({ type: 'deleted' });

pageObject.geoObjCommitsViewFooter = new Entity({ block: 'nk-geoobject-commits-view', elem: 'footer' });

pageObject.geoObjEditorView = new Entity({ block: 'nk-geoobject-editor-view' })
    .descendant(pageObject.sizeObserver);
pageObject.geoObjEditorView.ftType = new Entity({ block: 'nk-primary-type-edit-control' })
    .descendant(pageObject.button);
pageObject.geoObjEditorView.submit = pageObject.submit.copy();
pageObject.geoObjEditorView.submitDisabled = pageObject.submitDisabled.copy();
pageObject.geoObjEditorView.close = pageObject.icon.mods({ id: 'close' });
pageObject.geoObjEditorView.addLinkInput = pageObject.textInput.copy();
pageObject.geoObjEditorView.addLinkInput.clear = pageObject.textInput.clear.copy();
pageObject.geoObjEditorView.condition = pageObject.group2.descendant(pageObject.button);
pageObject.geoObjEditorView.radioGroup1 = pageObject.radioGroup.nthChild(1);
pageObject.geoObjEditorView.radioGroup2 = pageObject.radioGroup.nthChild(2);

pageObject.categoryView = pageObject.sidebarView.copy();
pageObject.categoryView.nkIsland = pageObject.sidebarView.island1.copy();
pageObject.categoryView.lastUsed = pageObject.sidebarView.island2.copy();
pageObject.categorySelectorGroupsView = new Entity({ block: 'nk-category-selector-groups-view' });
pageObject.categorySelectorGroupsView.group = new Entity({ block: 'nk-category-selector-groups-view', elem: 'group' });

pageObject.suggestCategory = new Entity({ block: 'nk-category-selector-view', elem: 'suggest' });
pageObject.suggestCategory.input = pageObject.nkTextInputControl.copy();
pageObject.suggestCategory.textInputFocused = pageObject.textInputFocused.copy();

pageObject.geoObjMasterEditorView = new Entity({ block: 'nk-geoobject-master-editor-view' });
pageObject.geoObjMasterEditorViewSuggest = new Entity({ block: 'nk-geoobject-master-editor-view', elem: 'suggest' });
pageObject.geoObjMasterEditorViewSuggest.input = pageObject.input.copy();

pageObject.geoObjMastersEditorView = new Entity({ block: 'nk-geoobject-masters-editor-view' });
pageObject.geoObjMastersEditorViewSuggest = new Entity({
    block: 'nk-geoobject-masters-editor-view',
    elem: 'suggest'
});
pageObject.geoObjMastersEditorViewSuggest.input = pageObject.input.copy();
pageObject.geoObjMasterEditorViewSelect = new Entity({ block: 'nk-geoobject-master-editor-view', elem: 'select' });

pageObject.shareView = new Entity({ block: 'nk-share-view' });

pageObject.eventView = new Entity({ block: 'nk-event-view' });
pageObject.eventView.date = new Entity({ block: 'nk-event-view', elem: 'date' });
pageObject.eventViewGeoObjLinks = new Entity({ block: 'nk-event-view', elem: 'geoobject-links' });
pageObject.eventViewGeoObjLink = new Entity({ block: 'nk-event-view', elem: 'geoobject-link' });

pageObject.geoObjLinkView = new Entity({ block: 'nk-geoobject-link-view' });
pageObject.gotoObjHistory = pageObject.eventViewGeoObjLink.nthChild(1);

pageObject.commitView = new Entity({ block: 'nk-geoobject-commit-view' });
pageObject.commitViewCommit = new Entity({ block: 'nk-geoobject-commit-view', elem: 'commit' });
pageObject.commitView.date = new Entity({ block: 'nk-geoobject-commit-view', elem: 'date' });
pageObject.commitView.state = new Entity({ block: 'nk-geoobject-commit-view', elem: 'state' });
pageObject.commitView.button = pageObject.button.copy();
pageObject.commitDiffView = new Entity({ block: 'nk-commit-diff-view' });
pageObject.commitDiffViewRelatedGeoObj = new Entity({ block: 'nk-commit-diff-view', elem: 'related-geoobject' });
pageObject.commitDiffViewRelatedGeoObjLink = pageObject.commitDiffViewRelatedGeoObj.descendant(pageObject.nkLink);

pageObject.geoObjRelsView = new Entity({ block: 'nk-geoobject-relations-view' });
pageObject.geoObjRelsViewItem = new Entity({ block: 'nk-geoobject-relations-view', elem: 'item' });
pageObject.geoObjRelsView.link = pageObject.nkLink.copy();
pageObject.geoObjRelsViewItem.link = pageObject.nkLink.copy();
pageObject.geoObjRelsView1 = pageObject.geoObjRelsView.nthType(1);
pageObject.geoObjRelsView2 = pageObject.geoObjRelsView.nthType(2);
pageObject.geoObjRelsView3 = pageObject.geoObjRelsView.nthType(3);
pageObject.geoObjRelsViewAction = new Entity({ block: 'nk-geoobject-relations-view', elem: 'action' });
pageObject.geoObjRelsViewSpinner = new Entity({ block: 'nk-geoobject-relations-view', elem: 'spinner' });
pageObject.geoObjRelsAddEntrance = pageObject.geoObjRelsView.nthChild(3).descendant(pageObject.nkLinkViewAction);
pageObject.geoObjRelsEntrance1 = pageObject.geoObjRelsView.nthChild(3).descendant(pageObject.nkLink.nthChild(1));
pageObject.geoObjRelsEntrance2 = pageObject.geoObjRelsView.nthChild(3).descendant(pageObject.nkLink.nthChild(2));
pageObject.geoObjRelsAddAddress = pageObject.geoObjRelsView.nthChild(2).descendant(pageObject.nkLinkViewAction);
pageObject.geoObjRelsAddress1 = pageObject.geoObjRelsView.nthChild(2).descendant(pageObject.nkLink.nthChild(1));
pageObject.geoObjRelsAddress2 = pageObject.geoObjRelsView.nthChild(2).descendant(pageObject.nkLink.nthChild(2));
pageObject.geoObjRelsAddArrivalPoint = pageObject.geoObjRelsView.nthChild(4).descendant(pageObject.nkLinkViewAction);
pageObject.geoObjRelsArrivalPoint = pageObject.geoObjRelsView.nthChild(4).descendant(pageObject.geoObjRelsViewItem.link);
pageObject.geoObjRelsAddCondLane = pageObject.geoObjRelsView.nthChild(4).descendant(pageObject.nkLinkViewAction);
pageObject.geoObjRelsCondLane = pageObject.geoObjRelsView.nthChild(4).descendant(pageObject.geoObjRelsViewItem);
pageObject.geoObjRelsCondAnnotation = pageObject.geoObjRelsView.nthChild(5).descendant(pageObject.geoObjRelsViewItem);
pageObject.geoObjRelsCondClosure = pageObject.geoObjRelsView.nthChild(6).descendant(pageObject.geoObjRelsViewItem);
pageObject.geoObjRelsCondToll = pageObject.geoObjRelsView.nthChild(7).descendant(pageObject.geoObjRelsViewItem);

pageObject.junctionCondsViewItem = new Entity({ block: 'nk-junction-conds-viewer-view', elem: 'item' });
pageObject.junctionCondsViewItem.link = pageObject.nkLink.copy();
pageObject.junctionCondsViewAdd = new Entity({ block: 'nk-junction-conds-viewer-view', elem: 'add' }).descendant(pageObject.nkLink);

pageObject.orgSummaryViewerViewItem = new Entity({
    block: 'nk-geoobject-organizations-summary-viewer-view',
    elem: 'item'
});
pageObject.orgSummaryViewerViewItem.addLink = pageObject.nkLinkViewAction.copy();
pageObject.orgSummaryViewerViewItem.nkLink = pageObject.nkLink.copy();
pageObject.orgSummaryViewerViewOrg1 = pageObject.orgSummaryViewerViewItem.nthChild(2).descendant(pageObject.nkLink);
pageObject.orgSummaryViewerViewOrg2 = pageObject.orgSummaryViewerViewItem.nthChild(3).descendant(pageObject.nkLink);

pageObject.geoObjOrgView = new Entity({ block: 'nk-geoobject-organizations-view' });
pageObject.geoObjOrgViewButton = new Entity({ block: 'nk-geoobject-organizations-view', elem: 'add-button' });
pageObject.geoObjOrgView.button = pageObject.button.copy();
pageObject.geoObjOrgView.close = pageObject.icon.mods({ id: 'close' });

pageObject.relatedGeoObjTemplatesView = new Entity({ block: 'nk-related-geoobject-templates-view' });
pageObject.relatedGeoObjTemplatesView.item = pageObject.listItemView.copy();

pageObject.businessEditorView = new Entity({ block: 'nk-business-editor-view' });
pageObject.businessEditorViewSuggest = new Entity({ block: 'nk-business-editor-view', elem: 'business-inputs' });
pageObject.businessEditorViewSuggest.input = pageObject.textInput.copy();

pageObject.businessMainRubricEditorViewSuggest = new Entity({ block: 'nk-business-main-rubric-editor-view', elem: 'suggest' });
pageObject.businessMainRubricEditorViewSuggest.input = pageObject.textInput.copy();

pageObject.createOrgView = new Entity({ block: 'nk-create-organization-view' });

pageObject.helpView = new Entity({ block: 'nk-help-view' });
pageObject.helpView.header = pageObject.sidebarHeader.copy();
pageObject.helpView.close = pageObject.icon.mods({ id: 'close' });
pageObject.helpViewOnboarding = new Entity({ block: 'nk-help-onboarding-view' });
pageObject.helpViewVideo = new Entity({ block: 'nk-help-video-view' });
pageObject.helpViewVideoPreview = new Entity({ block: 'nk-help-video-view', elem: 'preview' });
pageObject.helpViewVideoPlayer = new Entity({ block: 'nk-help-video-view', elem: 'video' });
pageObject.helpViewVideoPlayerClose = new Entity({ block: 'nk-help-video-view', elem: 'video-close' });
pageObject.helpViewLive = new Entity({ block: 'nk-help-live-view' });
pageObject.helpViewLiveMap = new Entity({ block: 'nk-help-live-view', elem: 'mini-map' });
pageObject.helpViewLiveGeoObjIcon = new Entity({ block: 'nk-help-live-view', elem: 'geoobject-icon' });
pageObject.helpViewLiveEventLinks = new Entity({ block: 'nk-help-live-view', elem: 'event-links' });
pageObject.helpViewPrimaryLinks = new Entity({ block: 'nk-help-links-view' });
pageObject.helpViewPrimaryLink = new Entity({ block: 'nk-help-links-view', elem: 'link' });
pageObject.helpViewPrimaryLinks.club = pageObject.helpViewPrimaryLink.mods({ id: 'club' });
pageObject.helpViewPrimaryLinks.modClub = pageObject.helpViewPrimaryLink.mods({ id: 'moderator-club' });
pageObject.helpViewPrimaryLinks.vk = pageObject.helpViewPrimaryLink.mods({ id: 'vkontakte' });
pageObject.helpViewPrimaryLinks.rules = pageObject.helpViewPrimaryLink.mods({ id: 'rules' });
pageObject.helpViewPrimaryLinks.rulesChanges = pageObject.helpViewPrimaryLink.mods({ id: 'rules-changes' });
pageObject.helpViewAdditionalLinks = new Entity({ block: 'nk-help-view', elem: 'additional-links' });
pageObject.helpViewAdditionalLink = new Entity({ block: 'nk-help-view', elem: 'additional-link' });
pageObject.helpViewAdditionalLinks.agreement = pageObject.helpViewAdditionalLink.nthChild(2);

pageObject.listCtrl = new Entity({ block: 'nk-list-edit-control' });
pageObject.listCtrlItem = new Entity({ block: 'nk-list-edit-control', elem: 'item' });
pageObject.listCtrl.controlAdd = pageObject.nkLink.copy();
pageObject.listCtrl.textInputControl = pageObject.nkTextInputControl.copy();
pageObject.listCtrl.listItems = new Entity({ block: 'nk-editable-list-control', elem: 'list-items' });
pageObject.listCtrl.listItems.inputControl = new Entity({ block: 'input', elem: 'control' });
pageObject.listCtrl.listItems.input = pageObject.input.copy();

pageObject.timeRangeEdit = new Entity({ block: 'nk-time-range-row-edit-layout', elem: 'row' });
pageObject.fieldsetTimeStart = pageObject.timeRangeEdit.nthChild(1).descendant(pageObject.nkGridCol.nthChild(1))
    .descendant(pageObject.input);
pageObject.fieldsetTimeEnd = new Entity({ block: 'nk-time-range-row-edit-layout', elem: 'time-end'})
    .descendant(pageObject.input);
pageObject.fieldsetDateStart = pageObject.timeRangeEdit.nthChild(2).descendant(pageObject.nkGridCol.nthChild(1))
    .descendant(pageObject.input);
pageObject.fieldsetDateEnd = new Entity({ block: 'nk-time-range-row-edit-layout', elem: 'date-end'})
    .descendant(pageObject.input);

pageObject.fieldsetCtrl = new Entity({ block: 'nk-list-edit-control' });
pageObject.fieldsetCtrl1 = new Entity({ block: 'nk-list-edit-control' }).nthChild(1).descendant(pageObject.textInput);
pageObject.fieldsetCtrlAdded = pageObject.fieldsetCtrl.mods({ state: 'added' });
pageObject.fieldset = new Entity({ block: 'nk-name-row-edit-layout', elem: 'name-attrs' });
pageObject.fieldsetType = new Entity({ block: 'nk-name-row-edit-layout', elem: 'name-type' });
pageObject.fieldsetLang = new Entity({ block: 'nk-name-row-edit-layout', elem: 'name-lang' });
pageObject.fieldsetName = pageObject.fieldset.mods({ id: 'name' });
pageObject.fieldsetName.input = pageObject.input.copy();
pageObject.fieldsetCtrl1.input = pageObject.input.copy();

pageObject.addrNameLang = new Entity({ block: 'nk-addr-name-row-edit-layout', elem: 'name-lang' });

pageObject.weekdaysEditLayoutRow = new Entity({ block: 'nk-weekdays-edit-layout', elem: 'row' });
pageObject.allWorkingDays = pageObject.weekdaysEditLayoutRow.nthChild(1).descendant(pageObject.nkCheckbox);
pageObject.allWeekendDays = pageObject.weekdaysEditLayoutRow.nthChild(2).descendant(pageObject.nkCheckbox);
pageObject.workingDay = pageObject.weekdaysEditLayoutRow.nthChild(1)
    .descendant(new Entity({ block: 'nk-weekdays-edit-layout', elem: 'day' }));
pageObject.weekendDay = pageObject.weekdaysEditLayoutRow.nthChild(2)
    .descendant(new Entity({ block: 'nk-weekdays-edit-layout', elem: 'day' }));

pageObject.nkPopup = new Entity({ block: 'nk-popup', modName: 'visible' });
pageObject.nkPopup.menu = new Entity({ block: 'nk-menu' });
pageObject.nkPopup.menuFocused = new Entity({ block: 'nk-menu', modName: 'focused' });
pageObject.nkPopup.menu.group = new Entity({ block: 'nk-menu', elem: 'group' });

pageObject.popup = new Entity({ block: 'popup' }).mix(new Entity({ block: 'popup', modName: 'visible' }));
pageObject.popup.menuFocused = new Entity({ block: 'menu', modName: 'focused' });

pageObject.suggestPopup = pageObject.nkPopup.copy();
pageObject.suggestPopup.desc = new Entity({ block: 'nk-suggest', elem: 'item-description' });
pageObject.suggestPopup.item = new Entity({ block: 'nk-suggest', elem: 'item' });
pageObject.suggestPopup.items = new Entity({ block: 'nk-suggest', elem: 'items' });

pageObject.notification = new Entity({ block: 'nk-notification-view' });
pageObject.notificationError = pageObject.notification.mods({ type: 'error' });
pageObject.notificationSuggest = pageObject.notification.mods({ type: 'suggest' });
pageObject.notificationSuccess = pageObject.notification.mods({ type: 'success' });
pageObject.notificationContent = new Entity({ block: 'nk-notification-view', elem: 'content' });

pageObject.userIcon = new Entity({ block: 'nk-user-icon' });
pageObject.userBarView = new Entity({ block: 'nk-user-bar-view' });
pageObject.userBarViewLoginBtn = new Entity({ block: 'nk-user-bar-view', elem: 'login-button' });
pageObject.userProfileLink = pageObject.nkPopup.descendant(pageObject.nkPopup.menu.group.nthChild(1))
    .descendant(pageObject.nkMenuItem.mods({ type: 'link' }).nthChild(1));
pageObject.userLogoutLink = pageObject.nkPopup.descendant(pageObject.nkPopup.menu.group.nthChild(2))
    .descendant(pageObject.nkMenuItem.mods({ type: 'link' }));
pageObject.userFlag = pageObject.nkPopup.descendant(new Entity({ block: 'nk-country-flag' }))

pageObject.layerManagerView = new Entity({ block: 'nk-map-layers-control-view' });
pageObject.layerManagerViewLayers = pageObject.icon.mods({ id: 'layers' });
pageObject.layerManagerViewLayersMenu = new Entity({ block: 'nk-map-layers-control-view', elem: 'layers' });
pageObject.layerManagerViewLayersMenu.item = pageObject.nkMenuItem.copy();
pageObject.layerManagerViewLayersMenu.item1 = pageObject.nkMenuItem.nthChild(1);
pageObject.layerManagerViewLayersMenu.item5 = pageObject.nkMenuItem.nthChild(5);
pageObject.layerManagerViewAdditionalLayersMenu = pageObject.layerManagerViewLayersMenu
    .descendant(pageObject.nkPopup.menu.nthChild(3));
pageObject.layerTracker = new Entity({ block: 'nk-map-layers-control-view', elem: 'layer_id_tracker' });

pageObject.mapControlsViewMapType = new Entity({ block: 'nk-map-controls-view', elem: 'map-type' });
pageObject.mapControlsViewMapTypeImage = new Entity({ block: 'nk-map-type-control-view', elem: 'image_current' });
pageObject.mapControlsViewRuler = new Entity({ block: 'nk-map-ruler-control-view' });

pageObject.regionViewBtn = new Entity({ block: 'nk-map-bottom-controls-view' });
pageObject.regionViewBtnDisabled = pageObject.regionViewBtn.descendant(pageObject.nkButtonDisabled);
pageObject.monitorIcon = pageObject.icon.mods({ id: 'user' });
pageObject.mrcIcon = pageObject.icon.mods({ id: 'mrc' });
pageObject.panoramaIcon = pageObject.icon.mods({ id: 'panorama' });
pageObject.panoramaIconDisabled = pageObject.regionViewBtnDisabled.descendant(pageObject.panoramaIcon);
pageObject.ymapsIcon = pageObject.icon.mods({ id: 'ymaps' });
pageObject.ymapsIconBtn = new Entity({ block: 'nk-map-region-view', elem: 'button' });
pageObject.eventsMonitorHintClose = new Entity({ block: 'nk-map-region-view', elem: 'events-monitor-hint-close' });
pageObject.feedbackIcon = pageObject.icon.mods({ id: 'feedback' });

pageObject.landStopSbView = pageObject.sidebarView.copy();
pageObject.landStopSbView.bus = pageObject.sidebarView.ctrl.nthChild(3);
pageObject.landStopSbView.bus.input = pageObject.input.copy();
pageObject.landStopSbView.bus.deleteIcon = pageObject.closeIconSmall.copy();
pageObject.landStopSbView.tram = pageObject.sidebarView.ctrl.nthChild(4);
pageObject.landStopSbView.tram.input = pageObject.input.copy();
pageObject.landStopSbView.tram.deleteIcon = pageObject.closeIconSmall.copy();
pageObject.landStopSbView.troll = pageObject.sidebarView.ctrl.nthChild(5);
pageObject.landStopSbView.troll.input = pageObject.input.copy();
pageObject.landStopSbView.troll.deleteIcon = pageObject.closeIconSmall.copy();
pageObject.landStopSbView.mini = pageObject.sidebarView.ctrl.nthChild(6);
pageObject.landStopSbView.mini.input = pageObject.input.copy();
pageObject.landStopSbView.mini.deleteIcon = pageObject.closeIconSmall.copy();

pageObject.poiSbView = pageObject.sidebarView.copy();
pageObject.poiSbView.rubricInput = pageObject.sidebarView.ctrl.nthChild(4).descendant(pageObject.input);
pageObject.poiSbView.location = pageObject.group2.descendant(pageObject.inputCtrl1);
pageObject.poiSbView.locationInput = pageObject.poiSbView.location.descendant(pageObject.nkTextInputControl);
pageObject.poiSbView.state = pageObject.group2.descendant(pageObject.button);
pageObject.poiSbView.hours = pageObject.group2.descendant(pageObject.inputCtrl3);
pageObject.poiSbView.hoursInput = pageObject.poiSbView.hours.descendant(pageObject.nkTextInputControl);
pageObject.poiSbView.website = pageObject.group3.descendant(pageObject.inputCtrl1);
pageObject.poiSbView.websiteInput = pageObject.poiSbView.website.descendant(pageObject.nkTextInputControl);
pageObject.poiSbView.email = pageObject.group3.descendant(pageObject.inputCtrl2);
pageObject.poiSbView.emailInput = pageObject.poiSbView.email.descendant(pageObject.nkTextInputControl);
pageObject.poiSbView.phone = pageObject.group3.descendant(pageObject.inputCtrl3);
pageObject.poiSbView.phoneInput = pageObject.poiSbView.phone.descendant(pageObject.nkTextInputControl);

pageObject.transportStopEditorItem = new Entity({ block: 'nk-transport-stop-routes-editor-view', elem: 'item' });
pageObject.transportStopEditorItem.delete = pageObject.closeIconSmall.copy();
pageObject.transportRoutesViewerItem = new Entity({ block: 'nk-transport-routes-viewer-view', elem: 'route' });
pageObject.transportTransitionEditorItem = new Entity({ block: 'nk-transport-transition-editor-view', elem: 'item' });
pageObject.transportTransitionEditorItem1 = pageObject.transportTransitionEditorItem.nthChild(1);
pageObject.transportTransitionEditorItem2 = pageObject.transportTransitionEditorItem.nthChild(2);
pageObject.transportTransitionEditorItem1.delete = pageObject.closeIconSmall.copy();
pageObject.transportTransitionEditorItem2.delete = pageObject.closeIconSmall.copy();

pageObject.adSbView = pageObject.sidebarView.copy();
pageObject.adSbViewCtrl = pageObject.sidebarView.ctrl.copy();
pageObject.adSbView.parentInput = pageObject.sidebarView.ctrl.nthChild(2).descendant(pageObject.input);
pageObject.adSbView.parentInputClear = pageObject.sidebarView.ctrl.nthChild(2).descendant(pageObject.textInput.clear);
pageObject.adSbView.capital = pageObject.section.nthChild(3).descendant(pageObject.group1)
    .descendant(pageObject.button);
pageObject.adSbView.city = pageObject.nkCheckboxControl.nthChild(2).descendant(pageObject.nkCheckbox);
pageObject.adSbView.municipality = pageObject.nkCheckboxControl.nthChild(3).descendant(pageObject.nkCheckbox);
pageObject.adSbView.notOfficial = pageObject.nkCheckboxControl.nthChild(4).descendant(pageObject.nkCheckbox);
pageObject.adSbView.populationInput = pageObject.group1.descendant(pageObject.nkTextInputControl);

pageObject.lanesEditor = new Entity({ block: 'nk-lanes-editor-view' });
pageObject.lanesEditorSelector = new Entity({ block: 'nk-lanes-editor-view', elem: 'lane-selector' });
pageObject.lanesEditorSelector.tab = pageObject.tab.copy();
pageObject.lanesEditorSelector.tab1 = pageObject.lanesEditorSelector.tab.nthChild(1);
pageObject.lanesEditorSelector.tab2 = pageObject.lanesEditorSelector.tab.nthChild(2);
pageObject.lanesEditorSelector.tab3 = pageObject.lanesEditorSelector.tab.nthChild(3);
pageObject.lanesEditorActions = new Entity({ block: 'nk-lanes-editor-view', elem: 'lane-actions' });
pageObject.lanesEditorDirections = new Entity({ block: 'nk-lanes-editor-view', elem: 'directions' });
pageObject.lanesEditorAddLeft = new Entity({ block: 'nk-lanes-editor-view', elem: 'add-left-lane' });
pageObject.lanesEditorAddRight = new Entity({ block: 'nk-lanes-editor-view', elem: 'add-right-lane' });

pageObject.condLaneEditorControl = new Entity({ block: 'nk-cond-lane-editor-view', elem: 'lanes-control' });
pageObject.condLaneEditorLane = new Entity({ block: 'nk-cond-lane-editor-view', elem: 'lane' });
pageObject.condLaneEditorLane1 = pageObject.condLaneEditorLane.nthChild(1);
pageObject.condLaneEditorLane2 = pageObject.condLaneEditorLane.nthChild(2);
pageObject.condLaneEditorLane3 = pageObject.condLaneEditorLane.nthChild(3);

pageObject.l45Icon = pageObject.icon.mods({ id: 'dir-l45' });
pageObject.l90Icon = pageObject.icon.mods({ id: 'dir-l90' });
pageObject.l135Icon = pageObject.icon.mods({ id: 'dir-l135' });
pageObject.l180Icon = pageObject.icon.mods({ id: 'dir-l180' });
pageObject.r45Icon = pageObject.icon.mods({ id: 'dir-r45' });
pageObject.r90Icon = pageObject.icon.mods({ id: 'dir-r90' });
pageObject.r135Icon = pageObject.icon.mods({ id: 'dir-r135' });
pageObject.r180Icon = pageObject.icon.mods({ id: 'dir-r180' });
pageObject.sIcon = pageObject.icon.mods({ id: 'dir-s' });
pageObject.slIcon = pageObject.icon.mods({ id: 'dir-sl' });
pageObject.srIcon = pageObject.icon.mods({ id: 'dir-sr' });
pageObject.lrIcon = pageObject.icon.mods({ id: 'dir-lr' });
pageObject.rlIcon = pageObject.icon.mods({ id: 'dir-rl' });

pageObject.search = new Entity({ block: 'nk-search-view' });
pageObject.search.input = pageObject.input.copy();
pageObject.search.clear = new Entity({ block: 'nk-text-input', elem: 'clear' });

pageObject.confirmationView = new Entity({ block: 'nk-confirmation-view' });
pageObject.confirmationView.submit = pageObject.submit.copy();

pageObject.mapOverlayLayout = new Entity({ block: 'nk-map-overlay-layout' });
pageObject.mapOverlayLayoutArrow = pageObject.mapOverlayLayout.mods({ type: 'arrow' }).descendant(pageObject.arrow);

pageObject.logo = new Entity({ block: 'nk-logo-view' });

pageObject.welcomeScreen = new Entity({ block: 'nk-welcome-screen-view' });
pageObject.welcomeScreenContent = new Entity({ block: 'nk-welcome-screen-view', elem: 'content' });
pageObject.welcomeScreenVideo = pageObject.welcomeScreenContent.descendant(new Entity({ block: 'nk-video-player' }));
pageObject.welcomeScreenText = new Entity({ block: 'nk-welcome-screen-view', elem: 'text' });
pageObject.welcomeScreenFooter = new Entity({ block: 'nk-welcome-screen-view', elem: 'footer' });
pageObject.welcomeScreenFooter.fbBtn = pageObject.nkButton.copy().nthChild(1);
pageObject.welcomeScreenFooter.startBtn = pageObject.nkButton.copy().nthChild(2);
pageObject.welcomeScreenClose = pageObject.welcomeScreenContent.descendant(pageObject.closeIconBig);

pageObject.subscriptionAlert = new Entity({ block: 'nk-subscription-prompt-view', elem: 'content' });

pageObject.unauthenticatedView = new Entity({ block: 'nk-unauthenticated-app-view' });

pageObject.passpLoginForm = new Entity({ block: 'passp-login-form' });
pageObject.passpCurrentAccDispName = new Entity({ block: 'passp-current-account', elem: 'display-name' });
pageObject.passpAuthHeaderLink = new Entity({ block: 'passp-auth-header-link', modName: 'visible' });
pageObject.passpAccListSignInIcon = new Entity({ block: 'passp-account-list', elem: 'sign-in-icon' });
pageObject.passpFieldLogin = new Entity('#passp-field-login');
pageObject.passpFieldPasswd = new Entity('#passp-field-passwd');
pageObject.passpSignInBtn = new Entity({ block: 'passp-sign-in-button' });
pageObject.passpSignInBtn.passpFormBtn = new Entity({ block: 'passp-form-button' });

module.exports = bemPageObject.create(pageObject);
