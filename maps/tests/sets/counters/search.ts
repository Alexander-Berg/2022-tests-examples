import cssSelectors from '../../common/css-selectors';
import counterGenerator from '../../lib/counter-generator';

async function close(browser: WebdriverIO.Browser): Promise<void> {
    await browser.waitAndClick(cssSelectors.closeButton);
}

counterGenerator({
    name: 'Поиск.',
    specs: [
        {
            name: 'maps_www.search_form',
            description: 'Поисковая форма',
            events: [
                {
                    type: 'enter',
                    state: {initial_query: 'паб рядом'},
                    setup: async (browser) => {
                        await browser.submitSearch('паб рядом');
                    }
                }
            ]
        },
        {
            name: 'maps_www.search_form',
            description: 'Кнопка "найти"',
            events: [
                {
                    type: 'enter',
                    state: {initial_query: 'кофейня сейчас'},
                    setup: async (browser) => {
                        await browser.setValueToInput(cssSelectors.search.input, 'кофейня сейчас');
                        await browser.waitAndClick(cssSelectors.search.searchButton);
                    }
                }
            ]
        },
        {
            name: 'maps_www.map.search_results',
            description: 'Поисковые результаты',
            url: '?text=яндекс',
            selector: cssSelectors.search.placemark.view,
            events: [
                {
                    type: 'show',
                    state: {reqid: '*', query: 'яндекс'}
                },
                {
                    type: 'hide',
                    state: {reqid: '*', query: 'яндекс'},
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.closeButton);
                    }
                }
            ]
        },
        {
            name: 'maps_www.map.search_results',
            description: 'Новые поисковые результаты',
            url: '?text=яндекс',
            selector: cssSelectors.search.placemark.view,
            preparation: async (browser) => {
                await browser.waitForVisible(cssSelectors.search.list.view);
            },
            setup: async (browser) => {
                await browser.submitSearch('Москва');
                await browser.waitForVisible(cssSelectors.search.toponymCard.title);
            },
            events: [
                {
                    type: 'show',
                    state: {reqid: '*', query: 'Москва'}
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.results',
            description: 'СЕРП',
            url: '?text=кафе',
            selector: cssSelectors.search.list.view,
            events: [
                {
                    type: 'show',
                    state: {reqid: '*', query: 'кафе'}
                },
                {
                    type: 'hide',
                    state: {reqid: '*', query: 'кафе', maxIndexOfVisibleSnippet: '*'},
                    setup: close
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.results',
            description: 'Обновление СЕРПа',
            url: '?text=кафе',
            selector: cssSelectors.search.list.view,
            preparation: async (browser) => {
                await browser.waitForVisible(cssSelectors.search.list.view);
            },
            events: [
                {
                    type: 'change_state',
                    setup: async (browser) => {
                        await browser.submitSearch('торт');
                        await browser.waitForVisible(cssSelectors.search.list.view);
                        await browser.waitForHidden(cssSelectors.search.loadingIndicator);
                    },
                    state: {reqid: '*', query: 'торт'},
                    options: {multiple: true}
                }
            ]
        },
        {
            name: 'maps_www.map.search_results.placemark',
            description: 'Метки результатов на карте',
            url: '?text=кафе',
            selector: cssSelectors.search.placemark.view,
            events: [
                {
                    type: 'show',
                    state: {type: '*', uri: '*', reqid: '*'},
                    options: {multiple: true}
                },
                {
                    type: 'hide',
                    state: {type: '*', uri: '*', reqid: '*'},
                    setup: close,
                    options: {multiple: true}
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.results.result_item',
            description: 'Сниппет СЕРПа',
            url: '?text=кафе',
            selector: cssSelectors.search.list.snippet.business,
            events: [
                {
                    type: 'show',
                    state: {type: '*', position: '*', uri: '*', reqid: '*', logId: '*'},
                    options: {multiple: true}
                },
                {
                    type: 'click',
                    state: {reqid: '*', type: '*', gml_id: '*', logId: '*', position: '*', permalink: '*', uri: '*'}
                },
                {
                    type: 'hide',
                    state: {type: '*', position: '*', uri: '*', reqid: '*'},
                    setup: close,
                    options: {multiple: true}
                }
            ]
        }
    ]
});
