const cr = require('../../credentials.js');
const HELP = 'https://yandex.ru/support/maps-builder/concept/';
const TERMS_API = 'https://yandex.ru/legal/maps_api/';
const TERMS = 'https://yandex.ru/legal/maps_termsofuse/?lang=ru';
const BMAPS = require('../../.hermione.conf.js').baseUrl + '/maps/';
const API = 'https://yandex.ru/support/maps-builder/concept/markers_2.html#markers_2__kod_js';
const IFRAME = 'https://yandex.ru/support/maps-builder/concept/markers_2.html#markers_2__kod_iframe';
const STATIC = 'https://tech.yandex.ru/maps/doc/staticapi/1.x/dg/concepts/About-docpage';
const PRINT = 'https://yandex.ru/support/maps-builder/concept/markers_2.html#markers_2__print_warning';
const EXPORT = 'https://yandex.ru/support/maps-builder/concept/markers_3.html';

require('../helper.js')(afterEach);

describe('Ссылка', () => {
    afterEach(function () {
        return this.browser.crLogout();
    });

    it('На помощь в промо', function () {
        return this.browser
            .crInit()
            .click(PO.promo.prev())
            .crCheckLink(PO.carouselHelper.slide3.help())
            .then((url) => this.browser
                .crCheckURL(url, HELP, 'На помощь в карусели')
            );
    });

    it('На условия использования под кнопкой сохранения', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crCheckLink(PO.sidebar.termsLink())
            .then((url) => this.browser
                .crCheckURL(url, TERMS_API, 'Сломана ссылка на условия использования API')
            );
    });

    it('На условия использования на карте', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.ymaps(), 'Не загрузилось АПИ')
            .crCheckLink(PO.ymaps.copyright())
            .then((url) => this.browser
                .crCheckURL(url, TERMS, 'Сломана ссылка на условия использования')
            );
    });

    it('На БЯК на карте', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crWaitForVisible(PO.ymaps.logo(), 'Лого на карте не загрузилось')
            .moveToObject(PO.ymaps.logo())
            .crCheckLink(PO.ymaps.logo())
            .then((url) => this.browser.crCheckURL(url, BMAPS, 'Сломана ссылка на БЯК', {
                skipQuery: true
            }));
    });

    it('Подробнее об API Конструктора карт в интерактивной карте', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.linkMap)
            .crSaveMap()
            .click(PO.sidebarExport.getCodeBtn())
            .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
            .crCheckLink(PO.popup.getCode.aboutAPI())
            .then((url) => this.browser
                .crCheckURL(url, API, 'Сломана ссылка на API')
            );
    });

    it('Об ограничениях для iframe', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.linkMap)
            .crSaveMap()
            .click(PO.sidebarExport.getCodeBtn())
            .crWaitForVisible(PO.popup.getCode(), 'Не открылся попап с кодом карты')
            .click(PO.popup.getCode.switcher.iframe())
            .crWaitForVisible(PO.popup.getCode.switcher.iframeChecked())
            .crCheckLink(PO.popup.getCode.aboutIframe())
            .then((url) => this.browser
                .crCheckURL(url, IFRAME, 'Сломана ссылка на ограничения для iframe')
            );
    });

    it('Подробнее о Static API', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.linkMap)
            .crSaveMap()
            .click(PO.sidebarExport.staticSwitcher.static())
            .crWaitForVisible(PO.sidebarExport.staticSwitcher.staticChecked(), 'Режим интерактивной карты не выбран')
            .crCheckLink(PO.sidebarExport.staticLink())
            .then((url) => this.browser
                .crCheckURL(url, STATIC, 'Сломана ссылка на Static API')
            );
    });

    it('На ограничения при печати при допустимой карте', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.linkMap)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crWaitForVisible(PO.sidebarExport.mainSwitcher.printChecked(), 'Режим печатной карты не выбран')
            .crCheckLink(PO.sidebarExport.printLink())
            .then((url) => this.browser
                .crCheckURL(url, PRINT, 'Сломана ссылка на ограничения печати')
            );
    });

    it('На ограничения печати при недопустимой карте', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.linkMapPrint)
            .crSaveMap()
            .click(PO.sidebarExport.mainSwitcher.print())
            .crWaitForVisible(PO.sidebarExport.mainSwitcher.printChecked(), 'Режим печатной карты не выбран')
            .crCheckLink(PO.sidebarExport.printLinkError())
            .then((url) => this.browser
                .crCheckURL(url, PRINT, 'Сломана ссылка на ограничения печати')
            );
    });

    it('Подробнее о форматах и экспорте', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.linkMapPrint)
            .crSaveMap()
            .click(PO.exportButton())
            .crWaitForVisible(PO.exportModal(), 'Не открылось окно экспорта')
            .crCheckLink(PO.exportModal.help())
            .then((url) => this.browser
                .crCheckURL(url, EXPORT, 'Сломана ссылка на помощь в экспорте')
            )
            .click(PO.exportModal.close());
    });
});
