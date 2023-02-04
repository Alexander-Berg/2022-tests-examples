import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1131878730',
    org: '/org/1131878730/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1131878730'
};

counterGenerator({
    name: 'Просмотр времени работы.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.workingStatus.regexp),
            description: 'Организация. Шапка карточки',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.hours.text,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.workingStatus.regexp),
            description: 'Организация. Тело карточки',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.workingStatus.view,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.workingStatus.regexp),
            description: '1орг. Шапка карточки',
            url: URLS.org,
            selector: cssSelectors.orgpage.header.workingStatus,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.workingStatus.regexp),
            description: '1орг. Тело карточки',
            url: URLS.org,
            selector: cssSelectors.search.businessCard.workingStatus.view,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.workingStatus.regexp),
            description: 'POI. Шапка карточки',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.hours.text,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.workingStatus.regexp),
            description: 'POI. Тело карточки',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.workingStatus.view,
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
