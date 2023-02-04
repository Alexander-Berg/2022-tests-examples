import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1413969678',
    org: '/org/makdonalds/1413969678/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1413969678'
};

counterGenerator({
    name: 'Кнопка "Закладка".',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.bookmark.regexp),
            description: 'Организация.',
            url: URLS.business,
            selector: cssSelectors.actionBar.bookmarkButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookmark.regexp),
            description: 'Тач. Организация.',
            isMobile: true,
            url: URLS.business,
            selector: cssSelectors.actionBar.bookmarkButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },

        {
            name: new RegExp(ANALYTIC_NAMES.bookmark.regexp),
            description: 'POI.',
            url: URLS.poi,
            selector: cssSelectors.actionBar.bookmarkButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookmark.regexp),
            description: 'Тач. POI.',
            isMobile: true,
            url: URLS.poi,
            selector: cssSelectors.actionBar.bookmarkButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },

        {
            name: new RegExp(ANALYTIC_NAMES.bookmark.regexp),
            description: '1орг.',
            url: URLS.org,
            selector: cssSelectors.actionBar.bookmarkButton,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookmark.regexp),
            description: 'Тач. 1орг.',
            isMobile: true,
            url: URLS.org,
            selector: cssSelectors.actionBar.bookmarkButton,
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
