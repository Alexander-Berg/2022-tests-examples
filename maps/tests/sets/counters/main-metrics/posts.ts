import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=119570452076',
    org: '/org/otutto/119570452076/',
    poi: '?poi[uri]=ymapsbm1://org?oid=119570452076'
};

counterGenerator({
    name: 'Посты организаций.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.post.regexp),
            description: 'Организация.',
            url: URLS.business,
            openPageOptions: {mockToday: '2021-02-02'},
            selector: cssSelectors.posts.card.posts.item,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.post.regexp),
            description: 'POI.',
            url: URLS.poi,
            openPageOptions: {mockToday: '2021-02-02'},
            selector: cssSelectors.posts.card.posts.item,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.post.regexp),
            description: '1орг.',
            url: URLS.org,
            openPageOptions: {mockToday: '2021-02-02'},
            selector: cssSelectors.posts.card.posts.item,
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
