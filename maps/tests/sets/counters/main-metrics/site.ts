import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1131878730',
    org: '/org/1131878730/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1131878730'
};

counterGenerator({
    name: 'Ссылка на сайт.',
    isMainMetric: true,
    specs: [
        // Клик по сайту организации
        {
            name: new RegExp(ANALYTIC_NAMES.siteLink.regexp),
            description: '1орг. Ссылка на сайт организации.',
            url: URLS.org,
            selector: cssSelectors.search.businessCard.links.url,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.siteLink.regexp),
            description: 'ТАЧ. Организация. Ссылка на сайт организации.',
            isMobile: true,
            url: URLS.org,
            selector: cssSelectors.actionBar.webButton,
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
            name: new RegExp(ANALYTIC_NAMES.siteLink.regexp),
            description: 'Организация. Ссылка на сайт организации.',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.links.url,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.siteLink.regexp),
            description: 'POI. Ссылка на сайт организации.',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.links.url,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.siteLink.regexp),
            description: 'Превью организации. Ссылка на сайт организации".',
            url: '?ll=37.605724,55.762744&z=17',
            selector: cssSelectors.businessMapPreview.siteLink,
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
        // Клик по ссылке на соц.сети организации
        {
            name: new RegExp(ANALYTIC_NAMES.socialLink.regexp),
            description: '1орг. Ссылка на соцсеть организации.',
            url: URLS.org,
            selector: cssSelectors.orgpage.contacts.socialLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.socialLink.regexp),
            description: 'Организация. Ссылка на соцсеть организации.',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.socialLink,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.socialLink.regexp),
            description: 'POI. Ссылка на соцсеть организации.',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.socialLink,
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
