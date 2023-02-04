/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const _ = require('lodash');

const MockDate = require('mockdate');

const listingContext = require('./listingContext');

beforeEach(() => {
    MockDate.set('2018-01-05');
});

afterEach(() => {
    MockDate.reset();
});

const searchData = {
    position: 2,
    key: 1591266886159,
    sale_id: '111-222',
    data: {
        pager: {
            total_page_count: 58,
            total_offers_count: 2134,
            page: 2,
            page_size: 37,
            from: 38,
            to: 74,
            current: 2,
        },
        query: {
            section: 'all',
            category: 'cars',
            sort: 'fresh_relevance_1-desc',
        },
        ids: [
            {
                'autoru-id': '1097450334',
                'autoru-hash-code': '286f3baa',
                category: 'cars',
                section: 'new',
                mark: { id: 'bmw' },
                model: { id: '5er' },
                imageUrl: '//avatars.mds.yandex.net/get-verba/937147/2a000001694e342d88f4d451d3d05035f7c7/small',
            },
            {
                'autoru-id': '1098483592',
                'autoru-hash-code': 'b9e9ab31',
                category: 'cars',
                section: 'used',
                mark: { id: 'lifan' },
                model: { id: 'solano' },
                imageUrl: '//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/554e5fb189ea366f4fb224a43b45819e/small',
            },
        ],
    },
};

// eslint-disable-next-line max-len
const lsItem = '[{"key":1591266886159,"data":{"pager":{"total_page_count":58,"total_offers_count":2134,"page":2,"page_size":37,"from":38,"to":74,"current":2},"query":{"section":"all","category":"cars","sort":"fresh_relevance_1-desc"},"ids":[{"autoru-id":"1097450334","autoru-hash-code":"286f3baa","category":"cars","section":"new","mark":{"id":"bmw"},"model":{"id":"5er"},"imageUrl":"//avatars.mds.yandex.net/get-verba/937147/2a000001694e342d88f4d451d3d05035f7c7/small"},{"autoru-id":"1098483592","autoru-hash-code":"b9e9ab31","category":"cars","section":"used","mark":{"id":"lifan"},"model":{"id":"solano"},"imageUrl":"//images.mds-proxy.test.avto.ru/get-autoru-vos/65698/554e5fb189ea366f4fb224a43b45819e/small"}]},"expires":1591270486159,"clicks":[{"navigation":{"sp":2,"pager":{"total_page_count":58,"total_offers_count":2134,"page":2,"page_size":37,"from":38,"to":74,"current":2}},"ts":1515110400000,"sale_id":"111-222","position":2}]}]';

describe('save', () => {
    it('не должен сохранить контекст, если позиция клика отрицательная', () => {
        listingContext.save({ position: -2 });
        expect(localStorage.setItem).not.toHaveBeenCalled();
    });

    it('должен сохранить контекст', () => {
        listingContext.save(searchData);
        expect(localStorage.setItem.mock.calls[0]).toMatchSnapshot();
    });

    it('должен поменять клики в контексте', () => {
        localStorage.getItem.mockImplementation(() => lsItem);
        listingContext.save({ ...searchData, position: 1 });
        expect(localStorage.setItem.mock.calls[0]).toMatchSnapshot();
    });

    it('должен поменять данные в контексте', () => {
        const newSearchData = _.cloneDeep(searchData);
        newSearchData.data.ids = [];
        localStorage.getItem.mockImplementation(() => lsItem);
        listingContext.save(newSearchData);
        expect(localStorage.setItem.mock.calls[0]).toMatchSnapshot();
    });
});

describe('get', () => {
    it('должен достать нужный контекст по id объявления', () => {
        localStorage.getItem.mockImplementation(() => lsItem);
        expect(listingContext.get('111-222')).toMatchSnapshot();
    });

    it('не должен достать контекст, если там нет нужного объявления', () => {
        localStorage.getItem.mockImplementation(() => lsItem);
        expect(listingContext.get('222-222')).toBeNull();
    });
});

describe('getByKey', () => {
    it('должен достать нужный контекст по ключу', () => {
        localStorage.getItem.mockImplementation(() => lsItem);
        expect(listingContext.getByKey(1591266886159)).toMatchSnapshot();
    });

    it('не должен достать контекст, если там нет нужного ключа', () => {
        localStorage.getItem.mockImplementation(() => lsItem);
        expect(listingContext.getByKey(123456)).toBeUndefined();
    });
});
