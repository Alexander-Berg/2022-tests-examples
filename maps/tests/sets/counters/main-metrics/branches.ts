import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1413969678',
    org: '/org/makdonalds/1413969678/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1413969678'
};

counterGenerator({
    name: 'Все филиалы сети.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.chain.regexp),
            description: 'Организация.',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.chain,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.chain.regexp),
            description: 'POI.',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.chain,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.chain.regexp),
            description: '1орг.',
            url: URLS.org,
            selector: cssSelectors.search.businessCard.chain,
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
