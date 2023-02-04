import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1413969678',
    org: '/org/makdonalds/1413969678/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1413969678'
};

counterGenerator({
    name: 'Входы.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.showEntrances.regexp),
            description: 'Организация.',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.address.entrances,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.showEntrances.regexp),
            description: '1орг.',
            url: URLS.org,
            selector: cssSelectors.search.businessCard.address.entrances,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.showEntrances.regexp),
            description: 'POI.',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.address.entrances,
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
