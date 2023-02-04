import cssSelectors from '../../../common/css-selectors';
import counterGenerator from '../../../lib/counter-generator';
import {ANALYTIC_NAMES} from './analytic-names';

const URLS = {
    business: '?oid=1357660703&ol=biz',
    org: '/org/1357660703/',
    poi: '?poi[uri]=ymapsbm1://org?oid=1357660703'
};

counterGenerator({
    name: 'Сниппет бронирования.',
    isMainMetric: true,
    specs: [
        {
            name: new RegExp(ANALYTIC_NAMES.bookingLink.regexp),
            description: 'Организация. Вариант бронирования.',
            url: URLS.business,
            selector: cssSelectors.hotelsBooking.firstLink,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.hotelsBooking.cardView, {vertical: 'center'});
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookingLink.regexp),
            description: 'POI. Вариант бронирования.',
            url: URLS.poi,
            selector: cssSelectors.hotelsBooking.firstLink,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.hotelsBooking.cardView, {vertical: 'center'});
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookingLink.regexp),
            description: '1орг. Вариант бронирования.',
            url: URLS.org,
            selector: cssSelectors.hotelsBooking.firstLink,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.hotelsBooking.cardView, {vertical: 'center'});
                    },
                    type: 'click'
                }
            ]
        },

        {
            name: new RegExp(ANALYTIC_NAMES.bookingControl.regexp),
            description: 'Организация. Контрол выбора времени.',
            url: URLS.business,
            selector: cssSelectors.hotelsBooking.controls.datepicker,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.hotelsBooking.cardView, {vertical: 'center'});
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookingControl.regexp),
            description: 'POI. Контрол выбора времени.',
            url: URLS.poi,
            selector: cssSelectors.hotelsBooking.controls.datepicker,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.hotelsBooking.cardView, {vertical: 'center'});
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookingControl.regexp),
            description: '1орг. Контрол выбора времени.',
            url: URLS.org,
            selector: cssSelectors.hotelsBooking.controls.datepicker,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.hotelsBooking.cardView, {vertical: 'center'});
                    },
                    type: 'click'
                }
            ]
        },

        {
            name: new RegExp(ANALYTIC_NAMES.bookingControl.regexp),
            description: 'Организация. Контрол выбора количества людей.',
            url: URLS.business,
            selector: cssSelectors.hotelsBooking.controls.personsSelect,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.hotelsBooking.cardView, {vertical: 'center'});
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookingControl.regexp),
            description: 'POI. Контрол выбора количества людей.',
            url: URLS.poi,
            selector: cssSelectors.hotelsBooking.controls.personsSelect,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.hotelsBooking.cardView, {vertical: 'center'});
                    },
                    type: 'click'
                }
            ]
        },
        {
            name: new RegExp(ANALYTIC_NAMES.bookingControl.regexp),
            description: '1орг. Контрол выбора количества людей.',
            url: URLS.org,
            selector: cssSelectors.hotelsBooking.controls.personsSelect,
            events: [
                {
                    setup: async (browser) => {
                        await browser.scrollIntoView(cssSelectors.hotelsBooking.cardView, {vertical: 'center'});
                    },
                    type: 'click'
                }
            ]
        }
    ]
});
