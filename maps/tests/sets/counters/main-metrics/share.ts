import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1413969678',
    org: '/org/makdonalds/1413969678/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1413969678'
};

counterGenerator({
    name: 'Кнопка "Поделиться".',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.share.regexp),
            description: 'Организация.',
            url: URLS.business,
            selector: cssSelectors.actionBar.shareButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.share.regexp),
            description: 'Тач. Организация.',
            isMobile: true,
            url: URLS.business,
            selector: cssSelectors.actionBar.shareButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },

        {
            name: new RegExp(ANALYTIC_NAMES.share.regexp),
            description: 'POI.',
            url: URLS.poi,
            selector: cssSelectors.actionBar.shareButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.share.regexp),
            description: 'Тач. POI.',
            isMobile: true,
            url: URLS.poi,
            selector: cssSelectors.actionBar.shareButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },

        {
            name: new RegExp(ANALYTIC_NAMES.share.regexp),
            description: '1орг.',
            url: URLS.org,
            selector: cssSelectors.actionBar.sendToPhoneButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.share.regexp),
            description: 'Тач. 1орг.',
            isMobile: true,
            url: URLS.org,
            selector: cssSelectors.actionBar.shareButton,
            events: [
                {
                    setup: async (browser) => {
                        await browser.swipeShutter('down');
                        await browser.waitForHidden(cssSelectors.sidebar.panel);
                        await browser.waitForVisible(cssSelectors.sidebar.minicard);
                    },
                    type: 'click'
                }
            ]
        }
    ]
});

counterGenerator({
    name: 'Блок "Поделиться".',
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.shareBlock.regexp),
            description: 'Отправить в телефон',
            url: URLS.business,
            selector: cssSelectors.cardShare.sendToPhone,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.actionBar.shareButton);
                        await browser.waitForVisible(cssSelectors.cardShare.view);
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.shareBlock.regexp),
            description: 'Короткая ссылка',
            url: URLS.business,
            selector: cssSelectors.cardShare.shortLinkClipboard,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.actionBar.shareButton);
                        await browser.waitForVisible(cssSelectors.cardShare.view);
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.shareBlock.regexp),
            description: 'Координаты',
            url: URLS.business,
            selector: cssSelectors.cardShare.coordinatesClipboard,
            events: [
                {
                    setup: async (browser) => {
                        await browser.waitAndClick(cssSelectors.actionBar.shareButton);
                        await browser.waitForVisible(cssSelectors.cardShare.view);
                    },
                    type: 'click'
                }
            ]
        }
    ]
});
