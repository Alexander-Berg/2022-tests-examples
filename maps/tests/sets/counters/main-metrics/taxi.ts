import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1131878730',
    org: '/org/1131878730/'
};

counterGenerator({
    name: 'Вызов такси.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.getTaxi.regexp),
            description: 'Организация',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.transit.taxi,
            setup: async (browser) => {
                await browser.scrollIntoView(cssSelectors.search.businessCard.transit.view, {vertical: 'center'});
                await browser.waitForVisible(cssSelectors.search.businessCard.transit.taxi);
            },
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.getTaxi.regexp),
            description: '1орг',
            url: URLS.org,
            selector: cssSelectors.search.businessCard.transit.taxi,
            setup: async (browser) => {
                await browser.scrollIntoView(cssSelectors.search.businessCard.transit.view, {vertical: 'center'});
                await browser.waitForVisible(cssSelectors.search.businessCard.transit.taxi);
            },
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
