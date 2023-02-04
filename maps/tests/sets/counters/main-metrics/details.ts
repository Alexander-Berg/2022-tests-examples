import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1413969678',
    poi: '?poi[uri]=ymapsbm1://org?oid=1413969678'
};

counterGenerator({
    name: 'Подробнее об организации.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.details.regexp),
            description: 'Организация.',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.features.moreInfoButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.details.regexp),
            description: 'POI.',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.features.moreInfoButton,
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
