/*eslint object-curly-spacing: "off", no-implicit-globals: "off"*/

var PO = {};
var pageObject = require('@yandex-int/bem-page-object');
var El = pageObject.Entity;

const apiVersion = require('../../configs/testing/env').ymaps.version;
const ymaps = 'ymaps-' + apiVersion.replace(/\./g, '-') + '-';

PO.link = new El('a');
PO.button = new El('button');
PO.input = new El('input');
PO.textarea = new El('textarea');
PO.inputError = new El({ block: 'input_error' });
PO.textareaError = new El({ block: 'textarea_error' });

PO.popup = new El({ block: 'popup' });
PO.popupVisible = new El({ block: 'popup_visible' });
PO.popup.getCode = new El({ block: 'sidebar', elem: 'export-settings-panel-get-code-holder' });
PO.popup.getCode.aboutAPI = new El({ block: 'sidebar', elem: 'export-settings-panel-about-api-link' }).child(PO.link);
PO.popup.getCode.aboutIframe = new El({ block: 'sidebar', elem: 'export-settings-panel-iframe-confines-link' })
    .child(PO.link);
PO.popup.getCode.code = new El({ block: 'textarea', modName: 'id', modVal: 'code-preview' });
PO.popup.downloadFile = new El({ block: 'notify', modName: 'message', modVal: 'downloadFile' });
PO.popup.fileTypeError = new El({ block: 'notify', modName: 'message', modVal: 'fileTypeError' });
PO.popup.fileTypeError.link = new El({ block: 'notify', elem: 'description' }).child(PO.link);

PO.popupVisible.error = new El({ block: 'error-hint' });

PO.popupVisible.menuItem = new El({ block: 'menu', elem: 'item' });

PO.ymaps = new El({ block: ymaps + 'map' });
PO.ymaps.copyright = new El({ block: ymaps + 'copyright', elem: 'link' });
PO.ymaps.logo = new El({ block: ymaps + 'copyright', elem: 'logo' });
PO.ymaps.control = new El({ block: ymaps + 'controls', elem: 'control_toolbar' });
PO.ymaps.addPlacemark = PO.ymaps.control.nthType(1);
PO.ymaps.addPlacemark.checked = new El({ block: ymaps + '_checked' });
PO.ymaps.addLine = PO.ymaps.control.nthType(2);
PO.ymaps.addLine.checked = new El({ block: ymaps + '_checked' });
PO.ymaps.addPolygon = PO.ymaps.control.nthType(3);
PO.ymaps.addPolygon.checked = new El({ block: ymaps + '_checked' });
PO.ymaps.map = new El({ block: ymaps + 'events-pane'});
PO.ymaps.zoomIcon = new El({ block: ymaps + 'zoom__icon'});
PO.ymaps.plus = new El({ block: ymaps + 'zoom__plus'}).descendant(PO.ymaps.zoomIcon);
PO.ymaps.minus = new El({ block: ymaps + 'zoom__minus'}).descendant(PO.ymaps.zoomIcon);
PO.ymaps.layers = new El({ block: ymaps + '_icon_layers' });
PO.ymaps.layersList = new El({ block: ymaps + 'listbox', elem: 'list' });
PO.ymapsLayerItem = new El({ block: ymaps + 'listbox', elem: 'list-item-text' });
PO.ymaps.groundPane = new El({block: ymaps + 'ground-pane'});

PO.ymaps.placemark = new El({ block: ymaps + 'placemark-overlay' });
PO.ymaps.placemark.iconWithCaption = new El({ block: ymaps + 'islets_icon-with-caption' });
PO.ymaps.placemark.iconWithCaption.caption = new El({ block: ymaps + 'islets_icon-caption' });
PO.ymaps.placemark.iconEmpty = new El({ block: ymaps + 'svg-icon' });
PO.ymaps.placemark.iconEmpty.content = new El({ block: ymaps + 'svg-icon-content' });
PO.ymaps.bicycle = new El({ block: ymaps + 'pictogram_icon_bicycle2' });
PO.ymaps.dog = new El({ block: ymaps + 'pictogram_icon_dog' });
PO.ymaps.placemark.bicycle = PO.ymaps.bicycle.copy();
PO.ymaps.placemark.dog = PO.ymaps.dog.copy();

PO.ymaps.vertexMenu = new El({ block: ymaps + 'islets_editor-vertex-menu' });
PO.ymaps.vertexMenu.item = new El({ block: ymaps + 'islets_editor-vertex-menu', elem: 'item' });

PO.ymaps.searchBoxInput = new El({ block: ymaps + 'searchbox-input', elem: 'input' });
PO.ymaps.searchBoxButton = new El({ block: ymaps + 'searchbox-button' });
PO.ymaps.searchSerp = new El({ block: ymaps + 'islets_serp-popup' });
PO.ymaps.searchSerpFirst = new El({ block: ymaps + 'islets__first' });

PO.balloon = new El({ block: ymaps + 'balloon' });
PO.balloon.save = new El({ block: 'button', modName: 'id', modVal: 'save-button' });
PO.balloon.remove = new El({ block: 'button', modName: 'id', modVal: 'remove-button' });
PO.balloon.close = new El({ block: ymaps + 'balloon', elem: 'close-button' });
PO.balloon.text = PO.textarea.copy();
PO.balloon.textError = PO.balloon.text.mix(PO.textareaError);
PO.balloon.selectColor = new El({ block: 'button', modName: 'theme', modVal: 'colors' });
PO.balloon.selectIcon = new El({ block: 'button', modName: 'theme', modVal: 'icons' });
PO.balloon.type = new El({ block: 'radio-group', modName: 'theme', modVal: 'islands' });
PO.balloon.type.nothing = new El({ block: 'radio', modName: 'index', modVal: '0' });
PO.balloon.type.number = new El({ block: 'radio', modName: 'index', modVal: '1' });
PO.balloon.type.icon = new El({ block: 'radio', modName: 'index', modVal: '2' });

PO.balloon.numberInput = new El({ block: 'input', modName: 'type', modVal: 'number' }).descendant(PO.input);
PO.balloon.captionInput = new El({ block: 'balloon-setup', elem: 'caption' }).descendant(PO.input);
PO.balloon.captionError = new El({ block: 'balloon-setup', elem: 'caption' }).mix(PO.inputError);
PO.balloon.selectColor.text = new El({ block: 'button', elem: 'text' });
PO.balloon.iconType = new El({ block: 'radio-group', modName: 'theme', modVal: 'type-icons' });
PO.balloon.iconType.withLeg = new El({ block: 'radio', modName: 'index', modVal: '0' }).descendant(PO.button);
PO.balloon.iconType.withoutLeg = new El({ block: 'radio', modName: 'index', modVal: '1' }).descendant(PO.button);
PO.balloon.selectIcon.iconBicycle = PO.ymaps.bicycle.copy();
PO.balloon.selectIcon.iconDog = PO.ymaps.dog.copy();
PO.balloon.limits = new El({ block: 'balloon-setup', elem: 'limits' });
PO.balloon.limits.text = new El({ block: 'balloon-setup', elem: 'limits-text' });
PO.balloon.limits.helpLink = new El({ block: 'balloon-setup', elem: 'limits-help-link' });

PO.balloon.transparency = new El({ block: 'balloon-setup', elem: 'transparency-field' }).descendant(PO.input);
PO.balloon.width = new El({ block: 'balloon-setup', elem: 'stroke-width' }).descendant(PO.input);
PO.balloon.poly = new El({ block: 'balloon-setup', elem: 'poly-options' });
PO.balloon.poly.strokeColor = new El({ block: 'balloon-setup', elem: 'stroke-color' });
PO.balloon.poly.strokeOpacity = new El({ block: 'balloon-setup', elem: 'stroke-opacity' }).descendant(PO.input);
PO.balloon.poly.fillColor = new El({ block: 'balloon-setup', elem: 'fill-color' });
PO.balloon.poly.fillOpacity = new El({ block: 'balloon-setup', elem: 'fill-opacity' }).descendant(PO.input);
PO.balloon.poly.strokeColor.text = new El({ block: 'button', elem: 'text' });
PO.balloon.poly.fillColor.text = new El({ block: 'button', elem: 'text' });

PO.header = new El({ block: 'header' });

PO.balloonColorMenu = new El({ block: 'popup', modName: 'theme', modVal: 'colors' }).mix(PO.popupVisible);
PO.balloonColorMenu.item = new El({ block: 'menu', elem: 'item_index_' });

PO.balloonIconsMenu = new El({ block: 'popup', modName: 'theme', modVal: 'icons' });
PO.balloonIconsMenu.item = new El({ block: 'menu', elem: 'item_index_' });

PO.modalCell = new El({ block: 'modal', elem: 'cell' });
PO.popupClose = new El({ block: 'popup-close-button' });
PO.popupVisible.close = new El({ block: 'popup-close-button' });
PO.popupVisible.modelCell = PO.modalCell.copy();

PO.modalCell.mapSaved = new El({ block: 'notify', modName: 'message', modVal: 'mapSaved' });
PO.modalCell.mapSaved.openDisk = new El({ block: 'notify', elem: 'button' });
PO.modalCell.mapSaved.close = new El({ block: 'popup-close-button', elem: 'button' });

PO.userData = new El({ block: 'user-data' }).mix(new El({ block: 'user-button', elem: 'avatar' }));
PO.userData.pic = new El({ block: 'user-data', elem: 'userpic' });
PO.userData.username = new El({ block: 'user-data', elem: 'username' });

PO.userMenu = new El({ block: 'user-menu' }).descendant(new El({ block: 'menu', elem: 'control' }));
PO.userMenu.exit = new El({ block: 'user-menu', elem: 'exit-link' });
PO.userMenu.addUser = new El({ block: 'user-menu', elem: 'add-user-link' });
PO.userMenu.otherUsersName = new El({ block: 'user-data', elem: 'username' });
PO.userMenu.disk = new El({ block: 'menu', elem: 'item', modName: 'index', modVal: '0' }).child(PO.link);
PO.userMenu.tune = new El({ block: 'menu', elem: 'item', modName: 'index', modVal: '1' }).child(PO.link);
PO.userMenu.passport = new El({ block: 'menu', elem: 'item', modName: 'index', modVal: '2' }).child(PO.link);

PO.page = new El({ block: 'page' });
PO.pagePreview = new El({ block: 'page_preview' });
PO.promoInitied = new El({ block: 'promo', modName: 'js', modVal: 'inited' });
PO.stepPromo = new El({ block: 'page', modName: 'step', modVal: 'promo' });
PO.stepMapselection = new El({ block: 'page', modName: 'step', modVal: 'mapselection' });
PO.stepEditor = new El({ block: 'page', modName: 'step', modVal: 'editor' });
PO.stepExport = new El({ block: 'page', modName: 'step', modVal: 'export-modal' });

PO.promo = new El({ block: 'promo' });
PO.promo.create = new El({ block: 'promo', elem: 'create-map-button' });
PO.promo.open = new El({ block: 'promo', elem: 'open-map-button' });
PO.promo.close = PO.popupClose.copy();
PO.promo.next = new El({ block: 'bx-next' });
PO.promo.prev = new El({ block: 'bx-prev' });

PO.carouselHelper = new El({ block: 'carousel-helper' });
PO.carouselHelper.slide3 = new El({ block: 'carousel-helper', elem: 'slider-item' }).nthType(4);
PO.carouselHelper.slide3.help = new El({ block: 'carousel-helper', elem: 'slider-item-description' }).child(PO.link);

PO.passport = new El({ block: 'passp-auth' });
PO.passport.error = new El({ block: 'passport-Domik-Form-Error' });
PO.passport.login = new El('#passp-field-login');
PO.passport.pass = new El('#passp-field-passwd');
PO.passport.submit = new El({ block: 'passp-sign-in-button' });
PO.passport.add = new El({ block: 'passp-account-list', elem: 'sign-in-icon' });
PO.passport.return = new El({ block: 'passp-previous-step-button' });
PO.passport.account = new El({ block: 'passp-current-account' });
PO.passport.routeDone = new El({ block: 'passp-route-enter-done' });
PO.passport.routeDone.back = PO.passport.routeDone.nthChild(3);
PO.passport.requestPhone = new El({ block: 'passp-enter-phone-form' });
PO.passport.requestPhone.back = PO.passport.requestPhone.nthChild(3);

PO.spin = new El('.spin_visible');

PO.mapSelection = new El({ block: 'map-selection' });
PO.mapSelection.holder = new El({ block: 'map-selection', elem: 'map-list-constructor-holder' });
PO.mapSelection.item = new El({ block: 'map-selection-item' });
PO.mapSelection.menuBtn = new El({ block: 'map-selection-item', elem: 'more-menu-dropdown' });
PO.mapSelection.itemText = new El({ block: 'map-selection-item', elem: 'title-text' });
PO.mapSelection.itemTag = new El({ block: 'map-selection-item', elem: 'title' })
    .descendant(new El({ block: 'map-selection-item', elem: 'tag' }));
PO.mapSelection.itemDesc = new El({ block: 'map-selection-item', elem: 'description' });
PO.mapSelection.itemUpdated = new El({ block: 'map-selection-item', elem: 'updated' });
PO.mapSelection.itemFirst = PO.mapSelection.item.nthType(1);
PO.mapSelection.item2 = PO.mapSelection.item.nthType(2);
PO.mapSelection.item3 = PO.mapSelection.item.nthType(3);
PO.mapSelection.itemFirstName = PO.mapSelection.itemFirst.descendant(PO.mapSelection.itemText);
PO.mapSelection.itemFirstDesc = PO.mapSelection.itemFirst.descendant(PO.mapSelection.itemDesc);
PO.mapSelection.itemFirstUpdated = PO.mapSelection.itemFirst.descendant(PO.mapSelection.itemUpdated);
PO.mapSelection.itemFirstMenuBtn = PO.mapSelection.itemFirst.descendant(PO.mapSelection.menuBtn);
PO.mapSelection.item2MenuBtn = PO.mapSelection.item2.descendant(PO.mapSelection.menuBtn);
PO.mapSelection.item3MenuBtn = PO.mapSelection.item3.descendant(PO.mapSelection.menuBtn);
PO.mapSelection.itemFirstMymapsTag = PO.mapSelection.itemFirst.descendant(PO.mapSelection.itemTag);
PO.mapSelection.item2Name = PO.mapSelection.item2.descendant(PO.mapSelection.itemText);
PO.mapSelection.item3Name = PO.mapSelection.item3.descendant(PO.mapSelection.itemText);
PO.mapSelection.item2mymapsTag = PO.mapSelection.item2.descendant(PO.mapSelection.itemTag);
PO.mapSelection.item3mymapsTag = PO.mapSelection.item3.descendant(PO.mapSelection.itemTag);
PO.mapSelection.item25 = PO.mapSelection.item.nthType(25);
PO.mapSelection.item50 = PO.mapSelection.item.nthType(50);
PO.mapSelection.item75 = PO.mapSelection.item.nthType(75);
PO.mapSelection.close = PO.popupClose.copy();
PO.mapSelection.create = new El({ block: 'button', modName: 'id', modVal: 'create-button2' });
PO.mapSelection.import = new El({ block: 'button', modName: 'id', modVal: 'import-button' });
PO.mapSelectionActive = PO.mapSelection.mix(new El({ block: 'map-selection', modName: 'active' }));
PO.mapSelectionMenu = new El({ block: 'map-selection-item', elem: 'popup' }).mix(new El('.dropdown_opened'));
PO.mapSelectionMenuGroup0 = new El({ block: 'menu', elem: 'group', modName: 'index', modVal: '0' });
PO.mapSelectionMenuGroup1 = new El({ block: 'menu', elem: 'group', modName: 'index', modVal: '1' });
PO.mapSelectionMenu.menuShare = PO.mapSelectionMenuGroup0
    .descendant(new El({ block: 'menu', elem: 'item', modName: 'index', modVal: '0' }));
PO.mapSelectionMenu.menuCopy = PO.mapSelectionMenuGroup0
    .descendant(new El({ block: 'menu', elem: 'item', modName: 'index', modVal: '1' }));
PO.mapSelectionMenu.menuDelete = PO.mapSelectionMenuGroup1
    .descendant(new El({ block: 'menu', elem: 'item', modName: 'index', modVal: '0' }));
PO.mapSelectionMenu.share = new El({ block: 'map-selection-item', elem: 'share-holder' });
PO.mapSelectionMenu.shareInput = PO.mapSelectionMenu.share.descendant(PO.input);
PO.mapSelectionMenu.shareBtn = PO.mapSelectionMenu.share
    .descendant(new El({ block: 'menu', elem: 'item', modName: 'index', modVal: '0' }));
PO.mapSelectionMenu.shareBtn.copyCaption = new El({ block: 'map-selection-item', elem: 'copyCaption' });
PO.mapSelectionMenu.shareBtn.copiedCaption = new El({ block: 'map-selection-item', elem: 'copiedCaption' });

PO.mapListButton = new El({ block: 'button', modName: 'id', modVal: 'mapListBtn' });

PO.saveAndContinue = new El({ block: 'button', modName: 'id', modVal: 'saveBtn' });

PO.exportButton = new El({ block: 'link', modName: 'id', modVal: 'export-link' });
PO.exportModal = new El({ block: 'export-modal' });
PO.exportModal.close = PO.popupClose.copy();
PO.exportModal.help = new El({ block: 'export-modal', elem: 'help-link' });
PO.exportModal.download = new El({ block: 'export-modal', elem: 'download-button' });
PO.exportModal.switcher = new El({ block: 'export-modal', elem: 'filetype-switcher' });
PO.exportModal.switcher.typeKML = new El({ block: 'radio', modName: 'index', modVal: '0' });
PO.exportModal.switcher.typeXLSX = new El({ block: 'radio', modName: 'index', modVal: '1' });
PO.exportModal.switcher.typeGPX = new El({ block: 'radio', modName: 'index', modVal: '2' });
PO.exportModal.switcher.typeGeoJSON = new El({ block: 'radio', modName: 'index', modVal: '3' });
PO.exportModal.switcher.typeCSV = new El({ block: 'radio', modName: 'index', modVal: '4' });

PO.selectColorMenu = new El('.popup_theme_colors .menu_theme_colors');
PO.selectColorMenu.item1 = new El({ block: 'menu', modName: 'item_index', modVal: '1' });

PO.sidebar = new El({ block: 'sidebar', elem: 'map-content-panel' });
PO.sidebar.termsLink = new El({ block: 'sidebar', elem: 'map-content-panel-terms' }).child(PO.link);

PO.checked = new El('.radio_checked');

PO.sidebarExport = new El({ block: 'sidebar', elem: 'export-settings-panel' });
PO.sidebarExport.getCodeBtn = new El({ block: 'sidebar', elem: 'export-settings-panel-get-code-button-holder' })
    .child(PO.button);
PO.sidebarExport.staticSwitcher = new El({ block: 'radio-group', modName: 'id', modVal: 'static-switcher' });
PO.sidebarExport.staticSwitcher.interactive = new El({ block: 'radio', modName: 'index', modVal: '0' });
PO.sidebarExport.staticSwitcher.interactiveChecked = PO.sidebarExport.staticSwitcher.interactive.mix(PO.checked);
PO.sidebarExport.staticSwitcher.static = new El({ block: 'radio', modName: 'index', modVal: '1' });
PO.sidebarExport.staticSwitcher.staticChecked = PO.sidebarExport.staticSwitcher.static.mix(PO.checked);
PO.sidebarExport.mainSwitcher = new El({ block: 'radio-group', modName: 'id', modVal: 'main-switcher' });
PO.sidebarExport.mainSwitcher.code = new El({ block: 'radio', modName: 'index', modVal: '0' });
PO.sidebarExport.mainSwitcher.codeChecked = PO.sidebarExport.mainSwitcher.code.mix(PO.checked);
PO.sidebarExport.mainSwitcher.print = new El({ block: 'radio', modName: 'index', modVal: '1' });
PO.sidebarExport.mainSwitcher.printChecked = PO.sidebarExport.mainSwitcher.print.mix(PO.checked);
PO.sidebarExport.notice = new El({ block: 'sidebar', elem: 'export-settings-panel-map-usage-notice' });
PO.sidebarExport.noticeError = new El({ block: 'sidebar', elem: 'export-settings-panel-print-error-notice' });
PO.sidebarExport.staticLink = PO.sidebarExport.notice.nthType(2).child(PO.link);
PO.sidebarExport.printLink = PO.sidebarExport.notice.nthType(3).child(PO.link);
PO.sidebarExport.printLinkError = PO.sidebarExport.noticeError.child(PO.link);
PO.sidebarExport.back = new El({block: 'link', modName: 'id', modVal: 'back-button' });

PO.sidebar.mapNameHolder = new El({ block: 'sidebar', elem: 'map-content-panel-label' });
PO.sidebar.mapName = PO.sidebar.mapNameHolder.descendant(PO.input);
PO.sidebar.mapNameError = PO.sidebar.mapNameHolder.descendant(PO.inputError);
PO.sidebar.mapDescHolder = new El({ block: 'sidebar', elem: 'description-holder' });
PO.sidebar.mapDesc = PO.sidebar.mapDescHolder.descendant(PO.textarea);
PO.sidebar.mapDescError = PO.sidebar.mapDesc.mix(PO.textareaError);
PO.sidebar.importBtn = new El({ block: 'button', modName: 'id', modVal: 'importBtn' });
PO.sidebar.limit = new El({ block: 'link', modName: 'id', modVal: 'limit-link' });

PO.sidebarExport.mapWidth = new El({ block: 'input', modName: 'id', modVal: 'map-width' }).descendant(PO.input);
PO.sidebarExport.mapWidthError = new El({ block: 'input', modName: 'id', modVal: 'map-width' })
    .mix(PO.inputError);
PO.sidebarExport.mapHeight = new El({ block: 'input', modName: 'id', modVal: 'map-height' }).descendant(PO.input);
PO.sidebarExport.mapHeightError = new El({ block: 'input', modName: 'id', modVal: 'map-height' })
    .mix(PO.inputError);
PO.sidebarExport.fitWidth = new El({ block: 'checkbox', modName: 'id', modVal: 'fit-width' });
PO.sidebarExport.fitWidth.input = PO.input.copy();

PO.sidebarExport.classicPrint = new El({ block: 'sidebar', elem: 'export-settings-panel-standard-print-block' })
    .descendant(new El({ block: 'classic-print-button' }));
PO.sidebarExport.error = new El({ block: 'sidebar', elem: 'export-settings-panel-print-error-notice' });
PO.sidebarExport.error.classicPrint = new El({ block: 'classic-print-button' });
PO.sidebarExport.errorZoom = new El({ block: 'sidebar', elem: 'export-settings-panel-print-error-zoom' });
PO.sidebarExport.errorMap = new El({ block: 'sidebar', elem: 'export-settings-panel-print-error-map' });

PO.import = new El({ block: 'import-modal' });
PO.import.attach = new El({ block: 'attach', elem: 'control' });
PO.import.status = new El({ block: 'import-modal', elem: 'status' });
PO.import.status.approve = new El({ block: 'import-modal', elem: 'approve-button' });
PO.import.status.abort = new El({ block: 'import-modal', elem: 'abort-button' });
PO.import.warningLink = new El({ block: 'import-modal', elem: 'warning-link' }).child(PO.link);
PO.import.warningErrors = new El({ block: 'import-modal', elem: 'warning-errors' }).child(PO.link).nthType(2);
PO.import.desc = new El({ block: 'import-modal', elem: 'desc' }).child(PO.link);
PO.import.templates = new El({ block: 'import-modal', elem: 'templates' });
PO.import.templates.xlsx = new El({ block: 'templates', elem: 'content' }).child(PO.link).nthType(1);
PO.import.templates.csv = new El({ block: 'templates', elem: 'content' }).child(PO.link).nthType(2);

PO.popup.getCode.switcher = new El({ block: 'radio-group', modName: 'id', modVal: 'interactive-map-switcher' });
PO.popup.getCode.switcher.js = new El({ block: 'radio', modName: 'index', modVal: '0' });
PO.popup.getCode.switcher.jsChecked = PO.popup.getCode.switcher.js.mix(PO.checked);
PO.popup.getCode.switcher.iframe = new El({ block: 'radio', modName: 'index', modVal: '1' });
PO.popup.getCode.switcher.iframeChecked = PO.popup.getCode.switcher.iframe.mix(PO.checked);
PO.popup.getCode.iframeError = new El({ block: 'popup', elem: 'iframe-error-map' });
PO.popup.getCode.iframeErrorZoom = new El({ block: 'popup', elem: 'iframe-error-zoom' });

PO.mapEditorInteractiveScript = new El({ block: 'map-editor', modName: 'mapType', modVal: 'interactiveScript' });
PO.mapEditorInteractiveScript.map = PO.ymaps.map.copy();
PO.mapEditorInteractiveScript.resizer = new El({ block: 'resizer', elem: 'slider_direction_' });

PO.mapEditorInteractiveIframe = new El({ block: 'map-editor', modName: 'mapType', modVal: 'interactiveIframe' });
PO.mapEditorInteractiveIframe.map = PO.ymaps.map.copy();
PO.mapEditorInteractiveIframe.resizer = new El({ block: 'resizer', elem: 'slider_direction_' });

PO.mapEditorPreviewHolder = new El({ block: 'map-editor', elem: 'preview-holder' });

PO.sidebarExport.yadiskBtn = new El({ block: 'button', modName: 'id', modVal: 'export-image-button' });
PO.sidebarExport.downloadBtn = new El({ block: 'button', modName: 'id', modVal: 'download-button' });
PO.sidebarExport.imageExtension = new El({ block: 'select', modName: 'id', modVal: 'image-extension' })
    .child(PO.button);
PO.sidebarExport.imageExtension.text = new El({ block: 'button', elem: 'text' });
PO.sidebarExport.imageDpi = new El({ block: 'select', modName: 'id', modVal: 'dpi-selector' }).child(PO.button);
PO.sidebarExport.imageDpi.text = new El({ block: 'button', elem: 'text' });

PO.help = new El({ block: 'help-button' });
PO.helpPopup = new El({ block: 'help-button', elem: 'popup' }).descendant(new El({ block: 'menu', elem: 'control' }));
PO.helpPopup.group = new El({ block: 'menu', elem: 'group' });
PO.helpPopup.groupTop = PO.helpPopup.group.mods({ index: '0' });
PO.helpPopup.item = new El({ block: 'menu', elem: 'item' });
PO.helpPopup.help = PO.helpPopup.groupTop.descendant(PO.helpPopup.item.mods({ index: '0' }));
PO.helpPopup.hotkeys = PO.helpPopup.groupTop.descendant(PO.helpPopup.item.mods({ index: '1' }));
PO.helpPopup.feedback = PO.helpPopup.groupTop.descendant(PO.helpPopup.item.mods({ index: '2' }));
PO.helpPopup.NK = PO.helpPopup.group.mods({ index: '1' }).descendant(PO.helpPopup.item.mods({ index: '0' }));

PO.geoObjectList = new El({ block: 'geoObject-list' });
PO.geoObjectList.holder = new El({ block: 'geoObject-list', elem: 'holder' });
PO.geoObjectList.item = new El({ block: 'geoObject-list-item' });
PO.geoObjectList.item.title = new El({ block: 'geoObject-list-item', elem: 'title' });
PO.geoObjectList.item.icon = new El({ block: 'geoObject-list-item', elem: 'icon' });
PO.geoObjectList.itemPointNumber = PO.geoObjectList.item.mods({ type: 'point-number' });
PO.geoObjectList.itemPointPic = PO.geoObjectList.item.mods({ type: 'point-pictogram' });
PO.geoObjectList.itemPointCompactNumber = PO.geoObjectList.item.mods({ type: 'point-compact-number' });
PO.geoObjectList.itemLinestring = PO.geoObjectList.item.mods({ type: 'linestring' });
PO.geoObjectList.itemPolygon = PO.geoObjectList.item.mods({ type: 'polygon' });
PO.geoObjectList.itemPointNumberTitle = PO.geoObjectList.itemPointNumber.descendant(PO.geoObjectList.item.title);
PO.geoObjectList.itemPointCompactNumberTitle = PO.geoObjectList.itemPointCompactNumber
    .descendant(PO.geoObjectList.item.title);
PO.geoObjectList.itemLinestringTitle = PO.geoObjectList.itemLinestring.descendant(PO.geoObjectList.item.title);
PO.geoObjectList.itemPolygonTitle = PO.geoObjectList.itemPolygon.descendant(PO.geoObjectList.item.title);
PO.geoObjectList.itemPointNumberIcon = PO.geoObjectList.itemPointNumber.descendant(PO.geoObjectList.item.icon);
PO.geoObjectList.itemPointPicIcon = PO.geoObjectList.itemPointPic.descendant(PO.geoObjectList.item.icon);
PO.geoObjectList.itemPointCompactNumberIcon = PO.geoObjectList.itemPointCompactNumber
    .descendant(PO.geoObjectList.item.icon);
PO.geoObjectList.itemLinestringIcon = PO.geoObjectList.itemLinestring.descendant(PO.geoObjectList.item.icon);
PO.geoObjectList.itemLinestringDistance = PO.geoObjectList.itemLinestring.descendant(new El({ block:
    'geoObject-list-item', elem: 'measure' }));
PO.geoObjectList.itemPolygonSquare = PO.geoObjectList.itemPolygon.descendant(new El({ block:
    'geoObject-list-item', elem: 'measure' }));
PO.geoObjectList.itemPolygonIcon = PO.geoObjectList.itemPolygon.descendant(PO.geoObjectList.item.icon);
PO.geoObjectList.itemPointDot = PO.geoObjectList.item.mods({ type: 'point-dot' });
PO.geoObjectList.itemPointDotTitle = PO.geoObjectList.itemPointDot.descendant(PO.geoObjectList.item.title);
PO.geoObjectList.itemPointDotIcon = PO.geoObjectList.itemPointDot.descendant(PO.geoObjectList.item.icon);

PO.mapHistoryUndo = new El({ block: 'map-history-buttons-undo' });
PO.mapHistoryRedo = new El({ block: 'map-history-buttons-redo' });
PO.mapHistoryShadow = new El({ block: 'map-history-buttons-shadow' });

module.exports.PO = pageObject.create(PO);
