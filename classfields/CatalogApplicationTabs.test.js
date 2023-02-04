const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CatalogApplicationTabs = require('./CatalogApplicationTabs');

const TEST_CASES_CATALOG = [
    {
        name: 'cars',
        params: { category: 'cars' },
    },
    {
        name: 'cars с маркой',
        params: {
            category: 'cars',
            mark: 'bmw',
        },
    },
    {
        name: 'cars с кучей всего',
        params: {
            category: 'cars',
            complectation_id: '21397498_21397672_21397560',
            configuration_id: '21397498',
            mark: 'mitsubishi',
            model: 'outlander',
            super_gen: '21397304',
        },
    },
];

TEST_CASES_CATALOG.forEach((testCase) => {
    it(`CatalogApplicationTabs ${ testCase.name }`, () => {
        const tree = createTree({ params: testCase.params, activeTab: 'catalog' });
        expect(shallowToJson(tree)).toMatchSnapshot();
    });
});

describe('Ссылка Выкупа', () => {
    it('Показывает ссылку, если передан параметр', () => {
        const tree = createTree({ isBuyoutLinkVisible: true });

        const buyoutLink = tree
            .find('.CatalogApplicationTabs__item')
            .filterWhere((wrapper) => wrapper.dive().text() === 'Выкуп');

        expect(buyoutLink.exists()).toBe(true);
    });

    it('Не показывает ссылку, если соответствующий параметр не передан', () => {
        const tree = createTree({});

        const buyoutLink = tree
            .find('.CatalogApplicationTabs__item')
            .filterWhere((wrapper) => wrapper.dive().text() === 'Выкуп');

        expect(buyoutLink.exists()).toBe(false);
    });
});

const createTree = function({ activeTab, isBuyoutLinkVisible, onlyContent, params }) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <CatalogApplicationTabs
                activeTab={ activeTab || '' }
                onlyContent={ onlyContent }
                params={ params }
                isBuyoutLinkVisible={ isBuyoutLinkVisible }
            />
        </ContextProvider>,
    ).dive();
};
