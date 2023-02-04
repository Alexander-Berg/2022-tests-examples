import cssSelectors from '../../../common/css-selectors';
import getSelectorByText from '../../../lib/func/get-selector-by-text';

describe('Серп.', () => {
    it('Скриншот.', async function () {
        const url = '?ll=37.312446,55.810238&z=14&mode=search&text=Кафе&sll=37.312446,55.810238&sspn=0.105572,0.041374';
        await this.browser.setViewportSize({width: 1440, height: 900});
        await this.browser.openPage(url, {mockGeolocation: 'denied', mockVersion: '1'});
        await this.browser.addStyles(`${cssSelectors.search.list.snippet.photo} {background: none !important;}`);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.search.list.view, 'search-cafe-list');
        await this.browser.waitAndVerifyMapScreenshot('search-cafe-map');
    });

    it('Закрытие', async function () {
        const url = '?ll=37.312446,55.810238&z=14&mode=search&text=Кафе&sll=37.312446,55.810238&sspn=0.105572,0.041374';
        await this.browser.setViewportSize({width: 1440, height: 900});
        await this.browser.openPage(url);
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.waitAndClick(cssSelectors.closeButton);
        await this.browser.waitForHidden(cssSelectors.search.placemark.view);
        await this.browser.waitForVisible(cssSelectors.home.view);
    });

    it('Работа со списком', async function () {
        const url = '?ll=37.593848,55.748832&z=16&mode=search&text=кафе&sll=37.593849,55.748833&sspn=0.008862,0.003540';
        // В хроме активный пин появляется не полностью при дефолтном разрешении
        await this.browser.setViewportSize({width: 1920, height: 1080});
        await this.browser.openPage(url);
        await this.browser.waitForVisible(cssSelectors.search.placemark.view);
        await this.browser.moveToObject(cssSelectors.search.list.snippet.viewFirst);
        await this.browser.waitForVisible(cssSelectors.search.list.snippet.viewFirstHovered);
        await this.browser.waitForVisible(cssSelectors.search.placemark.hover);
        await this.browser.click(cssSelectors.search.list.snippet.viewFirst);
        await this.browser.waitForVisible(cssSelectors.search.businessCard.view);
        await this.browser.waitAndCheckValue(cssSelectors.search.businessCard.title, 'Гоголь-Моголь');
    });

    it('Организация с приоритетным размещением.', async function () {
        const url =
            '?mode=search&text=Где поесть&sll=37.655395,55.734551&sspn=0.067892,0.027217' +
            '&ll=37.655396,55.734551&z=14';
        await this.browser.openPage(url);
        await this.browser.waitForVisible(cssSelectors.search.list.businessSnippet.badge);
    });

    it('Сниппет директа.', async function () {
        await this.browser.openPage(
            '/213/moscow/search/окна/?ll=37.590767%2C55.794345&sll=37.643475%2C55.814916&z=16.07'
        );
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.scrollIntoView(cssSelectors.search.list.directSnippet.view, {vertical: 'center'});
        await this.browser.waitAndVerifyScreenshot(
            cssSelectors.search.list.directSnippet.view,
            'search-direct-snippet'
        );
        await this.browser.waitAndClick(cssSelectors.search.list.directSnippet.view);
        await this.browser.waitForVisible(cssSelectors.search.directCard.view);
    });

    it('Открытие POI оставляет поисковые пины', async function () {
        const url = '?ll=37.588246,55.733655&sll=37.588246,55.733655&sspn=0.012019,0.004077&text=Кафе&z=18';
        await this.browser.setViewportSize({width: 1440, height: 900});
        await this.browser.openPage(url);
        await this.browser.waitForVisible(cssSelectors.search.panel);

        await this.browser.simulateGeoClick({
            point: [37.588155, 55.733873],
            description: 'Кликнуть в метку Яндекса'
        });
        await this.browser.waitForVisible(cssSelectors.poi.panel);
        await this.browser.waitForVisible(cssSelectors.search.placemark.active);
        await this.browser.waitForElementsCount(cssSelectors.search.placemark.view, 26);
    });

    it('Whatshere правой кнопкой после поиска сохраняет поисковые пины', async function () {
        const link = '?ll=30.040228,59.936789&mode=search&text=аптеки&z=12';
        await this.browser.setViewportSize({width: 1920, height: 1080});
        await this.browser.openPage(link, {mockGeolocation: 'denied'});
        await this.browser.waitAndClickInCenter(cssSelectors.mapBody, {rightClick: true});
        await this.browser.waitAndClick(getSelectorByText('Что здесь?', cssSelectors.contextMenu));
        await this.browser.waitForVisible(cssSelectors.search.toponymCard.view);
        await this.browser.waitForVisible(cssSelectors.search.placemark.active);
        await this.browser.waitForElementsCount(cssSelectors.search.placemark.view, 26);
    });

    it('Whatshere правой кнопкой сбрасывается поиском', async function () {
        const url = '?ll=30.404621,59.959057&z=17';
        await this.browser.setViewportSize({width: 1000, height: 2000});
        await this.browser.openPage(url);
        await this.browser.waitAndClickInCenter(cssSelectors.mapBody, {rightClick: true});
        await this.browser.waitAndClick(getSelectorByText('Что здесь?', cssSelectors.contextMenu));
        await this.browser.waitForVisible(cssSelectors.search.toponymCard.view);
        await this.browser.submitSearch('кафе');
        await this.browser.waitForHidden(cssSelectors.search.placemark.active);
    });

    it('Whatshere левой кнопкой сбрасывается поиском', async function () {
        const url = '?ll=30.404621,59.959057&z=17';
        await this.browser.setViewportSize({width: 2000, height: 1000});
        await this.browser.openPage(url);
        await this.browser.waitAndClick(cssSelectors.home.header);
        await this.browser.waitAndClickInCenter(cssSelectors.mapBody);
        await this.browser.waitAndClick(cssSelectors.whatshere.preview.title);
        await this.browser.waitForVisible(cssSelectors.search.toponymCard.view);
        await this.browser.submitSearch('кафе');
        await this.browser.waitForHidden(cssSelectors.search.placemark.active);
    });

    describe('Разделитель "Больше результатов рядом".', () => {
        it('Не отображается, когда результатов достаточно', async function () {
            const url = '??ll=37.507698,55.743515&mode=search&text=Кафе&z=19';
            await this.browser.openPage(url);
            await this.browser.dragPointerFromCenter({
                delta: 1,
                description: 'Драгнуть карту'
            });
            await this.browser.waitForHidden(cssSelectors.search.page.separator);
        });
        it('Отображается, когда результатов во вьюпорте недостаточно', async function () {
            const url = '?ll=37.495535,55.748997&mode=search&text=Кафе&z=19';
            await this.browser.openPage(url, {mockVersion: '1'});
            await this.browser.dragPointerFromCenter({
                delta: 10,
                description: 'Драгнуть карту'
            });
            await this.browser.scrollIntoView(cssSelectors.search.page.separator, {vertical: 'center'});
            await this.browser.waitAndVerifyScreenshot(cssSelectors.search.page.separator, 'search-page-separator');
        });
        it('Отображается, когда результатов нет во вьюпорте', async function () {
            const url = '?ll=37.494451,55.749221&mode=search&text=Кафе&z=19';
            await this.browser.openPage(url);
            await this.browser.dragPointerFromCenter({
                delta: 10,
                description: 'Драгнуть карту'
            });
            await this.browser.waitAndVerifyScreenshot(cssSelectors.search.page.separator, 'search-page-separator');
        });
    });

    describe('Подстрочники цен.', () => {
        it('Топливо', async function () {
            await this.browser.openPage('?ll=37.507698,55.743515&mode=search&text=Азс&z=19');
            await this.browser.waitForVisible(cssSelectors.search.list.snippet.subtitle);
            await this.browser.waitAndVerifyScreenshot(
                cssSelectors.search.list.snippet.subtitle,
                'serp/subtitles/fuel'
            );
        });

        it('Гостиница', async function () {
            await this.browser.openPage('?ll=37.507698,55.743515&mode=search&text=Отель&z=19');
            await this.browser.waitForVisible(cssSelectors.search.list.snippet.subtitle);
            await this.browser.waitAndVerifyScreenshot(
                cssSelectors.search.list.snippet.subtitle,
                'serp/subtitles/hotel'
            );
        });
    });
});
