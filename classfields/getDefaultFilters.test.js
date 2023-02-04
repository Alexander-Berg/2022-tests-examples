const mockdate = require('mockdate');

const getDefaultFilters = require('./getDefaultFilters');

beforeEach(() => {
    mockdate.set('2020-06-18');
});

afterEach(() => {
    mockdate.reset();
});

it('должен вернуть дефолтные фильтры', () => {
    expect(getDefaultFilters()).toEqual({
        platforms: [],
        call_result: 'ALL_RESULT_GROUP',
        call_target: 'ALL_TARGET_GROUP',
        callback: 'ALL_SOURCE_GROUP',
        category: 'CATEGORY_UNKNOWN',
        from: '2020-05-19',
        section: 'SECTION_UNKNOWN',
        tag: [],
        to: '2020-06-18',
        unique: 'ALL_UNIQUE_GROUP',
        text_domain: 'ALL_DOMAIN',
        text_query: '',
    });
});
