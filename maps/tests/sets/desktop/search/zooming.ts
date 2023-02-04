import cssSelectors from '../../../common/css-selectors';

describe('Призумы к результатам поиска.', () => {
    beforeEach(async function () {
        await this.browser.setViewportSize({width: 1920, height: 1080});
    });

    describe('C общим предусловием.', () => {
        beforeEach(async function () {
            await this.browser.openPage('?ll=37.620393,55.753960&z=11');
            await this.browser.waitForVisible(cssSelectors.search.input);
        });

        it('К списку результатов', async function () {
            await this.browser.setValueToInput(cssSelectors.search.input, 'Кафе');
            await this.browser.waitAndClick(cssSelectors.search.searchButton);
            await this.browser.waitForUrlContains({query: {z: '13'}}, {partial: true});
        });

        it('К навответу-топониму', async function () {
            await this.browser.setValueToInput(cssSelectors.search.input, 'Москва');
            await this.browser.waitAndClick(cssSelectors.search.searchButton);
            await this.browser.waitForUrlContains({query: {z: '9'}}, {partial: true});
        });

        it('К навответу-организации', async function () {
            await this.browser.setValueToInput(cssSelectors.search.input, 'Бардак кафе');
            await this.browser.waitAndClick(cssSelectors.search.searchButton);
            await this.browser.waitForUrlContains({query: {z: '17'}}, {partial: true});
        });

        it('В "Возможно, вы искали"', async function () {
            await this.browser.setValueToInput(cssSelectors.search.input, 'Льва Толстого');
            await this.browser.waitAndClick(cssSelectors.search.searchButton);
            await this.browser.scrollIntoView(cssSelectors.search.list.snippet.viewLast);
            await this.browser.waitAndClick(cssSelectors.search.list.snippet.viewLast);
            await this.browser.waitForUrlContains({query: {z: '17'}}, {partial: true});
        });

        it('К похожим местам рядом', async function () {
            await this.browser.setValueToInput(cssSelectors.search.input, 'большой театр');
            await this.browser.waitAndClick(cssSelectors.search.searchButton);
            await this.browser.waitForUrlContains({query: {z: '17'}}, {partial: true});
            await this.browser.waitAndClick(cssSelectors.search.businessCard.similarOrgs.firstCarouselItem);
            await this.browser.waitForUrlContains({query: {z: '15'}}, {partial: true});
        });

        it('К навответу после поиска', async function () {
            await this.browser.setValueToInput(cssSelectors.search.input, 'Кафе');
            await this.browser.waitAndClick(cssSelectors.search.searchButton);
            await this.browser.waitForUrlContains({query: {z: '13'}}, {partial: true});
            await this.browser.setValueToInput(cssSelectors.search.input, 'Бардак кафе');
            await this.browser.waitAndClick(cssSelectors.search.searchButton);
            await this.browser.waitForUrlContains({query: {z: '17'}}, {partial: true});
        });
    });

    it('Центр и зум не меняются, когда они заранее правильные', async function () {
        await this.browser.openPage('?ll=37.616148,55.752845&z=14');
        await this.browser.setValueToInput(cssSelectors.search.input, 'Кафе');
        await this.browser.waitAndClick(cssSelectors.search.searchButton);
        await this.browser.waitForVisible(cssSelectors.search.list.view);
        await this.browser.waitForUrlContains({query: {ll: '37.616148,55.752845', z: '14'}}, {partial: true});
    });
});
