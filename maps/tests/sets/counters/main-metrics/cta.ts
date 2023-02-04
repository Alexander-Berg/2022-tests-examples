import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';
import getSelectorByText from '../../../lib/func/get-selector-by-text';

counterGenerator({
    name: 'Нерекламные call to action.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.cta.regexp),
            description: 'Организация. Таб «Меню». Заказать доставку.',
            url: '?ol=biz&oid=1219161144&tab=menu',
            selector: getSelectorByText('Заказать доставку', cssSelectors.search.businessCard.fullItems.controls),
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.cta.regexp),
            description: 'Организация. Сниппет. Забронировать столик.',
            url: '?ol=biz&oid=1219161144',
            selector: getSelectorByText('Забронировать столик', cssSelectors.search.businessCard.cardFeatureView),
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.cta.regexp),
            description: 'POI. Таб «Меню». Заказать доставку.',
            url: '?poi[uri]=ymapsbm1://org?oid=1219161144&tab=menu',
            selector: getSelectorByText('Заказать доставку', cssSelectors.search.businessCard.fullItems.controls),
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.cta.regexp),
            description: 'POI. Сниппет. Забронировать столик.',
            url: '?poi[uri]=ymapsbm1://org?oid=1219161144',
            selector: getSelectorByText('Забронировать столик', cssSelectors.search.businessCard.cardFeatureView),
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.cta.regexp),
            description: '1орг. Таб «Меню». Заказать доставку.',
            url: '/org/1219161144/?tab=menu',
            selector: getSelectorByText('Заказать доставку', cssSelectors.search.businessCard.fullItems.controls),
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.cta.regexp),
            description: '1орг. Сниппет. Забронировать столик.',
            url: '/org/1219161144/',
            selector: getSelectorByText('Забронировать столик', cssSelectors.search.businessCard.cardFeatureView),
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
