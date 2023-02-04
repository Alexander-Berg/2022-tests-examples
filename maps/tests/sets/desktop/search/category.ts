import cssSelectors from '../../../common/css-selectors';
import {ExpectedQuery} from '../../../commands/wait-for-url-contains';
import getSelectorByText from '../../../lib/func/get-selector-by-text';

const filterButton = cssSelectors.search.filters.mapFilters.filterButton;
const filterOption = cssSelectors.search.filters.mapFilters.enumOption;

describe('Страницы категорий', () => {
    it('Блок c h1 и текстом для результатов', async function () {
        const url = '/45/cheboksary/category/aquapark/184106350/';
        await this.browser.openPage(url);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.search.list.meta.view, 'search-list-meta-view');
    });

    landingCheck({
        name: 'категория',
        url: '/213/moscow/category/cafe/184106390/',
        changePathOptions: [
            {
                name: 'по переходу в сниппет',
                preparation: async (browser) => {
                    await browser.waitAndClick(cssSelectors.search.list.snippet.viewFirst);
                },
                path: '/org/kafe_festivalnoye/1209774228/',
                query: {
                    'display-text': 'Кафе',
                    mode: 'search',
                    text: 'category_id:(184106390)',
                    z: 13
                }
            }
        ]
    });

    // st/MAPSUI-17082
    // landingCheck({
    //     name: 'категория с районом',
    //     url: '/213/moscow/geo/rayon_khamovniki/53211698/category/cafe/184106390/',
    //     changePathOptions: [{
    //         name: 'по переходу в сниппет',
    //         preparation: async (browser) => {
    //             await browser.waitAndClick(cssSelectors.search.list.snippet.viewFirst);
    //         },
    //         query: {
    //             'display-text': 'Кафе',
    //             mode: 'search',
    //             oid: '225038468495',
    //             ol: 'biz',
    //             text: '{"text":"cafe","what":[{"attr_name":"rubric","attr_values":["cafe"]}],"where":"sg:53211698"}'
    //         }
    //     }],
    //     basePath: '/213/moscow/geo/rayon_khamovniki/53211698/cafe/184106390/'
    // });

    landingCheck({
        name: 'бинарный фильтр',
        url: '/213/moscow/category/cafe/184106390/filter/breakfast/',
        changePathOptions: [
            {
                name: 'по добавлению опции enum',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Кухня', filterButton));
                    await browser.waitAndClick(getSelectorByText('Европейская', filterOption));
                },
                query: {
                    filter: 'type_cuisine:european_cuisine;breakfast:1;alternate_vertical:RequestWindow'
                }
            },
            {
                name: 'по удалению булевого фильтра',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Завтрак', filterButton));
                },
                query: {
                    filter: 'alternate_vertical:RequestWindow'
                }
            }
        ],
        basePath: '/213/moscow/category/cafe/184106390/'
    });
    landingCheck({
        name: 'enum фильтр',
        url: '/213/moscow/category/cafe/184106390/filter/type_cuisine/european_cuisine/',
        changePathOptions: [
            {
                name: 'по добавлению опции enum в этом же фильтре',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Кухня', filterButton));
                    await browser.waitAndClick(getSelectorByText('Итальянская', filterOption));
                },
                query: {
                    filter: 'type_cuisine:european_cuisine,italian_cuisine;alternate_vertical:RequestWindow'
                }
            },
            {
                name: 'по добавлению опции enum в другом фильтре',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Цены', filterButton));
                    await browser.waitAndClick(getSelectorByText('Средние', filterOption));
                },
                query: {
                    filter:
                        'type_cuisine:european_cuisine;price_category:price_reasonable;alternate_vertical:RequestWindow'
                }
            },
            {
                name: 'по удалению выделенной опции enum',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Кухня', filterButton));
                    await browser.waitAndClick(getSelectorByText('Европейская', filterOption));
                },
                query: {
                    filter: 'alternate_vertical:RequestWindow'
                }
            },
            {
                name: 'по добавлению булевого фильтра',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Завтрак', filterButton));
                },
                query: {
                    filter: 'type_cuisine:european_cuisine;breakfast:1;alternate_vertical:RequestWindow'
                }
            }
        ],
        basePath: '/213/moscow/category/cafe/184106390/'
    });

    it('Путь остается с 1 результатом', async function () {
        const url = '/11433/verhoyansk/category/museum/184105894/';
        await this.browser.openPage(url);
        await this.browser.waitForUrlContains({path: url});
        await this.browser.waitForElementsCount(cssSelectors.search.list.snippet.view, 1);
    });
});

describe('Поисковый лендинг', () => {
    landingCheck({
        name: 'поиск',
        url: '/213/moscow/search/%D0%BA%D0%B0%D1%84%D0%B5/',
        changePathOptions: [
            {
                name: 'по переходу в сниппет',
                preparation: async (browser) => {
                    await browser.waitAndClick(cssSelectors.search.list.snippet.viewFirst);
                },
                path: '/org/dzhazmen/119913270206/',
                query: {
                    mode: 'search',
                    text: 'кафе'
                }
            }
        ]
    });
    landingCheck({
        name: 'бинарный фильтр',
        url: '/213/moscow/search/%D0%BA%D0%B0%D1%84%D0%B5/filter/business_lunch/',
        changePathOptions: [
            {
                name: 'по добавлению опции enum',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Цены', filterButton));
                    await browser.waitAndClick(getSelectorByText('Средние', filterOption));
                },
                query: {
                    filter: 'price_category:price_reasonable;business_lunch:1;alternate_vertical:RequestWindow'
                }
            },
            {
                name: 'по удалению булевого фильтра',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Бизнес-ланч', filterButton));
                }
            }
        ],
        basePath: '/213/moscow/search/%D0%BA%D0%B0%D1%84%D0%B5/'
    });

    landingCheck({
        name: 'enum фильтр',
        url: '/213/moscow/search/%D0%BA%D0%B0%D1%84%D0%B5/filter/type_cuisine/european_cuisine/',
        changePathOptions: [
            {
                name: 'по добавлению опции enum в этом же фильтре',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Кухня', filterButton));
                    await browser.waitAndClick(getSelectorByText('Итальянская', filterOption));
                },
                query: {
                    filter: 'type_cuisine:european_cuisine,italian_cuisine;alternate_vertical:RequestWindow'
                }
            },
            {
                name: 'по добавлению опции enum в другом фильтре',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Цены', filterButton));
                    await browser.waitAndClick(getSelectorByText('Средние', filterOption));
                },
                query: {
                    filter:
                        'price_category:price_reasonable;type_cuisine:european_cuisine;alternate_vertical:RequestWindow'
                }
            },
            {
                name: 'по удалению выделенной опции enum',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Кухня', filterButton));
                    await browser.waitAndClick(getSelectorByText('Европейская', filterOption));
                }
            },
            {
                name: 'по добавлению булевого фильтра',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Завтрак', filterButton));
                },
                query: {
                    filter: 'breakfast:1;type_cuisine:european_cuisine;alternate_vertical:RequestWindow'
                }
            }
        ],
        basePath: '/213/moscow/search/%D0%BA%D0%B0%D1%84%D0%B5/'
    });
});

describe('Лендинг сетевой организации', () => {
    landingCheck({
        name: 'поиск сетевой организации',
        url: '/213/moscow/chain/tanuki/6003051/',
        changePathOptions: [
            {
                name: 'по переходу в сниппет',
                preparation: async (browser) => {
                    await browser.waitAndClick(cssSelectors.search.list.snippet.viewFirst);
                },
                path: '/org/tanuki/1938915047/',
                query: {
                    mode: 'search',
                    'display-text': 'Тануки',
                    text: 'chain_id:(6003051)'
                }
            }
        ]
    });
    landingCheck({
        name: 'бинарный фильтр',
        url: '/213/moscow/search/%D0%93%D0%B4%D0%B5%20%D0%BF%D0%BE%D0%B5%D1%81%D1%82%D1%8C/filter/business_lunch/',
        changePathOptions: [
            {
                name: 'по удалению булевого фильтра',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Бизнес-ланч', filterButton));
                }
            }
        ],
        basePath: '/213/moscow/search/%D0%93%D0%B4%D0%B5%20%D0%BF%D0%BE%D0%B5%D1%81%D1%82%D1%8C/filter/'
    });

    landingCheck({
        name: 'enum фильтр',
        url:
            '/213/moscow/search/%D0%93%D0%B4%D0%B5%20%D0%BF%D0%BE%D0%B5%D1%81%D1%82%D1%8C/filter/category_id/184106390/',
        changePathOptions: [
            {
                name: 'по добавлению булевого фильтра',
                preparation: async (browser) => {
                    await browser.waitAndClick(getSelectorByText('Бизнес-ланч', filterButton));
                },
                query: {
                    filter: 'business_lunch:1;category_id:184106390;alternate_vertical:RequestWindow'
                }
            }
        ],
        basePath: '/213/moscow/search/%D0%93%D0%B4%D0%B5%20%D0%BF%D0%BE%D0%B5%D1%81%D1%82%D1%8C/'
    });
});

interface ChangePathOption {
    name: string;
    preparation: (browser: WebdriverIO.Browser) => Promise<void>;
    path?: string;
    query?: ExpectedQuery;
}

interface LandingOptions {
    name: string;
    url: string;
    changePathOptions: ChangePathOption[];
    basePath?: string;
}

function landingCheck({name, url, changePathOptions, basePath}: LandingOptions): void {
    describe(`Лендинг ${name}.`, () => {
        it('Путь остается', async function () {
            await this.browser.setViewportSize({width: 1440, height: 900});
            await this.browser.openPage(url);
            await this.browser.waitForVisible(cssSelectors.search.list.view);
            await this.browser.waitForUrlContains({
                path: url
            });
        });

        changePathOptions.forEach((option) => {
            it(`Путь исчезает ${option.name}`, async function () {
                await this.browser.setViewportSize({width: 1440, height: 900});
                await this.browser.openPage(url);
                await this.browser.waitForVisible(cssSelectors.search.list.view);
                await this.browser.addStyles('.button__link {pointer-events: initial !important}');
                await this.browser.addStyles('.menu-item_air__link {pointer-events: initial !important}');
                await option.preparation(this.browser);
                await this.browser.waitForUrlContains(
                    {
                        path: option.path || basePath || '/213/moscow/',
                        query: option.query
                    },
                    {
                        partial: true
                    }
                );
            });
        });
    });
}
