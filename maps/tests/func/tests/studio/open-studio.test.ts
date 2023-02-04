import {waitForSelector, clickToSelector, waitForRemoved, openStudio, hoverOverSelector} from '../../utils/commands';

const SELECTORS = {
    outside: 'body',
    expectedPageSelectors: {
        sidebarLeft: '.sidebar__left',
        sidebarRight: '.sidebar__right',
        mapMenu: '.map-menu',
        mapStatusBar: '.map-statusbar-view',
        bugButton: '.report-button-view'
    },
    mainMenu: {
        title: '.main-menu__title',
        popup: '.popup__content .main-menu__menu',
        items: {
            save: '.main-menu__menu > .gui-submenu-item._save',
            dataSetsManager: '.main-menu__menu > .gui-submenu-item._dataSetsManager',
            copyStyleset: '.main-menu__menu > .gui-submenu-item._copyStyleset',
            clearLocalChanges: '.main-menu__menu > .gui-submenu-item._clearLocalChanges',
            renameBranch: '.main-menu__menu > .gui-submenu-item._renameBranch',
            exportMapTitle: '.main-menu__menu > .gui-submenu-item._exportMapTitle',
            getMProUrl: '.main-menu__menu > .gui-submenu-item._getMProUrl',
            notifications: '.main-menu__menu > .gui-submenu-item._notifications'
        }
    },
    mapMenu: {
        container: '.map-menu',
        helpMenuItem: '.map-menu .map-menu__help',
        help: {
            container: '.help-view',
            guideLink: '.help-view__link._guide',
            nmapsLink: '.help-view__link._nmaps',
            mapsLink: '.help-view__link._maps'
        }
    }
};

describe('Studio page', () => {
    beforeEach(() => openStudio());

    // https://testpalm.yandex-team.ru/testcase/gryadka-7
    test('should show studio page', () => {
        return Promise.all(Object.values(SELECTORS.expectedPageSelectors).map(waitForSelector));
    });

    // https://testpalm.yandex-team.ru/testcase/gryadka-9
    describe('Main menu.', () => {
        beforeEach(async () => {
            await clickToSelector(SELECTORS.mainMenu.title);
            await waitForSelector(SELECTORS.mainMenu.popup);
        });

        test('should show main menu', () => Promise.all(Object.values(SELECTORS.mainMenu.items).map(waitForSelector)));

        test('should close menu on title click', async () => {
            await clickToSelector(SELECTORS.mainMenu.title);
            await waitForRemoved(SELECTORS.mainMenu.popup);
        });

        test('should close menu on outside click', async () => {
            await clickToSelector(SELECTORS.outside);
            await waitForRemoved(SELECTORS.mainMenu.popup);
        });
    });

    test('Открытие списка Хоткеев', async () => {
        await waitForSelector(SELECTORS.mapMenu.container);
        await hoverOverSelector(SELECTORS.mapMenu.helpMenuItem);
        await Promise.all(Object.values(SELECTORS.mapMenu.help).map(waitForSelector));
    });
});
