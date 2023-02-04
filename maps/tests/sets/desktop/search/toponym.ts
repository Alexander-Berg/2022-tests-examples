import cssSelectors from '../../../common/css-selectors';
import getSelectorByText from '../../../lib/func/get-selector-by-text';

describe('Поиск топонима', () => {
    beforeEach(async function () {
        await this.browser.setViewportSize({width: 1920, height: 1080});
    });

    it('[Озеро Ильмень]', async function () {
        await checkToponymCard(this.browser, 'Озеро Ильмень');
    });

    it('[улица льва толстого]', async function () {
        await checkToponymCard(this.browser, 'Улица Льва Толстого');
        await this.browser.waitForVisible(cssSelectors.search.showParkings.enabled);
        await this.browser.waitAndVerifyMapScreenshot('lva-tolstogo-geometry');
    });

    it('города [Петрозаводск]', async function () {
        await checkToponymCard(this.browser, 'Петрозаводск');
        // Ждем окончания анимации границ.
        await this.browser.pause(1000);
        await this.browser.waitAndVerifyMapScreenshot('lva-tolstogo-petrozavodsk');
    });

    it('координат [55.7600051,37.5909192]', async function () {
        const text = '55.7600051,37.5909192';
        await checkToponymCard(this.browser, text);
        await this.browser.waitForVisible(cssSelectors.search.toponymCard.view);
        await this.browser.waitAndCheckValue(cssSelectors.search.toponymCard.title, 'Гранатный переулок, 8с4');
        await this.browser.waitForUrlContains({query: {ll: /37\.5907\d+,55\.7600\d+/, z: '17'}}, {partial: true});
        await this.browser.waitForVisible(cssSelectors.search.placemark.active);
    });

    it('далекого результата [Невский проспект]', async function () {
        await this.browser.openPage('?ll=37.626388,55.750349&z=13');
        await this.browser.submitSearch('Невский проспект');
        await this.browser.waitForVisible(cssSelectors.search.list.warning);
        await this.browser.waitForVisible(getSelectorByText('Невский проспект', cssSelectors.search.list.snippet.view));
    });

    it('без результатов [увксаедщбзжювсапмиротльдбю]', async function () {
        const text = 'увксаедщбзжювсапмиротльдбю';
        await this.browser.openPage('?ll=37.612178,55.753445&z=10');
        await this.browser.waitForVisible(cssSelectors.search.input);
        await this.browser.setValueToInput(cssSelectors.search.input, text);
        await this.browser.click(cssSelectors.search.searchButton);
        await this.browser.waitForVisible(cssSelectors.search.nothingFound.view);
    });
});

async function checkToponymCard(browser: WebdriverIO.Browser, text: string): Promise<void> {
    const url = '?ll=37.612178,55.753445&z=10';
    await browser.openPage(url);
    await browser.submitSearch(text);
    await browser.waitForVisible(cssSelectors.search.toponymCard.view);
}
