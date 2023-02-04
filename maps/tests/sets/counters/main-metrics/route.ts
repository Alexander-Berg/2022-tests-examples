import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1131878730',
    org: '/org/1131878730/'
};

counterGenerator({
    name: 'Маршрут.',
    isMainMetric: true,
    specs: [
        // Клик в кнопку "Маршрут"
        {
            name: new RegExp(ANALYTIC_NAMES.route.regexp),
            description: 'Организация. Кнопка "маршрут".',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.route,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.route.regexp),
            description: '1орг. Кнопка "маршрут"',
            url: URLS.org,
            selector: cssSelectors.actionBar.routeButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.route.regexp),
            description: 'ТАЧ. Организация. Кнопку "маршрут".',
            isMobile: true,
            url: URLS.org,
            selector: cssSelectors.actionBar.routeButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.swipeShutter('down');
                        await browser.waitForHidden(cssSelectors.sidebar.panel);
                        await browser.waitForVisible(cssSelectors.sidebar.minicard);
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.route.regexp),
            description: 'Превью организации. Кнопка "маршрут".',
            url: '?ll=37.605724,55.762744&z=17',
            selector: cssSelectors.businessMapPreview.route,
            events: [
                {
                    setup: async (browser) => {
                        await browser.simulateGeoHover({
                            point: [37.60515, 55.762553],
                            description: 'Навести курсор на Министерство культуры'
                        });
                    },
                    type: 'click'
                }
            ]
        },
        // Маршрут к организации от ближайшей станции метро
        {
            name: new RegExp(ANALYTIC_NAMES.metroRoute.regexp),
            description: 'Организация. Маршрут к организации от метро.',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.metro.distance,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.metroRoute.regexp),
            description: '1орг. Маршрут к организации от метро.',
            url: URLS.org,
            selector: cssSelectors.orgpage.contacts.masstransit.metro.distance,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        // Маршрут к организации от ближайшей остановки ОТ
        {
            name: new RegExp(ANALYTIC_NAMES.masstransitRoute.regexp),
            description: 'Организация. Маршрут к организации от остановки.',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.masstransit.distance,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.masstransitRoute.regexp),
            description: '1орг. Маршрут к организации от остановки.',
            url: URLS.org,
            selector: cssSelectors.orgpage.contacts.masstransit.aboveground.distance,
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
