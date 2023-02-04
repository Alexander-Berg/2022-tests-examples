const React = require('react');
const { shallow } = require('enzyme');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const OfferAmpInfo = require('./OfferAmpInfo');

const mockCardInfo = require('./mock');

it('правильно формирует линки', () => {
    const ContextProvider = createContextProvider(contextMock);

    const tree = shallow(
        <ContextProvider>
            <OfferAmpInfo cardInfo={ mockCardInfo }/>
        </ContextProvider>,
    ).dive();
    expect(tree.find('AmpLink')).toMatchSnapshot();
});
