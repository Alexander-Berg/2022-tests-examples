import cssSelectors from '../../../common/css-selectors';

describe('POI.', () => {
    describe('Организация.', () => {
        it('Клик по метке', async function () {
            await this.browser.openPage(`?ll=37.619856,55.753665&z=19`);
            await this.browser.simulateGeoClick({
                point: [37.619886, 55.753671],
                description: 'Кликнуть в метку мавзолея В.И. Ленина'
            });
            await this.browser.waitAndCheckValue(cssSelectors.search.businessCard.title, 'Мавзолей В.И. Ленина');
        });

        const url =
            '?ll=37.607619,55.766278&z=19&mode=poi&poi[uri]=ymapsbm1://org?oid=1062772148' +
            '&poi[point]=37.607619,55.766278';

        it('Карточка', async function () {
            await this.browser.setViewportSize({width: 1920, height: 2160});
            await this.browser.openPage('?mode=poi&poi[uri]=ymapsbm1://org?oid=1062772148', {mockToday: '2020-05-21'});
            await this.browser.waitForVisible(cssSelectors.search.placemark.active);
            await this.browser.waitForVisible(cssSelectors.search.menuView.view);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'poi-business-card');
        });

        it('Поиск филиалов', async function () {
            await this.browser.setViewportSize({width: 1920, height: 1080});
            await this.browser.openPage(url);
            await this.browser.scrollIntoView(cssSelectors.search.businessCard.chain);
            await this.browser.waitAndClick(cssSelectors.search.businessCard.chain);
            await this.browser.waitForUrlContains({query: {z: '10'}}, {partial: true});
            await this.browser.waitAndCheckValue(cssSelectors.search.input, 'VASILCHUKÍ Chaihona №1');
            await this.browser.waitForVisible(cssSelectors.search.list.view);
        });
    });
});
