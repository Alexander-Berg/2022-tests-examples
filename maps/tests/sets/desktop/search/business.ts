import cssSelectors from '../../../common/css-selectors';
import getSelectorByText from '../../../../tests/lib/func/get-selector-by-text';

describe('Поиск организации.', () => {
    beforeEach(async function () {
        await this.browser.setViewportSize({width: 1920, height: 1080});
    });

    describe('Нулевой саджест', () => {
        it('Рубрикатор', async function () {
            await openPageById(this.browser, 1062772148);
            await this.browser.waitForVisible(cssSelectors.search.form.input);
            await this.browser.waitAndClick(cssSelectors.search.form.input);
            await this.browser.waitForVisible(cssSelectors.search.form.catalog);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.search.form.catalog, 'suggest/zero-suggest');
        });
    });

    describe('Карточка.', () => {
        beforeEach(async function () {
            return openPageById(this.browser, 1062772148, '&where=37.587874,55.733670&ll=37.587873,55.733670&z=16');
        });

        it('Содержит все нужные элементы', async function () {
            await this.browser.setViewportSize({width: 1920, height: 2160});
            await openPageById(this.browser, 83999551334);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'business-card');
        });

        it('После скролла таббар остается под шапкой', async function () {
            await this.browser.openPage('/org/1062772148');
            await this.browser.scrollIntoView(cssSelectors.search.businessCard.address.view, {vertical: 'center'});
            await this.browser.waitForVisible(cssSelectors.search.businessCard.address.view);
            await this.browser.waitAndVerifyScreenshot(
                [cssSelectors.orgpage.header.wrapper, cssSelectors.tabs.container],
                'after-scroll'
            );
        });

        it('Геопродуктовая карточка содержит все нужные элементы', async function () {
            await this.browser.setViewportSize({width: 1920, height: 2160});
            await openPageById(this.browser, 71690530052);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'business-card-geoproduct');
        });

        it('Клик во время работы открывает и закрывает диалог', async function () {
            await this.browser.waitAndClick(cssSelectors.search.businessCard.hours.text);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.dialog.content, 'working-status-dialog-opened');
            await this.browser.waitAndClick(cssSelectors.search.businessCard.hours.closeButton);
            await this.browser.waitAndVerifyScreenshot(
                cssSelectors.search.businessCard.hours.text,
                'working-status-dialog-closed'
            );
        });

        it('Клик в фото открывает фотогалерею', async function () {
            await this.browser.waitAndClick(cssSelectors.search.businessCard.photos);
            await this.browser.waitForVisible(cssSelectors.photo.player);
        });

        // Можно выключить nativeEvents, но тогда ломается всё остальное
        it('Клик в телефон открывает и закрывает дропдаун', async function () {
            await openPageById(this.browser, 1124715036);
            await this.browser.waitAndClick(cssSelectors.search.businessCard.phones.show);
            await this.browser.waitAndClick(cssSelectors.search.businessCard.phones.control);
            await this.browser.waitForVisible(cssSelectors.search.businessCard.phones.content);
            await this.browser.scrollIntoView(cssSelectors.search.businessCard.phones.view, {vertical: 'center'});
            await this.browser.waitAndVerifyScreenshot(cssSelectors.search.businessCard.phones.view, 'phones-opened');
            await this.browser.waitAndClick(cssSelectors.search.businessCard.phones.control);
            await this.browser.waitForHidden(cssSelectors.search.businessCard.phones.content);
            await this.browser.scrollIntoView(cssSelectors.search.businessCard.phones.view, {vertical: 'center'});
            await this.browser.waitAndVerifyScreenshot(cssSelectors.search.businessCard.phones.view, 'phones-closed');
        });

        it('Клик в крестик закрывает карточку', async function () {
            await this.browser.waitAndClick(cssSelectors.closeButton);
            await this.browser.waitForHidden(cssSelectors.search.businessCard.view);
            await this.browser.waitForHidden(cssSelectors.search.placemark.active);
        });

        it('Клик в кнопку “К результатам поиска” открывает список результатов', async function () {
            await this.browser.waitAndClick(cssSelectors.backButton);
            await this.browser.waitForVisible(cssSelectors.search.list.view);
        });

        it('Клик по ссылке “Все филиалы сети” открывает все организации', async function () {
            const url =
                '?ll=37.587874,55.733670&z=16&mode=search&text=кафе&sll=37.587874,55.733670' +
                '&sspn=0.021651,0.006066&ol=biz&oid=1151417069';
            await this.browser.openPage(url);
            await this.browser.waitForVisible(cssSelectors.search.businessCard.view);
            await this.browser.waitAndClick(cssSelectors.search.businessCard.chain);
            await this.browser.waitAndCheckValue(cssSelectors.search.input, 'Хлеб Насущный');
            await this.browser.waitForVisible(cssSelectors.search.list.view);
            await this.browser.verifyTitle('Хлеб Насущный — Яндекс Карты');
        });

        it('Клик по ссылке "Псмотреть экспонаты" открывает страницу экспонатов', async function () {
            const url = '/org/1072168294/';
            const link = getSelectorByText('Посмотреть экспонаты', cssSelectors.search.businessCard.view);

            await this.browser.openPage(url);
            await this.browser.waitForVisible(cssSelectors.search.businessCard.view);
            await this.browser.waitAndVerifyLink(link, {value: 'goskatalog.ru', method: 'includes'});
        });

        describe('Лендинг chain.', () => {
            const url = '/213/moscow/chain/shokoladnica/6002084/';
            it('Путь остается', async function () {
                await this.browser.setViewportSize({width: 1000, height: 2000});
                await this.browser.openPage(url);
                await this.browser.waitForVisible(cssSelectors.search.list.view);
                await this.browser.waitForUrlContains({
                    path: url,
                    query: {
                        ll: '37.679391,55.745609',
                        sll: '37.679390,55.745454',
                        z: 11
                    }
                });
            });

            it('Путь исчезает', async function () {
                await this.browser.setViewportSize({width: 1000, height: 2000});
                await this.browser.openPage(url);
                await this.browser.waitForVisible(cssSelectors.search.list.view);
                await this.browser.waitAndClick(cssSelectors.search.list.snippet.viewFirst);
                await this.browser.waitForUrlContains({
                    path: '/org/shokoladnitsa/158162692868/',
                    query: {
                        ll: '37.679391,55.745609',
                        z: 11,
                        'display-text': 'Шоколадница',
                        mode: 'search',
                        sll: '37.679390,55.745454',
                        text: 'chain_id:(6002084)'
                    }
                });
            });

            it('Путь остается с 1 результатом', async function () {
                const url = '/35/krasnodar/chain/ikea/57249144/';
                await this.browser.openPage(url);
                await this.browser.waitForUrlContains({path: url});
                await this.browser.waitForElementsCount(cssSelectors.search.list.snippet.view, 1);
            });
        });

        describe('Кнопка "Входы и схема помещений".', () => {
            beforeEach(async function () {
                await this.browser.setViewportSize({width: 1024, height: 768});
                await this.browser.openPage('?ll=37.489533,55.602453&mode=search&oid=1093441083&ol=biz&z=10', {
                    enableVector: true,
                    mockVersion: '2'
                });
            });

            it('Отображается, когда есть индор', async function () {
                await this.browser.addStyles(`${cssSelectors.stickyWrapper} {position: static !important;}`);
                await this.browser.scrollIntoView(cssSelectors.search.businessCard.address.entrances);
                await this.browser.waitAndVerifyScreenshot(
                    cssSelectors.search.businessCard.address.entrances,
                    'entrances-and-indoor'
                );
            });

            it('Призумливет минимум к 17 зуму, когда есть индор', async function () {
                await this.browser.scrollIntoView(cssSelectors.search.businessCard.address.entrances);
                await this.browser.waitAndClick(cssSelectors.search.businessCard.address.entrances);
                await this.browser.waitForUrlContains({query: {z: 17}}, {partial: true});
            });
        });
    });

    describe('Карточка. Клик в исправить информацию об организации.', () => {
        const link = '?mode=search&ol=biz&oid=1750826912';

        it('Без логина открывает диалог авторизации (домен by)', async function () {
            await this.browser.openPage(link, {tld: 'by'});
            await this.browser.waitAndClick(cssSelectors.search.businessCard.edit);
            await this.browser.waitForVisible(cssSelectors.loginDialog.typeFeedback);
        });

        it('Без привязанного номера телефона открывает диалог про привязку номера (домен by)', async function () {
            await this.browser.openPage(link, {tld: 'by', fakeLogin: true});
            await this.browser.waitAndClick(cssSelectors.search.businessCard.edit);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.loginDialog.typeVerification, 'type-verification');
        });

        it('Без логина открывает форму редактирования (домены кроме by)', async function () {
            await this.browser.openPage(link);
            await this.browser.waitAndClick(cssSelectors.search.businessCard.edit);
            await this.browser.waitForVisible(cssSelectors.organizationFeedback.menu);
        });
    });

    it('Навответ [Большой театр]', async function () {
        await this.browser.openPage('?ll=37.612178,55.753445&z=10');
        await this.browser.setViewportSize({width: 1024, height: 768});
        await this.browser.waitForVisible(cssSelectors.search.input);
        await this.browser.setValueToInput(cssSelectors.search.input, 'Большой театр');
        await this.browser.waitAndClick(cssSelectors.search.searchButton);
        await this.browser.waitForVisible(cssSelectors.search.businessCard.view);
        await this.browser.waitAndCheckValue(
            cssSelectors.search.businessCard.title,
            'Государственный академический Большой театр России, Историческая сцена'
        );
        await this.browser.waitForUrlContains(
            {
                query: {
                    ll: '37.618725,55.759864',
                    z: '16'
                }
            },
            {partial: true}
        );
        await this.browser.waitForVisible(cssSelectors.search.placemark.active);
    });

    describe('Закрытая карточка.', () => {
        const oid = '1310945158';
        const url = `?mode=search&ol=biz&oid=${oid}`;

        beforeEach(async function () {
            await this.browser.openPage(url);
            await this.browser.waitForVisible(cssSelectors.search.businessCard.view);
        });
    });

    describe('Витрина.', () => {
        it('Клик по ссылке «Посмотреть все товары и услуги» ведёт в таб «Меню»/«Товары и услуги»', async function () {
            await this.browser.openPage('?mode=search&oid=1707122139&ol=biz&z=18');
            await this.browser.waitForVisible(cssSelectors.search.businessCard.specialOffers.view);
            await this.browser.waitAndClick(cssSelectors.search.businessCard.menuLink);
            await this.browser.waitForVisible(cssSelectors.search.businessCard.menu.tab);
        });
    });

    describe('Лекарства', () => {
        beforeEach(async function () {
            await openPageById(this.browser, 155581526297, '&text=парацетамол');
            await this.browser.waitForVisible(cssSelectors.search.businessCard.drugs.view);
        });

        it('Содержит все нужные элементы', async function () {
            await this.browser.waitAndVerifyScreenshot(
                cssSelectors.search.businessCard.drugs.view,
                'drugs/found-in-list'
            );
        });
    });

    describe('Признак скрытия "Стать владельцем" (hide_claim_organisation).', () => {
        describe('Присутствует.', () => {
            const url = '?mode=search&ol=biz&oid=131834249212';

            beforeEach(async function () {
                await this.browser.setViewportSize({width: 1000, height: 2000});
            });

            it('"Стать владельцем" в подвале не отображается', async function () {
                await this.browser.openPage(url);
                await this.browser.scrollIntoView(cssSelectors.search.businessCard.footer.view);
                await this.browser.waitForHidden(cssSelectors.search.businessCard.footer.becomeOwnerLink);
            });

            it('"Исправить информацию об организации" не отображается', async function () {
                await this.browser.openPage(url);
                await this.browser.waitForHidden(cssSelectors.search.businessCard.edit);
            });
        });

        describe('Отсутствует.', () => {
            const url = '?mode=search&ol=biz&oid=104319971557';

            beforeEach(async function () {
                await this.browser.setViewportSize({width: 1000, height: 2000});
            });

            it('"Стать владельцем" в подвале отображается', async function () {
                await this.browser.openPage(url);
                await this.browser.scrollIntoView(cssSelectors.search.businessCard.footer.becomeOwnerLink);
                await this.browser.waitAndVerifyScreenshot(
                    cssSelectors.search.businessCard.footer.becomeOwnerLink,
                    'footer/become-owner'
                );
            });

            it('"Исправить информацию об организации" отображается', async function () {
                await this.browser.openPage(url);
                await this.browser.waitForVisible(cssSelectors.search.businessCard.edit);
            });
        });
    });
});

async function openPageById(browser: WebdriverIO.Browser, id: number, extraUrl = ''): Promise<void> {
    await browser.openPage(`?mode=search&ol=biz&oid=${id}${extraUrl}`, {mockToday: '2018-12-06'});
    await browser.waitForVisible(cssSelectors.search.businessCard.view);
}
