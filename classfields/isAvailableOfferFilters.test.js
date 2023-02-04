const _ = require('lodash');

const isAvailableOfferFilters = require('./isAvailableOfferFilters');

const tariffMock = require('www-cabinet/react/dataDomain/calls/mocks/withTariffs.mock');
const filtersMock = require('www-cabinet/react/dataDomain/calls/mocks/withFilters.mock');

const FIELD_NAMES = require('www-cabinet/data/calls/filter-call-field-names.json');
const FILTER_CATEGORY_VALUES = require('www-cabinet/data/calls/filter-call-category-values.json');
const FILTER_SECTION_VALUES = require('www-cabinet/data/calls/filter-call-section-values.json');

let state;

beforeEach(() => {
    const filters = _.cloneDeep(filtersMock.filters);
    filters[FIELD_NAMES.CATEGORY] = FILTER_CATEGORY_VALUES.CATEGORY_UNKNOWN;
    filters[FIELD_NAMES.SECTION] = FILTER_SECTION_VALUES.SECTION_UNKNOWN;

    state = {
        calls: {
            settings: {
                offers_stat_enabled: true,
            },
            filters,
        },
        promoPopup: {
            tariffs: _.cloneDeep(tariffMock.tariffs),
        },
    };
});

it('всегда должен отдавать false, если настройка коллтрекинга по офферам отключена', () => {
    state.calls.settings.offers_stat_enabled = false;

    const result = isAvailableOfferFilters(state);

    expect(result).toBe(false);
});

describe('если категория не выбрана', () => {
    it('должен отдавать true, если есть подключенный тариф легковых с пробегом', () => {
        state.promoPopup.tariffs[0].enabled = true;

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(true);
    });

    it('должен отдавать true, если есть подключенный тариф легковых с типом звонков', () => {
        state.promoPopup.tariffs[1].enabled = true;

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(true);
    });

    it('должен отдавать false, если подключен только тариф комтранс с типом звонков', () => {
        state.promoPopup.tariffs[2].enabled = true;

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(false);
    });
});

describe('если выбрана категория "Легковые новые"', () => {
    it('должен отдавать false, если не подключен тариф', () => {
        state.calls.filters[FIELD_NAMES.CATEGORY] = FILTER_CATEGORY_VALUES.CARS;
        state.calls.filters[FIELD_NAMES.SECTION] = FILTER_SECTION_VALUES.NEW;

        state.promoPopup.tariffs[0].enabled = true;
        state.promoPopup.tariffs[1].enabled = false;
        state.promoPopup.tariffs[2].enabled = true;
        state.promoPopup.tariffs[3].enabled = true;

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(false);
    });

    it('должен отдавать false, если подключен тариф типа не звонков', () => {
        state.calls.filters[FIELD_NAMES.CATEGORY] = FILTER_CATEGORY_VALUES.CARS;
        state.calls.filters[FIELD_NAMES.SECTION] = FILTER_SECTION_VALUES.NEW;

        state.promoPopup.tariffs[1].enabled = true;
        state.promoPopup.tariffs[1].type = 'SINGLE';

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(false);
    });

    it('должен отдавать true, если подключен тариф звонков', () => {
        state.calls.filters[FIELD_NAMES.CATEGORY] = FILTER_CATEGORY_VALUES.CARS;
        state.calls.filters[FIELD_NAMES.SECTION] = FILTER_SECTION_VALUES.NEW;

        state.promoPopup.tariffs[1].enabled = true;

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(true);
    });
});

describe('если выбрана категория "Легковые c пробегом"', () => {
    it('должен отдавать false, если не подключен тариф', () => {
        state.calls.filters[FIELD_NAMES.CATEGORY] = FILTER_CATEGORY_VALUES.CARS;
        state.calls.filters[FIELD_NAMES.SECTION] = FILTER_SECTION_VALUES.USED;

        state.promoPopup.tariffs[0].enabled = false;
        state.promoPopup.tariffs[1].enabled = true;
        state.promoPopup.tariffs[2].enabled = true;
        state.promoPopup.tariffs[3].enabled = true;

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(false);
    });

    it('должен отдавать true, если подключен тариф', () => {
        state.calls.filters[FIELD_NAMES.CATEGORY] = FILTER_CATEGORY_VALUES.CARS;
        state.calls.filters[FIELD_NAMES.SECTION] = FILTER_SECTION_VALUES.USED;

        state.promoPopup.tariffs[0].enabled = true;

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(true);
    });
});

describe('если выбрана категория "Комтранс"', () => {
    it('должен всегда отдавать false', () => {
        state.calls.filters[FIELD_NAMES.CATEGORY] = FILTER_CATEGORY_VALUES.TRUCKS;
        state.calls.filters[FIELD_NAMES.SECTION] = FILTER_SECTION_VALUES.SECTION_UNKNOWN;

        state.promoPopup.tariffs[0].enabled = true;
        state.promoPopup.tariffs[1].enabled = true;
        state.promoPopup.tariffs[2].enabled = true;
        state.promoPopup.tariffs[3].enabled = true;

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(false);
    });
});

describe('если выбрана категория "Мото"', () => {
    it('должен всегда отдавать false', () => {
        state.calls.filters[FIELD_NAMES.CATEGORY] = FILTER_CATEGORY_VALUES.MOTO;
        state.calls.filters[FIELD_NAMES.SECTION] = FILTER_SECTION_VALUES.SECTION_UNKNOWN;

        state.promoPopup.tariffs[0].enabled = true;
        state.promoPopup.tariffs[1].enabled = true;
        state.promoPopup.tariffs[2].enabled = true;
        state.promoPopup.tariffs[3].enabled = true;

        const result = isAvailableOfferFilters(state);

        expect(result).toBe(false);
    });
});
