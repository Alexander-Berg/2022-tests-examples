import cssSelectors from '../../common/css-selectors';
import counterGenerator from '../../lib/counter-generator';

const orgUrl = '?ol=biz&oid=1827539165';
const orgState = {uri: '*', permalink: '1827539165', type: '*', logId: '*', gml_id: '*', reqid: '*', query: '*'};

async function closeCard(browser: WebdriverIO.Browser): Promise<void> {
    await browser.waitAndClick(cssSelectors.closeButton);
}

counterGenerator({
    name: 'Карточка организации.',
    specs: [
        {
            name: 'maps_www.serp_panel.preview_card',
            description: 'Карточка организации',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.view,
            events: [
                {
                    type: 'show',
                    state: orgState
                },
                {
                    type: 'hide',
                    state: orgState,
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.actions.carousel.bookmark',
            description: 'Кнопка "Закладка"',
            url: orgUrl,
            selector: cssSelectors.actionBar.bookmarkButton,
            events: [
                {
                    type: 'show',
                    state: {uri: '*', state: 'unsaved'}
                },
                {
                    type: 'click',
                    state: {
                        uri: '*',
                        permalink: '1827539165',
                        gml_id: '1',
                        logId: '*',
                        reqid: '*',
                        query: '*',
                        state: 'unsaved'
                    }
                },
                {
                    type: 'hide',
                    state: {uri: '*', state: 'unsaved'},
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.category',
            description: 'Категория',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.category,
            events: [
                {
                    type: 'show',
                    state: {name: '*'}
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    state: {name: '*'},
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.photos',
            description: 'Превью фото (когда показывается одно фото)',
            url: '?ol=biz&oid=197313077081',
            selector: cssSelectors.search.businessCard.photos,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: {...orgState, permalink: '197313077081'}
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.photos_carousel.carousel.photos',
            description: 'Превью фото (когда показывается автопролистывающаяся галерея)',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.photos,
            events: [
                {
                    type: 'click',
                    state: orgState
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.add_photo',
            description: 'Кнопка "Добавить фото"',
            url: '?ol=biz&oid=6897184485',
            selector: cssSelectors.search.businessCard.addPhoto,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: {
                        uri: '*',
                        permalink: '6897184485',
                        type: '*',
                        logId: '*',
                        gml_id: '*',
                        reqid: '*',
                        query: '*'
                    }
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.working_status',
            description: 'Время работы организации',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.hours.text,
            events: [
                {
                    type: 'show',
                    options: {
                        matchesAmount: 2
                    }
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    setup: closeCard,
                    options: {
                        matchesAmount: 2
                    }
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.working_status.dialog.content',
            description: 'Выпадающий список времени работы',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.hours.content,
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.search.businessCard.hours.control);
            },
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'hide',
                    setup: async (browser) => {
                        await browser.waitForVisible(cssSelectors.search.businessCard.hours.content);
                        await browser.waitAndClick(cssSelectors.dialog.closeButton);
                        await browser.waitForHidden(cssSelectors.search.businessCard.hours.content);
                    }
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.actions.carousel.build_route',
            description: 'Кнопка маршрута',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.route,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.actions.carousel.share',
            description: 'Кнопка "Поделиться"',
            url: orgUrl,
            selector: cssSelectors.actionBar.shareButton,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.panorama',
            description: 'Превью панорамы',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.panorama,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.advert',
            description: 'Реклама',
            url: '?ol=biz&oid=77291562852',
            selector: cssSelectors.search.businessCard.advert,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: {
                        uri: '*',
                        permalink: '77291562852',
                        type: '*',
                        logId: '*',
                        gml_id: '*',
                        reqid: '*',
                        query: '*'
                    }
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.phones',
            description: 'Телефон организации',
            url: '?ol=biz&oid=1070256757',
            selector: cssSelectors.search.businessCard.phones.number,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.phones.control',
            description: 'Кнопка выпадающего списка телефонов',
            url: '?ol=biz&oid=1124715036',
            selector: cssSelectors.search.businessCard.phones.control,
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.search.businessCard.phones.show);
            },
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: {
                        uri: '*',
                        permalink: '1124715036',
                        type: '*',
                        logId: '*',
                        gml_id: '*',
                        reqid: '*',
                        query: '*'
                    }
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.phones.content',
            description: 'Выпадающий список телефонов',
            url: '?ol=biz&oid=1124715036',
            selector: cssSelectors.search.businessCard.phones.content,
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.search.businessCard.phones.show);
                await browser.waitAndClick(cssSelectors.search.businessCard.phones.control);
            },
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'hide',
                    setup: async (browser) => {
                        await browser.waitForVisible(cssSelectors.search.businessCard.phones.content);
                        await browser.waitAndClick(cssSelectors.search.businessCard.phones.control);
                        await browser.waitForHidden(cssSelectors.search.businessCard.phones.content);
                    }
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.site_links.site_link',
            description: 'Ссылка на сайт организации',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.links.url,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.site_links.control',
            description: 'Кнопка выпадающего списка сайтов',
            url: '?ol=biz&oid=1053324879',
            selector: cssSelectors.search.businessCard.links.control,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: {
                        uri: '*',
                        permalink: '124394384886',
                        type: '*',
                        logId: '*',
                        gml_id: '*',
                        reqid: '*',
                        query: '*'
                    }
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.site_links.content',
            description: 'Выпадающий список сайтов',
            url: '?ol=biz&oid=1053324879',
            selector: cssSelectors.search.businessCard.links.content,
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.search.businessCard.links.control);
                await browser.waitForVisible(cssSelectors.search.businessCard.links.content);
            },
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'hide',
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.search.businessCard.links.control);
                        await browser.waitForHidden(cssSelectors.search.businessCard.links.content);
                    }
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.site_links.content.site_link',
            description: 'Ссылка на сайт в выпадающем списке',
            url: '?ol=biz&oid=1053324879',
            selector: cssSelectors.search.businessCard.links.contentUrl,
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.search.businessCard.links.control);
                await browser.waitForVisible(cssSelectors.search.businessCard.links.content);
            },
            events: [
                {
                    type: 'show',
                    options: {multiple: true}
                },
                {
                    type: 'hide',
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.search.businessCard.links.control);
                        await browser.waitForHidden(cssSelectors.search.businessCard.links.content);
                    },
                    options: {multiple: true}
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.masstransit',
            description: 'Ближайшая станция метро',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.metro.text,
            events: [
                {
                    type: 'show',
                    options: {multiple: true}
                },
                {
                    type: 'hide',
                    setup: closeCard,
                    options: {multiple: true}
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.masstransit.control',
            description: 'Кнопка выпадающего списка метро',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.metro.control,
            events: [
                {
                    type: 'show',
                    options: {multiple: true}
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    setup: closeCard,
                    options: {multiple: true}
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.masstransit.content',
            description: 'Выпадающий список метро',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.metro.content,
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.search.businessCard.metro.control);
                await browser.waitForVisible(cssSelectors.search.businessCard.metro.content);
            },
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'hide',
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.search.businessCard.metro.control);
                        await browser.waitForHidden(cssSelectors.search.businessCard.metro.content);
                    }
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.masstransit.content.metro_route',
            description: 'Кнопка маршрута до станции метро',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.metro.contentDistance,
            setup: async (browser) => {
                await browser.waitAndClick(cssSelectors.search.businessCard.metro.control);
                await browser.waitForVisible(cssSelectors.search.businessCard.metro.content);
            },
            events: [
                {
                    type: 'show',
                    options: {multiple: true}
                },
                {
                    type: 'hide',
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.search.businessCard.metro.control);
                        await browser.waitForHidden(cssSelectors.search.businessCard.metro.content);
                    },
                    options: {multiple: true}
                },
                {
                    type: 'click'
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.edit',
            description: 'Кнопка "Исправить информацию об организации"',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.edit,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.chain',
            description: 'Ссылка на сетевую организацию',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.chain,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.more_info',
            description: 'Ссылка на страницу организации',
            url: orgUrl,
            selector: cssSelectors.search.businessCard.features.moreInfoButton,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'click',
                    state: orgState
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.reviews.rate_suggestion.rate_stars',
            description: 'Звезды для оценки организации',
            url: orgUrl,
            selector: cssSelectors.ugc.ratingEdit.view,
            events: [
                {
                    type: 'show'
                },
                {
                    type: 'hide',
                    setup: closeCard
                }
            ]
        },
        {
            name: 'maps_www.serp_panel.preview_card.reviews.rate_suggestion.rate_stars.star',
            description: 'Звезда оценки организации',
            url: orgUrl,
            selector: cssSelectors.ugc.ratingEdit.nthStar.replace('%i', '3'),
            events: [
                {
                    type: 'click',
                    state: orgState
                }
            ]
        }
    ]
});
