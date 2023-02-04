import cssSelectors from '../../../common/css-selectors';
import getSelectorByText from '../../../../tests/lib/func/get-selector-by-text';

const bookmarksSelectors = cssSelectors.bookmarks;

describe('Закладки.', () => {
    afterEach(async function () {
        await this.browser.logout();
    });

    it('Открыть закладки через меню', async function () {
        await this.browser.openPage('', {userId: 'common'});
        await this.browser.waitAndClick(cssSelectors.mapControls.menu.control);
        await this.browser.waitAndClick(getSelectorByText('Закладки', cssSelectors.profile.panel));
        await this.browser.waitForVisible(bookmarksSelectors.panel);
    });

    describe('Панель.', () => {
        beforeEach(async function () {
            await this.browser.openPage('?bookmarks=true&mode=bookmarks', {userId: 'common'});
            await this.browser.waitForVisible(bookmarksSelectors.panel);
        });

        hermione.also.in('chrome-dark');
        it('Вход в "Закладки" пользователем без закладок', async function () {
            await this.browser.moveToObject(bookmarksSelectors.panel);
            await this.browser.waitAndVerifyScreenshot(bookmarksSelectors.panel, 'empty-panel');
        });

        it('Закрытие панели "Закладки"', async function () {
            await this.browser.waitAndClick(cssSelectors.closeButton);
            await this.browser.waitForHidden(bookmarksSelectors.panel);
        });

        describe('Кнопка сворачивания панелей', () => {
            it('Должна сворачивать панель', async function () {
                await this.browser.waitAndClick(cssSelectors.sidebarToggleButton.collapse);
                await this.browser.waitForHidden(bookmarksSelectors.panel);
                await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebarToggleButton.expand, 'expand-button');
            });

            it('Должна разворачивать свернутую панель', async function () {
                await this.browser.waitAndClick(cssSelectors.sidebarToggleButton.collapse);
                await this.browser.waitAndClick(cssSelectors.sidebarToggleButton.expand);
                await this.browser.waitForVisible(bookmarksSelectors.panel);
                await this.browser.waitAndVerifyScreenshot(
                    cssSelectors.sidebarToggleButton.collapse,
                    'collapse-button'
                );
            });
        });
    });

    describe('Дом/работа.', () => {
        beforeEach(async function () {
            const url = '?bookmarks=true&mode=bookmarks&ll=37.620393%2C55.753960&z=10';
            await this.browser.openPage(url, {userId: 'withHomeAndWork'});
        });

        it('Правильно отображется сниппет при добавленном адресе дома', async function () {
            await this.browser.waitAndVerifyScreenshot(bookmarksSelectors.home.view, 'home-exist-snippet');
        });

        it('Правильно отображется сниппет при добавленном адресе работы', async function () {
            await this.browser.waitAndVerifyScreenshot(bookmarksSelectors.work.view, 'work-exist-snippet');
        });

        it('Открывается меню по клику в три точки', async function () {
            await this.browser.waitAndClick(bookmarksSelectors.home.edit);
            await this.browser.waitForElementsCount(
                bookmarksSelectors.actionsMenu + ' ' + cssSelectors.listItem.view,
                3
            );
            await this.browser.waitForVisible(getSelectorByText('Построить маршрут', bookmarksSelectors.actionsMenu));
            await this.browser.waitForVisible(getSelectorByText('Изменить', bookmarksSelectors.actionsMenu));
            await this.browser.waitForVisible(getSelectorByText('Удалить', bookmarksSelectors.actionsMenu));
        });

        it('Открывается панель маршрутов до дома', async function () {
            await this.browser.waitAndClick(bookmarksSelectors.home.edit);
            await this.browser.waitAndClick(getSelectorByText('Построить маршрут', bookmarksSelectors.actionsMenu));
            await this.browser.waitForVisible(cssSelectors.routes.sidebarPanel);
            await this.browser.waitAndCheckValue(cssSelectors.routes.firstInput.input, '');
            await this.browser.waitAndCheckValue(cssSelectors.routes.secondInput.input, 'Дом');
        });

        it('Открывает панель маршрутов до работы', async function () {
            await this.browser.waitAndClick(bookmarksSelectors.work.edit);
            await this.browser.waitAndClick(getSelectorByText('Построить маршрут', bookmarksSelectors.actionsMenu));
            await this.browser.waitForVisible(cssSelectors.routes.sidebarPanel);
            await this.browser.waitAndCheckValue(cssSelectors.routes.firstInput.input, '');
            await this.browser.waitAndCheckValue(cssSelectors.routes.secondInput.input, 'Работа');
        });
    });

    describe('Карта.', () => {
        it('Открытие карточки закладки-топонима кликом в метку', async function () {
            await this.browser.openPage('?bookmarks=true&mode=bookmarks&ll=37.637406%2C55.739991&z=17', {
                userId: 'bookmarks'
            });
            await this.browser.waitForVisible(bookmarksSelectors.panel);
            await this.browser.waitForVisible(cssSelectors.map.container);
            await this.browser.simulateGeoClick({
                point: [37.637407, 55.740031],
                description: 'Кликнуть в метку закладки на карте'
            });
            await this.browser.waitForVisible(cssSelectors.search.toponymCard.view);
        });

        it('Открытие карточки закладки Работы кликом в метку', async function () {
            await this.browser.openPage('?bookmarks=true&mode=bookmarks&ll=37.610107,55.743634&z=17', {
                userId: 'bookmarks'
            });
            await this.browser.waitForVisible(bookmarksSelectors.panel);
            await this.browser.waitAndHover(bookmarksSelectors.placemark.place);
            await this.browser.waitForVisible(bookmarksSelectors.hoveredSnippet);
            await this.browser.waitAndClick(bookmarksSelectors.placemark.place);
            await this.browser.waitForVisible(cssSelectors.search.toponymCard.view);
        });
    });

    describe('Пользователь без списка закладок.', () => {
        beforeEach(async function () {
            const url = '?bookmarks=true&mode=bookmarks&ll=37.620393,55.753960&z=13';
            await this.browser.openPage(url, {userId: 'bookmarksWithoutLists'});
            await this.browser.waitForVisible(bookmarksSelectors.panel);
            await this.browser.waitForHidden(bookmarksSelectors.loadingSnippet);
        });

        it('Вход в "Закладки" пользователем без списков закладок', async function () {
            await this.browser.setViewportSize({width: 1200, height: 800});
            await this.browser.waitAndClick(cssSelectors.mapControls.menu.control);
            await this.browser.waitAndClick(getSelectorByText('Закладки', cssSelectors.profile.panel));
            await this.browser.waitForHidden(getSelectorByText('Закладки', cssSelectors.profile.panel));
            await this.browser.addStyles(`${bookmarksSelectors.snippetPhoto} {visibility: hidden;}`);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.bookmarks.panel, 'without-lists');
            await this.browser.waitAndVerifyMapScreenshot('without-lists-map');
        });

        it('Открытие карточки закладки-организации кликом в сниппет', async function () {
            await this.browser.waitAndClick(getSelectorByText('Избранное', cssSelectors.bookmarks.folderSnippet.view));
            await this.browser.waitAndClick(bookmarksSelectors.snippet);
            await this.browser.waitForVisible(cssSelectors.search.businessCard.view);
        });

        it('Добавление Дома с панели Закладки кликом в карту', async function () {
            const address = 'Красная площадь';
            await this.browser.waitAndClick(bookmarksSelectors.home.add);
            await this.browser.waitAndVerifyScreenshot(bookmarksSelectors.confirmation.view, 'home-add-dialog');
            await this.browser.simulateGeoClick({
                point: [37.620393, 55.75396],
                description: 'Клик в центр карты (Красная площадь)'
            });
            await this.browser.waitAndCheckValue(bookmarksSelectors.confirmation.input, address, {timeout: 3000});
            await this.browser.waitAndClick(bookmarksSelectors.confirmation.submitButton);
            await this.browser.waitForHidden(bookmarksSelectors.confirmation.submitButton);
            await this.browser.waitForVisible(bookmarksSelectors.placemark.place);
            await this.browser.waitAndCheckValue(bookmarksSelectors.home.address, address);
        });

        it('Добавление Дома с панели Закладки выбором из саджеста', async function () {
            await this.browser.waitAndClick(bookmarksSelectors.home.add);
            await this.browser.waitAndVerifyScreenshot(bookmarksSelectors.confirmation.view, 'home-add-dialog');
            await this.browser.setValueToInput(bookmarksSelectors.confirmation.input, 'Фрунзе 20 москва');
            await this.browser.waitAndClick(bookmarksSelectors.suggestItem);
            await this.browser.waitForHidden(bookmarksSelectors.suggest);
            await this.browser.waitAndClick(bookmarksSelectors.confirmation.submitButton);
            await this.browser.waitForHidden(bookmarksSelectors.confirmation.submitButton);
            await this.browser.waitForVisible(bookmarksSelectors.placemark.place);
            await this.browser.waitAndCheckValue(bookmarksSelectors.home.address, 'улица Тимура Фрунзе, 20');
        });

        it('Добавление Работы с панели Закладки вводом адреса', async function () {
            await this.browser.waitAndClick(bookmarksSelectors.work.add);
            await this.browser.waitAndVerifyScreenshot(bookmarksSelectors.confirmation.view, 'work-add-dialog');
            await this.browser.setValueToInput(bookmarksSelectors.confirmation.input, 'улица Тимура Фрунзе, 20');
            await this.browser.keys('Enter');
            await this.browser.waitForHidden(bookmarksSelectors.suggest);
            await this.browser.waitAndClick(bookmarksSelectors.confirmation.submitButton);
            await this.browser.waitForHidden(bookmarksSelectors.confirmation.submitButton);
            await this.browser.waitForVisible(bookmarksSelectors.placemark.place);
            await this.browser.waitAndCheckValue(bookmarksSelectors.work.address, 'улица Тимура Фрунзе, 20');
        });

        it('Удаление закладки из карточки в панели Мои места', async function () {
            const itemsCount = 4;
            await this.browser.waitAndClick(getSelectorByText('Избранное', cssSelectors.bookmarks.folderSnippet.view));
            await this.browser.waitForElementsCount(bookmarksSelectors.snippet, itemsCount);
            await this.browser.waitAndClick(bookmarksSelectors.snippet);
            await this.browser.waitAndClick(cssSelectors.actionBar.bookmarkButtonChecked);
            await this.browser.waitAndClick(getSelectorByText('Избранное', cssSelectors.bookmarks.selectFolder.view));
            if (this.browser.isPhone) {
                await this.browser.swipeShutter('down', {parentSelector: cssSelectors.bookmarks.selectFolder.view});
            } else {
                await this.browser.waitAndClick(cssSelectors.actionBar.bookmarkButton);
            }
            await this.browser.back();
            await this.browser.waitForElementsCount(bookmarksSelectors.snippet, itemsCount - 1);
        });
    });

    it('Открытие расшаренной закладки без логина', async function () {
        await this.browser.openPage(
            '?bookmarks%5Bid%5D=e6e91cb0-41a2-4c9a-861f-07a699d6fd2f&' +
                'bookmarks%5Buri%5D=ymapsbm1%3A%2F%2Fpin%3Fll%3D37.625918%252C55.746378&' +
                'll=37.622504%2C55.753215&mode=bookmarks&z=10'
        );
        await this.browser.waitAndCheckValue(cssSelectors.search.toponymCard.title, 'Лубочный переулок');
    });
});
