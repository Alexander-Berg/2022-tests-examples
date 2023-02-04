import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1131878730',
    org: '/org/1131878730/',
    callableOrg: '/org/1124715036/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1131878730'
};

counterGenerator({
    name: 'Телефон.',
    isMainMetric: true,
    specs: [
        // Клик в кнопку "показать телефон"
        {
            name: new RegExp(ANALYTIC_NAMES.showPhone.regexp),
            description: '1орг. Кнопка "Показать телефон".',
            url: URLS.org,
            selector: cssSelectors.orgpage.header.phones.moreButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.showPhone.regexp),
            description: 'Организация. Кнопка "Показать телефон".',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.phones.number,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.showPhone.regexp),
            description: 'POI. Кнопка "Показать телефон".',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.phones.show,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.showPhone.regexp),
            description: 'Превью организации. Кнопка "Показать телефон".',
            url: '?ll=37.605724,55.762744&z=17',
            selector: cssSelectors.businessMapPreview.showPhone,
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
        // Клик по телефону-ссылке
        {
            name: new RegExp(ANALYTIC_NAMES.callPhone.regexp),
            description: '1орг. Кнопка с телефоном.',
            url: URLS.callableOrg,
            selector: cssSelectors.orgpage.header.phone.call,
            events: [
                {
                    type: 'click',
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.orgpage.header.phones.moreButton);
                        await browser.waitAndClick(cssSelectors.orgpage.header.phones.view);
                        await browser.waitForVisible(cssSelectors.orgpage.header.phones.popup);
                        await browser.waitAndHover(cssSelectors.orgpage.header.phone.view);
                    }
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.callPhone.regexp),
            description: 'ТАЧ. 1орг. Кнопка с телефоном.',
            isMobile: true,
            url: URLS.org,
            selector: cssSelectors.search.businessCard.phones.call,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.callPhone.regexp),
            description: 'ТАЧ. POI. Кнопка с телефоном.',
            isMobile: true,
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.phones.call,
            events: [
                {
                    type: 'click',
                    setup: async (browser) => {
                        await browser.swipeShutter('up');
                        await browser.waitForHidden(cssSelectors.sidebar.minicard);
                        await browser.waitForVisible(cssSelectors.sidebar.panel);
                        await browser.scrollIntoView(cssSelectors.search.businessCard.phones.call, {
                            vertical: 'end'
                        });
                    }
                }
            ]
        }
    ]
});
