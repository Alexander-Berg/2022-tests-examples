const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const getAllCatalogComplectations = require('auto-core/react/dataDomain/catalogConfigurationsSubtree/selectors/getAllCatalogComplectations').default;
const catalogSubtreeMock = require('auto-core/react/dataDomain/catalogConfigurationsSubtree/mocks/subtree').default;
const cardGroupComplectationsMock = require('auto-core/react/dataDomain/cardGroupComplectations/mocks/complectations').default;

const CardGroupComplectationSelector = require('./CardGroupComplectationSelector');

const complectations = getAllCatalogComplectations({
    catalogConfigurationsSubtree: catalogSubtreeMock,
    cardGroupComplectations: cardGroupComplectationsMock,
});

const searchParameters = {
    catalog_filter: [ {} ],
};

it('CardGroupComplectationSelector должен создавать правильные ссылки', () => {
    const tree = shallow(
        <CardGroupComplectationSelector
            complectations={ complectations }
            activeComplectationId="20913376"
            onSelectComplectation={ jest.fn() }
            searchParameters={ searchParameters }
        />
        ,
        { context: contextMock },
    );

    const item = tree.find('CardGroupComplectationSelectorItem').first();
    expect(item.prop('url')).toBe('link/card-group/?catalog_filter=complectation_name%3DActive');
});
