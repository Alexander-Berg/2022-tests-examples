/**
 * @jest-environment node
 */

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const controller = require('./convert-listing-url-to-searcher-params');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен вернуть 404 для пустого урла', () => {
    return de.run(controller, {
        context,
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toEqual({
                error: {
                    id: 'UNKNOWN_URL_TO_CONVERT',
                    status_code: 404,
                },
            });
        });
});

it('должен вернуть 404 для неизвестный урлов', () => {
    return de.run(controller, {
        context,
        params: { url: '/this-is-404-route/' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toEqual({
                error: {
                    id: 'UNKNOWN_URL_TO_CONVERT',
                    status_code: 404,
                },
            });
        });
});

describe('listing', () => {
    /* eslint-disable max-len */

    const TESTS = [
        // MOTO
        {
            params: {
                url: '/chelyabinsk/snegohody/russkaya_mehanika/tayga_patrul_551/used/?',
            },
            result: {
                category: 'moto',
                query: {
                    moto_category: 'SNOWMOBILE',
                    state: 'USED',
                    'mark-model-nameplate': [
                        'RUSSKAYA_MEHANIKA#TAYGA_PATRUL_551',
                    ],
                    image: 'true',
                    custom_state_key: 'CLEARED',
                    currency: 'RUR',
                    top_days: 'off',
                    page_num_offers: 1,
                    rid: [
                        56,
                    ],
                    sort_offers: 'cr_date-DESC',
                    page_size: 37,
                    mark: '',
                    model: '',
                    super_gen: '',
                    autoru_top_count: 3,
                    top_expected_count: 0,
                    autoru_fresh_count: 0,
                    fresh_expected_count: 0,
                    steering_wheel: 'LEFT',
                },
            },
        },
        {
            params: {
                url: '/chelyabinsk/snowmobile/russkaya_mehanika/tayga_patrul_551/used/?',
            },
            result: {
                category: 'moto',
                query: {
                    moto_category: 'SNOWMOBILE',
                    state: 'USED',
                    'mark-model-nameplate': [
                        'RUSSKAYA_MEHANIKA#TAYGA_PATRUL_551',
                    ],
                    image: 'true',
                    custom_state_key: 'CLEARED',
                    currency: 'RUR',
                    top_days: 'off',
                    page_num_offers: 1,
                    rid: [
                        56,
                    ],
                    sort_offers: 'cr_date-DESC',
                    page_size: 37,
                    mark: '',
                    model: '',
                    super_gen: '',
                    autoru_top_count: 3,
                    top_expected_count: 0,
                    autoru_fresh_count: 0,
                    fresh_expected_count: 0,
                    steering_wheel: 'LEFT',
                },
            },
        },

        // TRUCKS
        {
            params: {
                url: '/rossiya/legkie-gruzoviki/kia/bongo/all/?from=wizard.commercial-model&utm_source=auto_wizard&utm_medium=touch&utm_campaign=commercial-model',
            },
            result: {
                category: 'trucks',
                query: {
                    trucks_category: 'LCV',
                    state: [
                        'NEW',
                        'USED',
                    ],
                    'mark-model-nameplate': [
                        'KIA#BONGO',
                    ],
                    image: 'true',
                    custom_state_key: 'CLEARED',
                    currency: 'RUR',
                    top_days: 'off',
                    page_num_offers: 1,
                    rid: [
                        225,
                    ],
                    sort_offers: 'cr_date-DESC',
                    page_size: 37,
                    mark: '',
                    model: '',
                    super_gen: '',
                    autoru_top_count: 3,
                    top_expected_count: 0,
                    autoru_fresh_count: 0,
                    fresh_expected_count: 0,
                },
            },
        },
        {
            params: {
                url: '/rossiya/agricultural/all/?from=wizard.commercial-category&utm_source=auto_wizard&utm_medium=touch&body_key=MOTOBLOCK&utm_campaign=commercial-category.subcategory',
            },
            result: {
                category: 'trucks',
                query: {
                    trucks_category: 'AGRICULTURAL',
                    body_key: 'MOTOBLOCK',
                    state: [
                        'NEW',
                        'USED',
                    ],
                    image: 'true',
                    custom_state_key: 'CLEARED',
                    currency: 'RUR',
                    top_days: 'off',
                    page_num_offers: 1,
                    rid: [
                        225,
                    ],
                    sort_offers: 'cr_date-DESC',
                    page_size: 37,
                    autoru_top_count: 3,
                    top_expected_count: 0,
                    autoru_fresh_count: 0,
                    fresh_expected_count: 0,
                },
            },
        },
        {
            params: {
                url: '/rossiya/agricultural/new/?body_key=MOTOBLOCK',
            },
            result: {
                category: 'trucks',
                query: {
                    trucks_category: 'AGRICULTURAL',
                    body_key: 'MOTOBLOCK',
                    state: 'NEW',
                    image: 'true',
                    custom_state_key: '',
                    currency: 'RUR',
                    top_days: 'off',
                    page_num_offers: 1,
                    rid: [
                        225,
                    ],
                    search_tag: [],
                    sort_offers: 'cr_date-DESC',
                    steering_wheel: '',
                    page_size: 37,
                    autoru_top_count: 3,
                    top_expected_count: 0,
                    autoru_fresh_count: 0,
                    fresh_expected_count: 0,
                },
            },
        },
    ];

    TESTS.forEach((testCase) => {
        it(`должен вернуть правильный ответ для ${ testCase.params.url }`, () => {
            return de.run(controller, {
                context,
                params: testCase.params,
            })
                .then(() => {
                    expect(res.send).toHaveBeenCalledWith(testCase.result);
                });
        });
    });
});
