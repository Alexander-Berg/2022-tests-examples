const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const OfferAmpBreadcrumbs = require('./OfferAmpBreadcrumbs');

it('Должен отрендерить хлебные крошки', () => {
    const Context = createContextProvider(contextMock);

    const wrapper = shallow(
        <Context>
            <OfferAmpBreadcrumbs breadcrumbs={ breadcrumbsPublicApiMock } offer={ cardMock }/>
        </Context>,
    ).dive();

    expect(shallowToJson(wrapper.find('.OfferAmpBreadcrumbs'))).toMatchSnapshot();
});
