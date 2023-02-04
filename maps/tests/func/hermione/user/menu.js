const PASSPORT = 'https://passport.yandex.ru/auth';
const DISK = 'https://disk.yandex.ru/client/disk/';
const TUNE = 'https://yandex.ru/tune/';
const PASSPORT2 = 'https://passport.yandex.ru/';
const cr = require('../../credentials.js');

require('../helper.js')(afterEach);

describe('Меню пользователя', () => {
    beforeEach(function () {
        return this.browser
            .crInit('MANY_MAPS')
            .crShouldBeVisible(PO.userData.username())
            .crShouldBeVisible(PO.userData.pic());
    });

    afterEach(function () {
        return this.browser
            .crLogout();
    });

    it('Открытие меню пользователя по клику на аватарку', function () {
        return this.browser
            .click(PO.userData.pic())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось');
    });

    it('Открытие меню пользователя по клику на username', function () {
        return this.browser
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось');
    });

    it('Многократное открытие и закрытие меню пользователя', function () {
        return this.browser
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось 1')
            .click(PO.userData.username())
            .crWaitForHidden(PO.userMenu(), 1000, 'Не закрылось меню пользователя 1')
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось 2')
            .click(PO.userData.username())
            .crWaitForHidden(PO.userMenu(), 1000, 'Не закрылось меню пользователя 2')
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось 3')
            .click(PO.userData.pic())
            .crWaitForHidden(PO.userMenu(), 1000, 'Не закрылось меню пользователя 3')
            .click(PO.userData.pic())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось 4')
            .click(PO.userData.pic())
            .crWaitForHidden(PO.userMenu(), 1000, 'Не закрылось меню пользователя 4')
            .click(PO.userData.pic())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось 5')
            .click(PO.userData.pic())
            .crWaitForHidden(PO.userMenu(), 1000, 'Не закрылось меню пользователя 5');
    });

    it('Вернуться на сервис из паспорта', function () {
        return this.browser
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось')
            .click(PO.userMenu.addUser())
            .waitForVisible(PO.passport(), 'Паспорт не открылся')
            .click(PO.passport.return())
            .crWaitForVisible(PO.stepMapselection(), 'Не открылся список карт');
    });

    it('Ссылка на Диск', function () {
        return this.browser
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось')
            .crCheckLink(PO.userMenu.disk())
            .then((url) => this.browser
                .crCheckURL(url, DISK, 'Сломана ссылка на Диск')
            );
    });

    it('Ссылка Настройки', function () {
        return this.browser
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось')
            .crCheckLink(PO.userMenu.tune())
            .then((url) => this.browser
                .crCheckURL(url, TUNE, 'Сломана ссылка на Настройки')
            );
    });

    it('Ссылка на Паспорт', function () {
        return this.browser
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось')
            .crCheckLink(PO.userMenu.passport())
            .then((url) => this.browser
                .crCheckURL(url, PASSPORT2, 'Сломана ссылка на Паспорт')
            );
    });

    it('Закрыть по клику вне меню', function () {
        return this.browser
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось 1')
            .click(PO.header())
            .crWaitForHidden(PO.userMenu(), 1000, 'Не закрылось меню пользователя 1')
            .click(PO.userData.username())
            .crWaitForVisible(PO.userMenu(), 'Меню пользователя не открылось 2')
            .click(PO.ymaps.map())
            .crWaitForHidden(PO.userMenu(), 1000, 'Не закрылось меню пользователя 2');
    });
});
