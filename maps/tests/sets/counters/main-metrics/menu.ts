import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import getSelectorByText from '../../../lib/func/get-selector-by-text';
import {ANALYTIC_NAMES} from './analytic-names';

const MENU_URLS = {
    business: '?ol=biz&oid=1413969678',
    poi: '?poi[uri]=ymapsbm1://org?oid=1413969678',
    org: '/org/makdonalds/1413969678/',
    orgWithFullMenu: '/org/makdonalds/1413969678/menu/'
};

const SHOWCASE_URLS = {
    business: '?ol=biz&oid=1106897219',
    org: '/org/1106897219',
    poi: '?poi[uri]=ymapsbm1://org?oid=1106897219'
};

counterGenerator({
    name: 'Просматривал продукты.',
    isMainMetric: true,
    specs: [
        // Узкая поисковая карточка
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'Организация. Меню товаров и услуг. Ссылка "Посмотреть всё меню"',
            url: MENU_URLS.business,
            selector: cssSelectors.search.businessCard.menuLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'Организация. Меню товаров и услуг. Кнопка next',
            url: MENU_URLS.business,
            selector: cssSelectors.cardRelatedProducts.nextButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.cardRelatedProducts.view);
                        await browser.waitForVisible(cssSelectors.cardRelatedProducts.nextButton);
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'Организация. Меню товаров и услуг. Кнопка prev',
            url: MENU_URLS.business,
            selector: cssSelectors.cardRelatedProducts.prevButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.cardRelatedProducts.view);
                        await browser.waitAndClick(cssSelectors.cardRelatedProducts.nextButton);
                        await browser.waitAndHover(cssSelectors.cardRelatedProducts.view);
                        await browser.waitForVisible(cssSelectors.cardRelatedProducts.prevButton);
                    },
                    options: {
                        matchesAmount: 2
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'Организация. Витрина. Ссылка "Посмотреть всё меню"',
            url: SHOWCASE_URLS.business,
            selector: cssSelectors.search.businessCard.menuLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'Организация. Витрина. Кнопка next',
            url: SHOWCASE_URLS.business,
            selector: cssSelectors.search.businessCard.specialOffers.nextButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.search.businessCard.specialOffers.view);
                        await browser.waitForVisible(cssSelectors.search.businessCard.specialOffers.nextButton);
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'Организация. Витрина. Кнопка prev',
            url: SHOWCASE_URLS.business,
            selector: cssSelectors.search.businessCard.specialOffers.prevButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.search.businessCard.specialOffers.view);
                        await browser.waitAndClick(cssSelectors.search.businessCard.specialOffers.nextButton);
                        await browser.waitAndHover(cssSelectors.search.businessCard.specialOffers.view);
                        await browser.waitForVisible(cssSelectors.search.businessCard.specialOffers.prevButton);
                    },
                    options: {matchesAmount: 2},
                    type: 'click'
                }
            ]
        },

        // Узкая ПОИ-карточка
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'POI. Меню товаров и услуг. Ссылка "Посмотреть всё меню"',
            url: MENU_URLS.poi,
            selector: cssSelectors.search.businessCard.menuLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'POI. Меню товаров и услуг. Кнопка next',
            url: MENU_URLS.poi,
            selector: cssSelectors.cardRelatedProducts.nextButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.cardRelatedProducts.view);
                        await browser.waitForVisible(cssSelectors.cardRelatedProducts.nextButton);
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'POI. Меню товаров и услуг. Кнопка prev',
            url: MENU_URLS.poi,
            selector: cssSelectors.cardRelatedProducts.prevButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.cardRelatedProducts.view);
                        await browser.waitAndClick(cssSelectors.cardRelatedProducts.nextButton);
                        await browser.waitAndHover(cssSelectors.cardRelatedProducts.view);
                        await browser.waitForVisible(cssSelectors.cardRelatedProducts.prevButton);
                    },
                    options: {matchesAmount: 2},
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'POI. Витрина. Ссылка "Посмотреть всё меню"',
            url: SHOWCASE_URLS.poi,
            selector: cssSelectors.search.businessCard.menuLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'POI. Витрина. Кнопка next',
            url: SHOWCASE_URLS.poi,
            selector: cssSelectors.search.businessCard.specialOffers.nextButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.search.businessCard.specialOffers.view);
                        await browser.waitForVisible(cssSelectors.search.businessCard.specialOffers.nextButton);
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: 'POI. Витрина. Кнопка prev',
            url: SHOWCASE_URLS.poi,
            selector: cssSelectors.search.businessCard.specialOffers.prevButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.search.businessCard.specialOffers.view);
                        await browser.waitAndClick(cssSelectors.search.businessCard.specialOffers.nextButton);
                        await browser.waitAndHover(cssSelectors.search.businessCard.specialOffers.view);
                        await browser.waitForVisible(cssSelectors.search.businessCard.specialOffers.prevButton);
                    },
                    options: {matchesAmount: 2},
                    type: 'click'
                }
            ]
        },

        // Широкая карточка
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: '1орг. Меню товаров и услуг. Ссылка "Посмотреть всё меню"',
            url: MENU_URLS.org,
            selector: cssSelectors.search.businessCard.menuLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: '1орг. Меню товаров и услуг. Кнопка next',
            url: MENU_URLS.org,
            selector: cssSelectors.cardRelatedProducts.nextButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.cardRelatedProducts.view, {vertical: 'center'});
                        await browser.waitAndHover(cssSelectors.cardRelatedProducts.view);
                        await browser.waitForVisible(cssSelectors.cardRelatedProducts.nextButton);
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: '1орг. Меню товаров и услуг. Кнопка prev',
            url: MENU_URLS.org,
            selector: cssSelectors.cardRelatedProducts.prevButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.cardRelatedProducts.view, {vertical: 'center'});
                        await browser.waitAndHover(cssSelectors.cardRelatedProducts.view);
                        await browser.waitAndClick(cssSelectors.cardRelatedProducts.nextButton);
                        await browser.waitAndHover(cssSelectors.cardRelatedProducts.view);
                        await browser.waitForVisible(cssSelectors.cardRelatedProducts.prevButton);
                    },
                    options: {matchesAmount: 2},
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: '1орг. Витрина. Ссылка "Посмотреть всё меню"',
            url: SHOWCASE_URLS.org,
            selector: cssSelectors.search.businessCard.menuLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: '1орг. Витрина. Кнопка next',
            url: SHOWCASE_URLS.org,
            selector: cssSelectors.search.businessCard.specialOffers.nextButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.search.businessCard.specialOffers.view);
                        await browser.waitForVisible(cssSelectors.search.businessCard.specialOffers.nextButton);
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuLook.regexp),
            description: '1орг. Витрина. Кнопка prev',
            url: SHOWCASE_URLS.org,
            selector: cssSelectors.search.businessCard.specialOffers.prevButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndHover(cssSelectors.search.businessCard.specialOffers.view);
                        await browser.waitAndClick(cssSelectors.search.businessCard.specialOffers.nextButton);
                        await browser.waitAndHover(cssSelectors.search.businessCard.specialOffers.view);
                        await browser.waitForVisible(cssSelectors.search.businessCard.specialOffers.prevButton);
                    },
                    options: {matchesAmount: 2},
                    type: 'click'
                }
            ]
        }
    ]
});

counterGenerator({
    name: 'Продукт.',
    isMainMetric: true,
    specs: [
        // Узкая поисковая карточка
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: 'Организация. Меню товаров и услуг. Продукт',
            url: MENU_URLS.business,
            selector: cssSelectors.cardRelatedProducts.relatedProduct,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: 'Организация. Витрина. Ссылка на продукт',
            url: SHOWCASE_URLS.business,
            selector: cssSelectors.search.businessCard.specialOffers.itemLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: 'Организация. Витрина. Изображение продукта',
            url: SHOWCASE_URLS.business,
            selector: cssSelectors.search.businessCard.specialOffers.itemImg,
            events: [
                {
                    type: 'click'
                }
            ]
        },

        // Узкая ПОИ-карточка
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: 'POI. Меню товаров и услуг. Продукт',
            url: MENU_URLS.poi,
            selector: cssSelectors.cardRelatedProducts.relatedProduct,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: 'POI. Витрина. Ссылка на продукт',
            url: SHOWCASE_URLS.poi,
            selector: cssSelectors.search.businessCard.specialOffers.itemLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: 'POI. Витрина. Изображение продукта',
            url: SHOWCASE_URLS.poi,
            selector: cssSelectors.search.businessCard.specialOffers.itemImg,
            events: [
                {
                    type: 'click'
                }
            ]
        },

        // Широкая карточка
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: '1орг. Меню товаров и услуг. Продукт',
            url: MENU_URLS.org,
            selector: cssSelectors.cardRelatedProducts.relatedProduct,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: '1орг. Витрина. Ссылка на продукт',
            url: SHOWCASE_URLS.org,
            selector: cssSelectors.search.businessCard.specialOffers.itemLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: '1орг. Витрина. Изображение продукта',
            url: SHOWCASE_URLS.org,
            selector: cssSelectors.search.businessCard.specialOffers.itemImg,
            events: [
                {
                    type: 'click'
                }
            ]
        },

        // Широкая карточка с открытым табом "Меню"
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: '1орг. Таб "Меню". Меню товаров и услуг. Продукт',
            url: MENU_URLS.orgWithFullMenu,
            selector: getSelectorByText('Большой Снэк Бокс с крыльями', cssSelectors.search.businessCard.menu.tab),
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: '1орг. Таб "Меню". Меню товаров и услуг. Категория продукта',
            url: MENU_URLS.orgWithFullMenu,
            selector: getSelectorByText('Десерты и выпечка', cssSelectors.search.businessCard.menu.tab),
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuItem.regexp),
            description: '1орг. Таб "Меню". Меню товаров и услуг. Закрыть категорию продукта',
            url: MENU_URLS.orgWithFullMenu,
            selector: cssSelectors.orgpage.menu.categories.selected,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndClick(
                            getSelectorByText('Десерты и выпечка', cssSelectors.search.businessCard.menu.tab)
                        );
                        await browser.waitForVisible(cssSelectors.orgpage.menu.categories.selected);
                    },
                    options: {matchesAmount: 2},
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.menuButton.regexp),
            description: 'Превью организации. Кнопка "Меню"',
            url: '?ll=37.605903,55.762307&z=19',
            selector: cssSelectors.businessMapPreview.menu,
            events: [
                {
                    setup: async (browser) => {
                        await browser.simulateGeoHover({
                            point: [37.605903, 55.762307],
                            description: 'Навести курсор на бар "Ровесник"'
                        });
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.pricesButton.regexp),
            description: 'Превью организации. Кнопка "Цены"',
            url: '?ll=37.647130,55.758673&z=21',
            selector: cssSelectors.businessMapPreview.prices,
            events: [
                {
                    setup: async (browser) => {
                        await browser.simulateGeoHover({
                            point: [37.647062, 55.758697],
                            description: 'Навести курсор на "Покровка Роял СПА"'
                        });
                    },
                    type: 'click'
                }
            ]
        }
    ]
});
