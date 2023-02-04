import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    org: '/org/krygina_studio/7892023858/',
    poi: '?poi[uri]=ymapsbm1://org?oid=7892023858'
};

counterGenerator({
    name: 'Мини-виджет бронирования.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.bookingWidget.regexp),
            description: 'POI. Услуга в виджете.',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.bookingMiniWidget.nthService.replace('%i', '1'),
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.search.businessCard.bookingMiniWidget.view, {
                            vertical: 'center'
                        });
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookingWidget.regexp),
            description: '1орг. Услуга в виджете.',
            url: URLS.org,
            selector: cssSelectors.search.businessCard.bookingMiniWidget.nthService.replace('%i', '1'),
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.search.businessCard.bookingMiniWidget.view, {
                            vertical: 'center'
                        });
                    },
                    type: 'click'
                }
            ]
        }
    ]
});
