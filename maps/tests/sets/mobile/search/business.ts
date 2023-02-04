import cssSelectors from '../../../common/css-selectors';
import {OpenPageOptions} from '../../../commands/open-page';
import getSelectorByText from '../../../../tests/lib/func/get-selector-by-text';

describe('Тач. Поиск организации', () => {
    describe('Карточка.', () => {
        it('Геопродуктовая карточка содержит все нужные элементы', async function () {
            await openPage(this.browser, 71690530052);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.sidebar.panel, 'business-card-geoproduct', {
                allowViewportOverflow: true
            });
        });

        it('Открытие панорам.', async function () {
            await openPage(this.browser, 1062772148, {mockToday: '2018-12-06'});
            await this.browser.waitAndClick(cssSelectors.search.businessCard.panorama);
            await this.browser.waitForVisible(cssSelectors.panoramas.player);
        });

        it('Клик по ссылке "Псмотреть экспонаты" открывает страницу экспонатов', async function () {
            const url = '/org/1072168294/';
            const link = getSelectorByText('Посмотреть экспонаты', cssSelectors.search.businessCard.view);
            await this.browser.openPage(url);
            await this.browser.waitForVisible(cssSelectors.search.businessCard.view);
            await this.browser.waitAndVerifyLink(link, {value: 'goskatalog.ru', method: 'includes'});
        });
    });

    describe('Нулевой саджест', () => {
        it('Рубрикатор', async function () {
            await openPage(this.browser, 1062772148);
            await this.browser.swipeShutter('down');
            await this.browser.waitForHidden(cssSelectors.sidebar.panel);
            await this.browser.waitForVisible(cssSelectors.sidebar.minicard);
            await this.browser.waitAndClick(cssSelectors.search.form.input);
            await this.browser.waitForVisible(cssSelectors.search.form.catalog);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.search.form.catalog, 'suggest/zero-suggest', {
                allowViewportOverflow: true
            });
        });
    });

    describe('Табы', () => {
        it('При клике на название текущего таба должен произойти скролл наверх', async function () {
            const nthReviewItem = cssSelectors.ugc.review.card.nthItem;
            const inActiveReviewTab = (selector: string) =>
                `${cssSelectors.tabs.visibleNthTabContent.replace('%i', '5')} ${selector}`;
            await openPage(this.browser, 1186345401);
            await this.browser.waitAndClick(cssSelectors.tabs.reviewsTabTitle);
            await this.browser.perform(async () => {
                await this.browser.waitUntil(async () => {
                    await this.browser.scrollIntoView(inActiveReviewTab(nthReviewItem.replace('%i', '4')));
                    await this.browser.waitForViewportVisibility(
                        inActiveReviewTab(nthReviewItem.replace('%i', '2')),
                        'fullyInvisible',
                        3000
                    );
                    return true;
                }, 30000);
            }, 'Проскроллить список отзывов так, чтобы первый отзыв полностью исчез из видимой области.');
            await this.browser.waitAndClick(cssSelectors.tabs.reviewsTabTitle);
            await this.browser.waitForViewportVisibility(
                inActiveReviewTab(nthReviewItem.replace('%i', '2')),
                'atLeastPartiallyVisible'
            );
        });

        it('При клике на таб "Отзывы" должна показаться панель с отзывами', async function () {
            await openPage(this.browser, 1186345401);
            await this.browser.waitAndClick(cssSelectors.tabs.reviewsTabTitle);
            await this.browser.waitForViewportVisibility(cssSelectors.ugc.review.card.view, 'atLeastPartiallyVisible');
        });

        it('При переключении между табами должна сохраняться позиция скролла', async function () {
            await openPage(this.browser, 1186345401);
            await this.browser.waitAndClick(cssSelectors.tabs.reviewsTabTitle);
            const selector =
                cssSelectors.tabs.visibleTab + ' ' + cssSelectors.ugc.review.card.nthItem.replace('%i', '3');
            await this.browser.waitForViewportVisibility(selector, 'fullyInvisible');
            await this.browser.scrollIntoView(selector);
            await this.browser.waitAndClick(cssSelectors.tabs.overviewTabTitle);
            await this.browser.waitAndClick(cssSelectors.tabs.reviewsTabTitle);
            await this.browser.waitForViewportVisibility(selector, 'atLeastPartiallyVisible');
        });

        it('При открытии лендинга /inside/ должен показаться таб «Организации внутри»', async function () {
            await this.browser.openPage('/org/1113219372/inside/');
            await this.browser.waitForViewportVisibility(
                cssSelectors.search.businessCard.placesInside.tab,
                'atLeastPartiallyVisible'
            );
            await this.browser.waitForUrlContains({path: '/org/yevropeyskiy/1113219372/inside/'});
        });

        it('При открытии лендинга /reviews/ должен показаться таб «Отзывы»', async function () {
            await this.browser.openPage('/org/batoni/103818976702/reviews/');
            await this.browser.waitForViewportVisibility(cssSelectors.ugc.review.card.view, 'atLeastPartiallyVisible');
            await this.browser.waitForUrlContains({path: '/org/batoni/103818976702/reviews/'});
        });

        it('При открытии лендинга /gallery/ должен показаться таб «Фото»', async function () {
            await this.browser.openPage('/org/batoni/103818976702/gallery/');
            await this.browser.waitForViewportVisibility(cssSelectors.photo.list, 'atLeastPartiallyVisible');
            await this.browser.waitForUrlContains({path: '/org/batoni/103818976702/gallery/'});
        });

        it('При переключении на таб «Меню» должен установиться SEO URL', async function () {
            await openPage(this.browser, 226239194869);
            await this.browser.waitAndClick(cssSelectors.tabs.menuTabTitle);
            await this.browser.waitForUrlContains({path: '/org/kroshka_kartoshka/226239194869/menu/'});
        });

        it('При переключении на таб «Отзывы» должен установиться SEO URL', async function () {
            await openPage(this.browser, 103818976702);
            await this.browser.waitAndClick(cssSelectors.tabs.reviewsTabTitle);
            await this.browser.waitForUrlContains({path: '/org/batoni/103818976702/reviews/'});
        });

        it('При переключении на таб «Фото» должен установиться SEO URL', async function () {
            await openPage(this.browser, 103818976702);
            await this.browser.waitAndClick(cssSelectors.tabs.photosTabTitle);
            await this.browser.waitForUrlContains({path: '/org/batoni/103818976702/gallery/'});
        });

        it('При открытии панорам должен установиться SEO URL', async function () {
            await openPage(this.browser, 103818976702);
            await this.browser.waitAndClick(cssSelectors.search.businessCard.panorama);
            await this.browser.waitForUrlContains({path: '/org/batoni/103818976702/panorama/'});
        });

        it('Таб "Похожие организации рядом" не отображается для геопродуктовой организации', async function () {
            await openPage(this.browser, 1322505104);
            await this.browser.waitForVisible(cssSelectors.tabs.titles);
            await this.browser.waitForHidden(cssSelectors.tabs.relatedTabTitle);
        });

        it('Таб "Похожие организации рядом" отображается для негеопродуктовой организации', async function () {
            await openPage(this.browser, 150967860736);
            await this.browser.waitForVisible(cssSelectors.tabs.relatedTabTitle);
        });
    });
});

async function openPage(browser: WebdriverIO.Browser, id: number, options: OpenPageOptions = {}): Promise<void> {
    await browser.openPage(`?ol=biz&oid=${id}`, options);
    await browser.waitAndClick(cssSelectors.collapsedCardTitle);
    await browser.waitForVisible(cssSelectors.search.businessCard.view);
}
