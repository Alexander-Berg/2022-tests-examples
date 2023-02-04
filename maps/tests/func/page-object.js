const PO = {};

const pageObject = require('@yandex-int/bem-page-object');

const El = pageObject.Entity;

const config = require('./config');

const apiVersion = config.apiVersion;

const yamaps = 'ymaps-' + apiVersion.replace(/\./g, '-') + '-';

PO.tilesLoaded = new El('tiles-loaded');

PO.link = new El('a');
PO.button = new El('button');
PO.input = new El('input');
PO.ymaps = new El('ymaps');
PO.textarea = new El('textarea');
PO.inputError = new El({block: 'input_error'});
PO.textareaError = new El({block: 'textarea_error'});

PO.mapId = new El('#map');
PO.map = new El({block: yamaps + 'map'});
PO.map.map = new El({block: yamaps + 'events-pane'});

PO.groundPane = new El({block: yamaps + 'ground-pane'});

PO.map.controls = new El({block: yamaps + 'controls-pane'});

PO.map.controls.traffic = new El({block: yamaps + 'traffic'});
PO.map.controls.traffic.settings = new El({block: yamaps + 'traffic', elem: 'icon_icon_settings'});

PO.map.controls.zoom = new El({block: yamaps + 'zoom'});
PO.map.controls.zoom.plus = new El({block: yamaps + 'zoom', elem: 'plus'});
PO.map.controls.zoom.runner = new El({block: yamaps + 'zoom', elem: 'runner'});
PO.map.controls.zoom.scale = new El({block: yamaps + 'zoom', elem: 'scale'});
PO.map.controls.zoom.minus = new El({block: yamaps + 'zoom', elem: 'minus'});

PO.map.controls.search = new El({block: yamaps + 'searchbox'});
PO.map.controls.search.small = new El({block: yamaps + 'float-button-icon_icon_magnifier'});
PO.map.controls.search.large = new El({block: yamaps + 'searchbox', elem: 'normal-layout'});
PO.map.controls.search.large.input = new El({block: yamaps + 'searchbox-input', elem: 'input'});
PO.map.controls.search.large.button = new El({block: yamaps + 'searchbox-button'});
PO.map.controls.search.large.clear = new El({block: yamaps + 'searchbox-input', elem: 'clear-button'});
PO.map.controls.search.large.list = new El({block: yamaps + 'searchbox-list-button'});
PO.map.controls.search.large.serp = new El({block: yamaps + 'islets_serp-popup'});
PO.map.controls.search.large.serp.panel = new El({block: yamaps + 'islets_serp'});
PO.map.controls.search.large.serp.advert = new El({block: yamaps + 'islets_serp-advert'});
PO.map.controls.search.large.serp.item = new El({block: yamaps + 'islets_serp-item'});

PO.map.controls.fullscreen = new El({block: yamaps + 'float-button-icon_icon_expand'});
PO.map.controls.fullscreenCollapse = new El({block: yamaps + 'float-button-icon_icon_collapse'});

PO.map.controls.button = new El({block: yamaps + 'controls', elem: 'control_toolbar'});
PO.map.controls.button1 = PO.map.controls.button.nthType(1);
PO.map.controls.button2 = PO.map.controls.button.nthType(2);
PO.map.controls.button3 = PO.map.controls.button.nthType(3);
PO.map.controls.button4 = PO.map.controls.button.nthType(4);
PO.map.controls.buttonHiddenIcon = new El({block: yamaps + 'hidden-icon'});
PO.map.controls.buttonHiddenText = new El({block: yamaps + 'hidden-text'});
PO.mapControlsButtonText = new El({block: yamaps + 'float-button-text'});

PO.searchPanel = new El({block: yamaps + 'searchpanel-pane'});
PO.searchPanel.input = new El({block: yamaps + 'searchbox-input', elem: 'input'});
PO.searchPanel.button = new El({block: yamaps + 'searchbox-button-text'});
PO.searchPanel.fold = new El({block: yamaps + 'searchbox', elem: 'fold-button-icon'});
PO.searchPanel.serp = new El({block: yamaps + 'islets_serp-popup'});

PO.map.controls.listbox = new El({block: yamaps + 'listbox'});
PO.map.controls.listbox.selectedItem = new El({block: yamaps + 'listbox', elem: 'list-item_selected_yes'});
PO.map.controls.listbox.nonSelectedItem = new El({block: yamaps + 'listbox', elem: 'list-item_selected_no'});
PO.map.controls.listbox.typeSelectorIcon = new El({block: yamaps + '_icon_layers'});
PO.mapControlsListboxItem = new El({block: yamaps + 'listbox', elem: 'list-item-text'});

PO.map.controls.routeButton = new El({block: yamaps + 'route-panel-button'});
PO.map.controls.routeButton.button = new El({block: yamaps + 'route-panel-button', elem: 'button'});
PO.map.controls.routeButton.panel = new El({block: yamaps + 'route-panel-button', elem: 'popup'});
PO.map.controls.routeButton.panel.svgB = new El({block: yamaps + 'route-panel-input', elem: 'icon_last'});
PO.map.controls.routeButton.panel.routeType = new El({block: yamaps + 'route-panel-types'});
PO.map.controls.routeButton.panel.routeType.auto = new El({block: yamaps + 'route-panel-types', elem: 'type_auto'});
PO.map.controls.routeButton.panel.routeType.mass = new El({block: yamaps + 'route-panel-types',
    elem: 'type_masstransit'});
PO.map.controls.routeButton.panel.routeType.pedestrian = new El({block: yamaps + 'route-panel-types',
    elem: 'type_pedestrian'});
PO.map.controls.routeButton.panel.clear = new El({block: yamaps + 'route-panel', elem: 'clear'});
PO.map.controls.routeButton.panel.location = new El({block: yamaps + 'route-panel-input', elem: 'location'});

PO.map.controls.routeEditor = new El({block: yamaps + 'float-button-icon_icon_routes'});

PO.map.controls.geolocation = new El({block: yamaps + 'float-button-icon_icon_geolocation'});

PO.map.controls.ruler = new El({block: yamaps + 'float-button-icon_icon_ruler'});

PO.suggest = new El({block: yamaps + 'search', elem: 'suggest'});
PO.suggest.catalogItem = new El({block: yamaps + 'search', elem: 'suggest-catalog-item'});
PO.suggest.item0 = new El({block: yamaps + 'suggest-item-0'});

PO.map.searchPanel = new El({block: yamaps + 'searchpanel-pane'});
PO.map.searchPanel.fold = new El({block: yamaps + 'searchbox', elem: 'fold-button-icon'});

PO.map.pane = new El({block: yamaps + 'inner-panes'});
PO.map.pane.events = new El({block: yamaps + 'events-pane'});
PO.map.pane.editor = new El({block: yamaps + 'editor-pane'});
PO.map.pane.areas = new El({block: yamaps + 'areas-pane'});

PO.map.placemark = new El({block: yamaps + 'places-pane'});
PO.map.placemark.svgIcon = new El({block: yamaps + 'svg-icon'});
PO.map.placemark.placemark = new El({block: yamaps + 'placemark-overlay'});
PO.map.placemark.svgIconContent = new El({block: yamaps + 'svg-icon-content'});
PO.mapPlacemarkSvgIconContent = new El({block: yamaps + 'svg-icon-content'}).child(PO.ymaps);

PO.editorMenuItem = new El({block: yamaps + 'islets_editor-vertex-menu'});

PO.map.balloon = new El({block: yamaps + 'balloon-pane'});
PO.map.balloon.closeButton = new El({block: yamaps + 'balloon', elem: 'close-button'});
PO.map.balloon.content = new El({block: yamaps + 'balloon', elem: 'content'}).child(PO.ymaps).child(PO.ymaps);
PO.map.balloon.footer = new El({block: yamaps + 'balloon-content', elem: 'footer'});
PO.map.balloon.accordionItemTitle = new El({block: yamaps + 'b-cluster-accordion', elem: 'item-title'});
PO.map.balloon.accordionItemCaption = new El({block: yamaps + 'b-cluster-accordion', elem: 'item-caption'});
PO.map.balloon.accordionMenu = new El({block: yamaps + 'b-cluster-accordion', elem: 'menu'});
PO.map.balloon.carouselPagerItem = new El({block: yamaps + 'b-cluster-carousel', elem: 'pager-item'});
PO.map.balloon.carouselNext = new El({block: yamaps + 'b-cluster-carousel', elem: 'nav_type_next'});
PO.map.balloon.carouselEllipsis = new El({block: yamaps + 'b-cluster-carousel', elem: 'pager-item_ellipsis_yes'});
PO.map.balloon.carouselEllipsis2 = PO.map.balloon.carouselEllipsis.nthType(2);
PO.map.balloon.twoColumnsTabs = new El({block: yamaps + 'b-cluster-tabs'});
PO.map.balloon.routeMore = new El({block: yamaps + 'route-content', elem: 'button-holder'});

PO.map.balloonPanel = new El({block: yamaps + 'panel-pane'});
PO.map.balloonPanel.content = new El({block: yamaps + 'balloon', elem: 'content'}).child(PO.ymaps).child(PO.ymaps);
PO.map.balloonPanel.closeButton = new El({block: yamaps + 'balloon', elem: 'close-button'});

PO.map.outerBalloon = new El({block: yamaps + 'outerBalloon-pane'});
PO.map.outerBalloon.closeButton = new El({block: yamaps + 'balloon', elem: 'close-button'});

PO.map.copyrights = new El({block: yamaps + 'copyrights-pane'});
PO.map.copyrights.promo = new El({block: yamaps + 'map-copyrights-promo'});
PO.map.copyrights.link = new El({block: yamaps + 'copyright', elem: 'link'});
PO.map.copyrights.logo = new El({block: yamaps + 'copyright', elem: 'logo'});

PO.map.hint = new El({block: yamaps + 'outerHint-pane'});
PO.map.hint.text = new El({block: yamaps + 'hint', elem: 'text'}).child(PO.ymaps).child(PO.ymaps);

PO.routePoints = new El({block: yamaps + 'routerPoints-pane'});
PO.routePoints.placemark = new El({block: yamaps + 'placemark-overlay'});
PO.routePoints.pinA = new El({block: yamaps + 'route-pin', elem: 'label-0'});
PO.routePoints.pinC = new El({block: yamaps + 'route-pin', elem: 'label-2'});
PO.routePoints.pinD = new El({block: yamaps + 'route-pin', elem: 'label-3'});
PO.routePoints.transportPin = new El({block: yamaps + 'transport-pin', elem: 'icon'});
PO.routePoints.pinText = new El({block: yamaps + 'route-pin', elem: 'text'}).child(PO.ymaps).child(PO.ymaps);

PO.ymaps.layersBtn = new El({block: yamaps, modName: 'icon', modVal: 'layers'});
PO.ymaps.layersPanel = new El({block: yamaps + 'listbox', elem: 'panel'});
PO.yamapsLayerItem = new El({block: yamaps + 'listbox', elem: 'list-item-text'});

PO.panorama = new El({block: yamaps + 'panorama-screen'});
PO.panorama.plus = new El({block: yamaps + 'islets_button-icon_plus'});

module.exports.PO = pageObject.create(PO);
