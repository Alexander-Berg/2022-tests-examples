import cssSelectors from '../../../common/css-selectors';
import {parseUrl} from '../../../commands/wait-for-url-contains';

describe('Строка поиска.', () => {
    beforeEach(async function () {
        await this.browser.openPage('');
    });

    it('Поиск по кнопке', async function () {
        await this.browser.setValueToInput(cssSelectors.search.form.input, 'кафе');
        await this.browser.click(cssSelectors.search.form.button);
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.waitForVisible(cssSelectors.search.placemark.view);
    });

    it('Поиск по нажатию enter (рубрика)', async function () {
        await this.browser.setValueToInput(cssSelectors.search.form.input, 'кафе');
        await this.browser.keys('Enter');
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.waitForVisible(cssSelectors.search.placemark.view);
    });

    it('Поиск организации (сеть)', async function () {
        await this.browser.submitSearch('Макдоналдс');
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.waitForVisible(cssSelectors.search.placemark.view);
        await this.browser.waitAndCheckValue(
            cssSelectors.search.list.businessSnippet.title,
            new Array(5).fill('Макдоналдс')
        );
    });

    it('Повторный поиск', async function () {
        await this.browser.setViewportSize({width: 1440, height: 900});
        await this.browser.submitSearch('кафе');
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.clearElement(cssSelectors.search.input);
        await this.browser.submitSearch('гостиница');
        await this.browser.waitForHidden(cssSelectors.search.loadingIndicator);
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.verifyRequestId({
            lastId: '1652895143302207-2459774512-vla1-4332-vla-addrs-nmeta-new-8031',
            amount: 2
        });
    });

    it('Изменение <title> при поиске', async function () {
        await this.browser.submitSearch('кафе');
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.verifyTitle('Кафе — Яндекс Карты');
    });

    describe('Переход на лендинг /search/.', () => {
        const url = '/213/moscow/';

        it('Регион известен', async function () {
            await this.browser.submitSearch('/?кафе=в москве');
            await this.browser.waitForVisible(cssSelectors.search.list.view);
            await this.browser.waitForUrlContains({
                path: url + 'search/%2F%3F%D0%BA%D0%B0%D1%84%D0%B5%3D%D0%B2%20%D0%BC%D0%BE%D1%81%D0%BA%D0%B2%D0%B5/'
            });
        });

        it('Запрос с опечаткой', async function () {
            await this.browser.submitSearch('Рестоарн');
            await this.browser.waitForVisible(cssSelectors.search.list.view);
            await this.browser.waitForUrlContains({
                path: url + 'search/%D0%A0%D0%B5%D1%81%D1%82%D0%BE%D1%80%D0%B0%D0%BD/'
            });
        });

        it('Регион неизвестен', async function () {
            await this.browser.openPage('/?ll=46.892200,56.470640&z=6');
            await this.browser.submitSearch('кафе');
            await this.browser.waitForVisible(cssSelectors.search.list.view);
            await this.browser.waitForUrlContains({query: {mode: 'search', text: 'кафе'}}, {partial: true});
        });
    });

    describe('Поиск по сетям.', () => {
        it('Регион известен', async function () {
            await this.browser.submitSearch('Шоколадница');
            await this.browser.waitForVisible(cssSelectors.search.list.view);
            await this.browser.waitForUrlContains({path: '/213/moscow/chain/shokoladnica/6002084/'});
        });

        it('Регион неизвестен', async function () {
            await this.browser.openPage('/?ll=46.892200,56.470640&z=6');
            await this.browser.submitSearch('Шоколадница');
            await this.browser.waitForVisible(cssSelectors.search.list.view);
            await this.browser.waitForUrlContains(
                {
                    query: {
                        mode: 'search',
                        text: 'Шоколадница'
                    }
                },
                {partial: true}
            );
        });
    });

    describe('Табы поиска.', () => {
        const prevCenter = [37.553984, 55.715146].join(',');
        const url = `/213/moscow/search/Где поесть?filter=alternate_vertical:Collections&ll=${prevCenter}&z=16`;

        it('Сохраняется спан после возврата из подборки, открытой из таба "Подборки"', async function () {
            await this.browser.setViewportSize({width: 1440, height: 900});
            await this.browser.openPage(url);
            await this.browser.waitAndClick(
                cssSelectors.search.list.snippet.view + ' [data-coordinates="37.605911,55.738636"]'
            );
            await this.browser.waitForVisible(cssSelectors.discovery.view);
            await this.browser.waitForUrlContains({query: {ll: '37.606670,55.743177', z: 13}}, {partial: true});
            await this.browser.waitAndClick(cssSelectors.contentPanelHeader.headerButtons.close);
            await this.browser.waitForUrlContains({query: {ll: '37.553984,55.715146', z: 16}}, {partial: true});
        });

        it('Сохраняется выдраганный спан после возврата из подборки, открытой из таба "Подборки"', async function () {
            await this.browser.setViewportSize({width: 1440, height: 900});
            await this.browser.openPage(url);
            let draggedCenter = '';
            await this.browser.perform(async () => {
                await this.browser.dragPointerFromCenter({
                    delta: 100,
                    description: 'Драгнуть карту в любом направлении'
                });
                await this.browser.waitUntil(async () => {
                    const {query} = parseUrl(await this.browser.getUrl());
                    draggedCenter = query.ll;
                    return draggedCenter !== prevCenter;
                });
            }, 'Драгнуть карту в любом направлении и запомнить ll');
            await this.browser.waitAndClick(
                cssSelectors.search.list.snippet.view + ' [data-coordinates="37.605911,55.738636"]'
            );
            await this.browser.waitForVisible(cssSelectors.discovery.view);
            await this.browser.waitForUrlContains({query: {ll: '37.606670,55.743177', z: 13}}, {partial: true});
            await this.browser.waitAndClick(cssSelectors.contentPanelHeader.headerButtons.close);
            await this.browser.perform(async () => {
                await this.browser.waitForUrlContains({query: {ll: draggedCenter, z: 16}}, {partial: true});
            }, 'Убедиться что запомненный ll вернулся в строку состояния');
        });
    });

    it('Строка очищается после возврата с поиска', async function () {
        await this.browser.openPage('/');
        await this.browser.submitSearch('Кафе');
        await this.browser.waitForVisible(cssSelectors.search.panel);
        await this.browser.back();
        await this.browser.waitAndCheckValue(cssSelectors.search.input, '');
    });

    it('После драга карты и ответа геопоиска - строка сохраняет свое значение, если ответ пуст', async function () {
        await this.browser.openPage('/');
        await this.browser.submitSearch('brick');
        await this.browser.waitAndClickInCenter(cssSelectors.search.list.snippet.viewFirst, {simulateClick: true});
        await this.browser.dragPointerFromCenter({
            delta: 100,
            description: 'Совершить драг карты в любом направлении, чтобы изменилось начальное положение.'
        });
        await this.browser.waitAndCheckValue(cssSelectors.search.input, 'brick');
    });
});
