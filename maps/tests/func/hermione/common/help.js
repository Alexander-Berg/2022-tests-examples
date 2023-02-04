const cr = require('../../credentials.js');
const HELP = 'https://yandex.ru/support/maps-builder/concept/';
const HOTKEYS = 'https://yandex.ru/support/maps-builder/concept/hotkey.html';
const FEEDBACK = 'https://tech.yandex.ru/maps/doc/geocoder/desc/feedback/troubleshooting-docpage/';
const NK = 'https://nmaps.tst.maps.yandex.ru/';
const EXPORT_HELP = 'https://yandex.ru/support/maps-builder/concept/markers_3.html';

require('../helper.js')(afterEach);

describe('Меню помощи', () => {
    const helpPopup = PO.helpPopup;

    it('В приветственном окне', function () {
        return this.browser
            .crInit()
            .crShouldBeVisible(PO.help())
            .crShouldNotBeVisible(helpPopup())
            .crCheckHelpMenuLink(helpPopup.help(), HELP, 'Сломана ссылка на помощь')
            .crCheckHelpMenuLink(helpPopup.hotkeys(), HOTKEYS, 'Сломана ссылка на горячие клавиши')
            .crCheckHelpMenuLink(helpPopup.feedback(), FEEDBACK, 'Сломана ссылка на фидбек')
            .crCheckHelpMenuLink(helpPopup.NK(), NK, 'Сломана ссылка на НК', {skipHash: true});
    });

    it('В списке карт', function () {
        return this.browser
            .crInit('MANY_MAPS', '', cr.mapState.list)
            .crShouldBeVisible(PO.help())
            .crShouldNotBeVisible(helpPopup())
            .crCheckHelpMenuLink(helpPopup.help(), HELP, 'Сломана ссылка на помощь')
            .crCheckHelpMenuLink(helpPopup.hotkeys(), HOTKEYS, 'Сломана ссылка на горячие клавиши')
            .crCheckHelpMenuLink(helpPopup.feedback(), FEEDBACK, 'Сломана ссылка на фидбек')
            .crCheckHelpMenuLink(helpPopup.NK(), NK, 'Сломана ссылка на НК', {skipHash: true})
            .click(PO.mapSelection.close())
            .crLogout();
    });

    it('На карте до сохранения', function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crShouldBeVisible(PO.help())
            .crShouldNotBeVisible(helpPopup())
            .crCheckHelpMenuLink(helpPopup.help(), HELP, 'Сломана ссылка на помощь')
            .crCheckHelpMenuLink(helpPopup.hotkeys(), HOTKEYS, 'Сломана ссылка на горячие клавиши')
            .crCheckHelpMenuLink(helpPopup.feedback(), FEEDBACK, 'Сломана ссылка на фидбек')
            .crCheckHelpMenuLink(helpPopup.NK(), NK, 'Сломана ссылка на НК', {skipHash: true})
            .crLogout();
    });

    it('В окне экспорта', function () {
        return this.browser
            .crInit('MANY_MAPS', cr.linkMap)
            .crSaveMap()
            .click(PO.exportButton())
            .crWaitForVisible(PO.exportModal(), 'Не открылось окно экспорта')
            .crShouldBeVisible(PO.help())
            .crShouldNotBeVisible(helpPopup())
            .crCheckHelpMenuLink(helpPopup.help(), EXPORT_HELP, 'Сломана ссылка на помощь экспорта')
            .crCheckHelpMenuLink(helpPopup.hotkeys(), HOTKEYS, 'Сломана ссылка на горячие клавиши')
            .crCheckHelpMenuLink(helpPopup.feedback(), FEEDBACK, 'Сломана ссылка на фидбек')
            .crCheckHelpMenuLink(helpPopup.NK(), NK, 'Сломана ссылка на НК', {skipHash: true})
            .click(PO.exportModal.close())
            .crLogout();
    });

    it('Закрытие по повторному клику', function () {
        return this.browser
            .crInit()
            .crShouldBeVisible(PO.help())
            .crShouldNotBeVisible(helpPopup())
            .click(PO.help())
            .crShouldBeVisible(helpPopup())
            .click(PO.help())
            .crWaitForHidden(helpPopup(), 1000, 'Меню хелпа не закрылось');
    });

    it('Закрытие по клику вне меню', function () {
        return this.browser
            .crInit()
            .crShouldBeVisible(PO.help())
            .crShouldNotBeVisible(helpPopup())
            .click(PO.help())
            .crShouldBeVisible(helpPopup())
            .click(PO.modalCell())
            .crWaitForHidden(helpPopup(), 1000, 'Меню хелпа не закрылось 1')
            .click(PO.help())
            .crShouldBeVisible(helpPopup())
            .click(PO.promo())
            .crWaitForHidden(helpPopup(), 1000, 'Меню хелпа не закрылось 2');
    });
});
