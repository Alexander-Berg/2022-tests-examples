const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const _ = require('lodash');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const breadcrumbsPublicApi = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

const ListingPopularMMM = require('./ListingPopularMMM');

const MARK_LEVEL = breadcrumbsPublicApi.data.find(item => item.level === 'MARK_LEVEL');
const MODEL_LEVEL = breadcrumbsPublicApi.data.find(item => item.level === 'MODEL_LEVEL');

const CONFIG = {
    data: {
        pageType: 'listing',
    },
};

const TEST_CASES = [
    {
        name: 'без параметра catalog_filter',
        state: {
            breadcrumbsPublicApi,
            config: CONFIG,
        },
        items: MARK_LEVEL.entities,
        mark: '',
        type: 'marks',
    },
    {
        name: 'с пустым параметром catalog_filter',
        state: {
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ {} ],
                    },
                },
            },
            breadcrumbsPublicApi,
            config: CONFIG,
        },
        items: MARK_LEVEL.entities,
        mark: '',
        type: 'marks',
    },
    {
        name: 'c выбранной маркой',
        state: {
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'FORD' } ],
                    },
                },
            },
            breadcrumbsPublicApi,
            config: CONFIG,
        },
        items: MODEL_LEVEL.entities,
        mark: 'FORD',
        type: 'models',
    },
    {
        name: 'c выбранной маркой и моделью',
        state: {
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'FORD', model: 'ECOSPORT' } ],
                    },
                },
            },
            breadcrumbsPublicApi,
            config: CONFIG,
        },
        items: [],
        mark: '',
        type: undefined,
    },
    {
        name: 'выбрано несколько марок',
        state: {
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { mark: 'FORD' }, { mark: 'BMW' } ],
                    },
                },
            },
            breadcrumbsPublicApi,
            config: CONFIG,
        },
        items: [],
        mark: '',
        type: undefined,
    },
    {
        name: 'выбран вендор',
        state: {
            listing: {
                data: {
                    search_parameters: {
                        catalog_filter: [ { vendor: 'VENDOR1' } ],
                    },
                },
            },
            breadcrumbsPublicApi,
            config: CONFIG,
        },
        items: [],
        mark: '',
        type: undefined,
    },
    {
        name: 'выбрано исключение',
        state: {
            listing: {
                data: {
                    search_parameters: {
                        exclude_catalog_filter: [ { mark: 'FORD' } ],
                    },
                },
            },
            breadcrumbsPublicApi,
            config: CONFIG,
        },
        items: [],
        mark: '',
        type: undefined,
    },
];

TEST_CASES.forEach((testCase) => {
    it(`ListingPopularMMM map state to props ${ testCase.name }`, () => {
        const store = mockStore(testCase.state);
        const tree = shallow(
            <Provider store={ store }>
                <ListingPopularMMM onClick={ _.noop }/>
            </Provider >,
        ).dive();
        expect(tree.find('ListingPopularMMM').props().items).toEqual(testCase.items);
        expect(tree.find('ListingPopularMMM').props().mark).toEqual(testCase.mark);
        expect(tree.find('ListingPopularMMM').props().type).toEqual(testCase.type);
    });
});
