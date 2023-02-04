/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ListingSeoTagCarousel = require('./ListingSeoTagCarousel');

describe('тег новинки', () => {
    it('должен отрендерить тег, если есть новинки', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        category: 'cars',
                        section: 'all',
                    },
                    new4newCount: 10,
                },
            },
            listingPriceRanges: {
                data: [
                    {
                        price_to: 100000,
                        offers_count: 5,
                    },
                ],
            },
        });

        const context = {
            ...contextMock,
            link: (routeName) => {
                if (routeName === 'listing') {
                    return 'link/listing/cars/all/tag/new4new/';
                }
            },
        };

        const tree = shallow(
            <ListingSeoTagCarousel
                onClick={ _.noop }
            />,
            { context: { ...context, store } },
        ).dive();
        expect(tree.find({ searchParamValue: 'new4new', searchParamName: 'search_tag' })).toHaveLength(1);
    });

    it('не должен отрендерить тег, если нет новинок', () => {
        const context = contextMock;
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {},
                    new4newCount: 0,
                },
            },
            listingPriceRanges: {
                data: [
                    {
                        price_to: 100000,
                        offers_count: 5,
                    },
                ],
            },
        });

        const tree = shallow(
            <ListingSeoTagCarousel
                onClick={ _.noop }
            />,
            { context: { ...context, store } },
        ).dive();
        expect(tree.find({ searchParamValue: 'new4new', searchParamName: 'search_tag' })).toHaveLength(0);
    });

});

describe('Перелинковка с кузовами', () => {

    it('должен отрендерить перелинковку с кузовами в разделе cars', () => {
        const storeMock = {
            listing: {
                data: {
                    search_parameters: {
                        category: 'cars',
                        section: 'all',
                    },
                },
            },
        };

        const context = {
            ...contextMock,
            link: (routeName, searchParams, ...args) => {
                if (routeName === 'listing') {
                    return 'link/listing/cars/all/tag/new4new/';
                }
                return contextMock.link(...args);
            },
        };

        const store = mockStore(storeMock);

        const tree = shallow(
            <ListingSeoTagCarousel
                onClick={ _.noop }
            />,
            { context: { ...context, store } },
        ).dive();

        expect(tree.find({ searchParamName: 'body_type_group' })).toHaveLength(9);
    });

    it('не должен рендерить перелинковку с кузовами в разделе trucks', () => {
        const storeMock = {
            listing: {
                data: {
                    search_parameters: {
                        category: 'trucks',
                        section: 'all',
                        trucks_category: 'TRAILER',
                    },
                },
            },
        };

        const context = contextMock;

        const store = mockStore(storeMock);

        const tree = shallow(
            <ListingSeoTagCarousel
                onClick={ _.noop }
            />,
            { context: { ...context, store } },
        ).dive();

        expect(tree.find({ searchParamName: 'body_type_group' })).toHaveLength(0);
    });

    it('не должен рендерить перелинковку с кузовами в разделе moto', () => {
        const storeMock = {
            listing: {
                data: {
                    search_parameters: {
                        category: 'moto',
                        moto_category: 'MOTORCYCLE',
                        section: 'all',
                    },
                },
            },
        };

        const context = contextMock;

        const store = mockStore(storeMock);

        const tree = shallow(
            <ListingSeoTagCarousel
                onClick={ _.noop }
            />,
            { context: { ...context, store } },
        ).dive();

        expect(tree.find({ searchParamName: 'body_type_group' })).toHaveLength(0);
    });

    it('должен отрендерить тег "В кредит" для марки', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        category: 'cars',
                        section: 'all',
                        catalog_filter: [ {
                            mark: 'BMW',
                        } ],
                    },
                },
            },
        });

        const context = {
            ...contextMock,
            link: (routeName, searchParams, ...args) => {
                if (routeName === 'listing') {
                    return 'link/listing/cars/all/tag/new4new/';
                }
                return contextMock.link(...args);
            },
        };

        const tree = shallow(
            <ListingSeoTagCarousel
                onClick={ _.noop }
            />,
            { context: { ...context, store } },
        ).dive();
        expect(tree.find({ searchParamValue: 'on_credit', searchParamName: 'on_credit' })).toHaveLength(1);
    });

    it('не должен отрендерить тег "В кредит" для марки с фильтром', () => {
        const context = contextMock;
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        category: 'cars',
                        section: 'all',
                        catalog_filter: [ {
                            mark: 'BMW',
                        } ],
                        body_type_group: [
                            'HATCHBACK',
                            'HATCHBACK_3_DOORS',
                            'HATCHBACK_5_DOORS',
                            'LIFTBACK',
                        ],
                    },
                },
            },
        });

        const tree = shallow(
            <ListingSeoTagCarousel
                onClick={ _.noop }
            />,
            { context: { ...context, store } },
        ).dive();
        expect(tree.find({ searchParamValue: 'on_credit', searchParamName: 'on_credit' })).toHaveLength(0);
    });

    it('должен отрендерить тег "В кредит", когда выбран фильтр кузова', () => {
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        category: 'cars',
                        section: 'all',
                        body_type_group: [
                            'HATCHBACK',
                            'HATCHBACK_3_DOORS',
                            'HATCHBACK_5_DOORS',
                            'LIFTBACK',
                        ],
                    },
                },
            },
        });

        const context = {
            ...contextMock,
            link: (routeName, searchParams, ...args) => {
                if (routeName === 'listing') {
                    return 'link/listing/cars/all/tag/new4new/';
                }
                return contextMock.link(...args);
            },
        };

        const tree = shallow(
            <ListingSeoTagCarousel
                onClick={ _.noop }
            />,
            { context: { ...context, store } },
        ).dive();
        expect(tree.find({ searchParamValue: 'on_credit', searchParamName: 'on_credit' })).toHaveLength(1);
    });

    it('не должен отрендерить тег "В кредит" для поколения', () => {
        const context = contextMock;
        const store = mockStore({
            listing: {
                data: {
                    search_parameters: {
                        category: 'cars',
                        section: 'all',
                        catalog_filter: [ {
                            mark: 'BMW',
                            model: '3ER',
                            generation: '21398591',
                        } ],
                    },
                },
            },
        });

        const tree = shallow(
            <ListingSeoTagCarousel
                onClick={ _.noop }
            />,
            { context: { ...context, store } },
        ).dive();
        expect(tree.find({ searchParamValue: 'on_credit', searchParamName: 'on_credit' })).toHaveLength(0);
    });
});
