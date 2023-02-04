import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?ol=biz&oid=1413969678',
    org: '/org/makdonalds/1413969678/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1413969678'
};

counterGenerator({
    name: 'Отзывы.',
    isMainMetric: true,
    specs: [
        // Отправка отзывов
        {
            name: new RegExp(ANALYTIC_NAMES.sendUgc.regexp),
            description: '1орг. Отправка отзыва',
            url: `${URLS.org}reviews/`,
            selector: cssSelectors.ugc.reviewForm.sendButton,
            login: true,
            events: [
                {
                    type: 'click',
                    setup: async function (browser) {
                        await browser.waitAndClick(cssSelectors.ugc.ratingEdit.nthStar.replace('%i', '3'));
                    }
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.sendUgc.regexp),
            description: '1орг. Отправка отзыва после загрузки фото',
            url: `${URLS.org}gallery/`,
            selector: cssSelectors.ugc.reviewForm.sendButton,
            login: true,
            events: [
                {
                    type: 'click',
                    setup: async function (browser) {
                        await browser.waitAndClick(cssSelectors.photo.addPhotoButton);
                        await browser.addStyles(`${cssSelectors.photo.addPhotos.input} {display: block !important}`);
                        await browser.uploadImage(cssSelectors.photo.addPhotos.input, '300x300');
                        await browser.waitAndClick(cssSelectors.photo.addPhotoForm.sendButton);
                        await browser.waitAndClick(cssSelectors.ugc.ratingEdit.nthStar.replace('%i', '3'));
                    }
                }
            ]
        },

        // Просмотр отзывов
        {
            name: new RegExp(ANALYTIC_NAMES.openUgc.regexp),
            description: 'Организация. Просмотр отзывов',
            url: URLS.business,
            selector: cssSelectors.ugc.review.card.showMore,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.openUgc.regexp),
            description: '1орг. Просмотр отзывов',
            url: URLS.org,
            selector: cssSelectors.ugc.review.card.showMore,
            events: [
                {
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.openUgc.regexp),
            description: 'POI. Просмотр отзывов',
            url: URLS.poi,
            selector: cssSelectors.ugc.review.card.showMore,
            events: [
                {
                    type: 'click'
                }
            ]
        }
    ]
});
