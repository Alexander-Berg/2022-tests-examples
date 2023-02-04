import cssSelectors from '../../../common/css-selectors';

describe('Саджест поиска.', () => {
    beforeEach(async function () {
        await this.browser.openPage('?ll=37.620393,55.753960&z=10');
    });

    hermione.also.in('chrome-dark');
    it('Скриншот', async function () {
        await this.browser.setValueToInput(cssSelectors.search.form.input, 'Каф');
        await this.browser.waitAndVerifyScreenshot(cssSelectors.search.suggest.panel, 'panel');
    });

    it('Выбор саджеста с клавиатуры', async function () {
        await this.browser.setValueToInput(cssSelectors.search.form.input, 'Кафе');
        await this.browser.keys('ArrowDown');
        await this.browser.keys('Enter');
        await this.browser.waitForVisible(cssSelectors.search.list.view);
    });

    it('Саджест организации', async function () {
        await this.browser.setValueToInput(cssSelectors.search.form.input, 'московский планетр');
        await this.browser.waitAndClick(cssSelectors.search.suggest.firstItem);
        await this.browser.waitForVisible(cssSelectors.search.businessCard.view);
        await this.browser.waitAndCheckValue(cssSelectors.search.businessCard.title, 'Московский планетарий');
    });

    it('Скрытие панели и рубричный саджест', async function () {
        await this.browser.waitAndClick(cssSelectors.search.miniCatalogFirstItem);
        await this.browser.waitForVisible(cssSelectors.search.list.view);
    });

    it('Рубричный саджест', async function () {
        await this.browser.waitAndClick(cssSelectors.search.miniCatalogFirstItem);
        await this.browser.waitForVisible(cssSelectors.search.list.view);
    });

    it('Изменение <title> при переходе на карточку через саджест', async function () {
        await this.browser.setValueToInput(cssSelectors.search.form.input, 'vjcrjdcrbq rhtv');
        await this.browser.keys('ArrowDown');
        await this.browser.keys('Enter');
        await this.browser.waitForVisible(cssSelectors.search.businessCard.view);
        await this.browser.verifyTitle('Московский Кремль — Яндекс Карты');
    });
});
