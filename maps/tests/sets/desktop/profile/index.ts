import cssSelectors from '../../../common/css-selectors';
import getSelectorByText from '../../../lib/func/get-selector-by-text';

const shouldBeHiddenSelector = [
    cssSelectors.contentPanelHeader.view,
    cssSelectors.orgpage.breadcrumbs,
    cssSelectors.cardFooter.view,
    cssSelectors.maillist.view,
    cssSelectors.search.businessCard.similarOrgs.carousel,
    cssSelectors.mapControls.controlGroup,
    cssSelectors.search.placemark.lowRelevant,
    cssSelectors.orgpage.faq.view,
    cssSelectors.sidebarToggleButton.collapse,
    cssSelectors.actionBar.sendToPhoneButton,
    cssSelectors.actionBar.bookmarkButton
].join(', ');

const ignoreElements = [cssSelectors.search.businessCard.histogram.view];

describe('Профиль организации.', () => {
    hermione.also.in(['chrome-dark', 'iphone-dark']);
    describe('Скриншоты.', () => {
        beforeEach(async function () {
            await this.browser.setViewportSize({width: 1100, height: 4000});
        });

        it('Обычный', async function () {
            await this.browser.openPage('/1124715036', {application: 'profile', mockToday: '2021-04-15 13:00'});
            await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'base', {ignoreElements});
        });

        it('Геопродукт из директа', async function () {
            await this.browser.openPage('/1171919284?no_similar=true', {application: 'profile'});
            await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'gp-from-direct', {ignoreElements});
        });
    });

    describe('Обычный.', () => {
        it('Отсутствуют элементы', async function () {
            await this.browser.openPage('/1124715036/?ll=37.588144,55.733842&z=17', {application: 'profile'});
            await this.browser.waitForVisible(cssSelectors.search.businessCard.view);

            await this.browser.waitAndClickInCenter(cssSelectors.orgpage.header.address, {simulateClick: true});
            await this.browser.waitForNotAppear(cssSelectors.search.toponymCard.view);

            await this.browser.waitForNotAppear(shouldBeHiddenSelector);

            // проверка карты
            await this.browser.simulateGeoClick({
                point: [37.588071, 55.734949],
                description: 'Кликнуть в пои "Магазин и музей Яндекса"'
            });
            await this.browser.waitForNotAppear(
                getSelectorByText('Магазин и музей Яндекса', cssSelectors.search.businessCard.title)
            );
        });

        it('Параметр intent=photo открывает таб фото', async function () {
            await this.browser.openPage('/210656549210?intent=photo', {application: 'profile'});
            await this.browser.waitForVisible(`${cssSelectors.tabs.photosTabTitle}._selected`);
        });

        it('Параметр intent=reviews открывает таб отзывов', async function () {
            await this.browser.openPage('/210656549210?intent=reviews', {application: 'profile'});
            await this.browser.waitForVisible(`${cssSelectors.tabs.reviewsTabTitle}._selected`);
        });

        it('Редирект профиля гостиницы в Я.Путешествия', async function () {
            await this.browser.openPage('/210656549210', {
                application: 'profile',
                readySelector: 'body',
                ignoreMapReady: true
            });
            await this.browser.verifyUrl(/^https:\/\/travel.yandex.ru.*/);
        });
    });

    describe('С параметром no_similar=true (из директа)', () => {
        it('Отсутствуют элементы', async function () {
            await this.browser.openPage('/1171919284?no_similar=true', {application: 'profile'});
            await this.browser.waitForNotAppear(cssSelectors.bannerView);
            await this.browser.waitForNotAppear(cssSelectors.search.businessCard.footer.advertLink);
        });

        it('Отсутствует реклама в галерее фото', async function () {
            await this.browser.openPage('/1171919284?no_similar=true', {application: 'profile'});
            await this.browser.waitAndClick(cssSelectors.orgpage.photo.item);
            await this.browser.waitAndClick(cssSelectors.photo.closeButton);
            await this.browser.waitForNotAppear(cssSelectors.photo.player + ' ' + cssSelectors.bannerView);
        });

        it('Отсутствует реклама в маршруте', async function () {
            await this.browser.openPage('/1788984433?no_similar=true', {application: 'profile'});
            await this.browser.waitAndClick(cssSelectors.actionBar.routeButton);
            await this.browser.waitForNotAppear(cssSelectors.bannerView);
            await this.browser.setValueToInput(cssSelectors.routes.firstInput.input, 'Фонвизинская');
            await this.browser.waitAndClick(cssSelectors.routes.firstInput.firstSuggestItem);
            await this.browser.waitAndClick(cssSelectors.routes.routeList.autoSnippet.detailedButton);
            await this.browser.waitForNotAppear(cssSelectors.bannerView);
        });

        it('Отсутствуют элементы геопродукта', async function () {
            await this.browser.openPage('/1171919284?no_similar=true', {application: 'profile'});
            const shouldBeHidden = [
                cssSelectors.bannerView,
                cssSelectors.actionBar.actionButton,
                cssSelectors.cardOffer.view,
                cssSelectors.search.businessCard.verifiedBadge,
                cssSelectors.search.businessCard.advert,
                cssSelectors.orgpage.ownership.view
            ].join(', ');
            await this.browser.waitForNotAppear(shouldBeHidden);
        });

        it('Параметр bid подменяет телефон', async function () {
            await this.browser.openPage('/112824516708?no_similar=true&bid=9296326710', {application: 'profile'});
            await this.browser.waitForVisible(getSelectorByText('+7 (495) 290-96-17'));
        });

        it('Нет редиректа профиля гостиницы в Я.Путешествия', async function () {
            await this.browser.openPage('/210656549210?no_similar=true', {
                application: 'profile',
                readySelector: 'body',
                ignoreMapReady: true
            });
            await this.browser.waitForUrlContains({path: '/210656549210'}, {skipUrlControlled: true});
        });
    });

    describe('Рубрики', () => {
        checkCase('Кафе', '/6531532095');
        checkCase('Аптека', '/32282545097');
        checkCase('АЗС', '/186988628504');
        checkCase('Гостиница', '/85178642768?no_similar=true');
        checkCase('Кинотеатр', '/1201334081');
        checkCase('Салон красоты', '/1847645310');
        checkCase('Банкомат', '/1318483034');
    });
});

function checkCase(caseName: string, url: string): void {
    it(caseName, async function () {
        await this.browser.openPage(url, {application: 'profile'});
        await this.browser.waitForNotAppear(shouldBeHiddenSelector);
    });
}
