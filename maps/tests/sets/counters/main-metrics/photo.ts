import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1131878730',
    org: '/org/1131878730/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1131878730',
    serp: '/213/moscow/search/Где поесть/'
};

counterGenerator({
    name: 'Фото.',
    isMainMetric: true,
    specs: [
        // Клик в фото
        {
            name: new RegExp(ANALYTIC_NAMES.openPhotoCard.regexp),
            description: 'Организация.',
            url: URLS.business,
            selector: cssSelectors.search.businessCard.photos,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.openPhotoOrg.regexp),
            description: '1орг.',
            url: URLS.org,
            selector: cssSelectors.orgpage.photo.item,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.openPhotoOrg.regexp),
            description: 'Превью организации. Карусель фотографий',
            url: '?ll=37.606718,55.762324&z=19',
            selector: cssSelectors.businessMapPreview.photo,
            events: [
                {
                    setup: async (browser) => {
                        await browser.simulateGeoHover({
                            point: [37.606385, 55.762433],
                            description: 'Навести курсор на гастропаб "Челси"'
                        });
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.openPhotoCard.regexp),
            description: 'POI.',
            url: URLS.poi,
            selector: cssSelectors.search.businessCard.photos,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.openPhotoSerp.regexp),
            description: 'Рубричный поиск.',
            url: URLS.serp,
            selector: cssSelectors.search.gallery.photo,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.openPhotoSerp.regexp),
            description: 'ТАЧ. Рубричный поиск.',
            isMobile: true,
            url: URLS.serp,
            selector: cssSelectors.search.gallery.photo,
            events: [
                {
                    type: 'click',
                    setup: async (browser) => {
                        await browser.swipeShutter('down');
                        await browser.waitForHidden(cssSelectors.sidebar.panel);
                        await browser.waitForVisible(cssSelectors.sidebar.minicard);
                    }
                }
            ]
        }
    ]
});
