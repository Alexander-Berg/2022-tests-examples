import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const urls = {
    org: '/org/235255021665/',
    poi: '?poi[uri]=ymapsbm1://org?oid=235255021665'
};

counterGenerator({
    name: 'Геопродукт.',
    isMainMetric: true,
    specs: [
        // Клик по cta-кнопке
        {
            name: new RegExp(ANALYTIC_NAMES.geoproductCta.regexp),
            description: 'Организация. Cta-кнопка геопродукта.',
            url: urls.poi,
            selector: cssSelectors.actionBar.actionButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.geoproductCta.regexp),
            description: 'Orgpage. Cta-кнопка геопродукта.',
            url: urls.org,
            selector: cssSelectors.actionBar.actionButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.geoproductCta.regexp),
            description: 'Тач. Организация. Cta-кнопка геопродукта.',
            isMobile: true,
            url: urls.org,
            selector: `${cssSelectors.sidebar.panel} ${cssSelectors.actionBar.actionButton}`,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.geoproductCta.regexp),
            description: 'Превью организации. Cta-кнопка геопродукта.',
            url: '?ll=37.627224,55.758134&z=19',
            selector: cssSelectors.businessMapPreview.geoproductCta,
            events: [
                {
                    setup: async (browser) => {
                        await browser.simulateGeoHover({
                            point: [37.627097, 55.758221],
                            description: 'Навести курсор на ресторан "Ткемали"'
                        });
                    },
                    type: 'click'
                }
            ]
        },
        // Клик по блоку геопродукта. Разворачивание блока
        {
            name: new RegExp(ANALYTIC_NAMES.geoproductOpen.regexp),
            description: 'Организация. Разворачивание геопродукта.',
            url: urls.poi,
            selector: cssSelectors.cardOffer.view,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.geoproductOpen.regexp),
            description: '1орг. Разворачивание геопродукта.',
            url: urls.org,
            selector: cssSelectors.cardOffer.view,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        // Переход на сайт геопродукта.
        {
            name: new RegExp(ANALYTIC_NAMES.geoproductLink.regexp),
            description: 'Организация. Переход на сайт геопродукта.',
            url: urls.poi,
            selector: cssSelectors.cardOffer.link,
            events: [
                {
                    type: 'click',
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.cardOffer.view);
                    }
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.geoproductLink.regexp),
            description: '1орг. Переход на сайт геопродукта',
            url: urls.org,
            selector: cssSelectors.cardOffer.link,
            events: [
                {
                    type: 'click',
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.cardOffer.view);
                    }
                }
            ]
        }
    ]
});
