import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

counterGenerator({
    name: 'Табы в карточке.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.openPhotoTab.regexp),
            description: 'Фото.',
            url: '?ol=biz&oid=1131878730',
            selector: cssSelectors.tabs.photosTabTitle,
            events: [
                {
                    type: 'click',
                    state: ANALYTIC_NAMES.openPhotoTab.params
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.openUgcTab.regexp),
            description: 'Отзывы.',
            url: '?ol=biz&oid=1131878730',
            selector: cssSelectors.tabs.reviewsTabTitle,
            events: [
                {
                    type: 'click',
                    state: ANALYTIC_NAMES.openUgcTab.params
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.postTab.regexp),
            description: 'Новости.',
            url: '/org/makdonalds/1015955793',
            selector: cssSelectors.tabs.postsTabTitle,
            events: [
                {
                    type: 'click',
                    state: ANALYTIC_NAMES.postTab.params
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.pricesTab.regexp),
            description: 'Товары и услуги.',
            url: '?ol=biz&oid=1293315017',
            selector: cssSelectors.tabs.pricesTabTitle,
            events: [
                {
                    type: 'click',
                    state: ANALYTIC_NAMES.pricesTab.params
                }
            ]
        }
    ]
});
