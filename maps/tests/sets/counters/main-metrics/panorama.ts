import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1131878730',
    org: '/org/1131878730/'
};

counterGenerator({
    name: 'Панорама.',
    isMainMetric: true,
    specs: [
        // Клик в панораму
        {
            name: new RegExp(ANALYTIC_NAMES.panorama.regexp),
            description: 'Организация',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.panorama,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.panorama.regexp),
            description: '1орг',
            url: URLS.org,
            selector: cssSelectors.orgpage.panorama,
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
