const React = require('react');
const { shallow } = require('enzyme');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CardInfo = require('./CardInfo');

const mockCardInfo = require('./mock');

it('правильно формирует линки', () => {
    const ContextProvider = createContextProvider(contextMock);

    const tree = shallow(
        <ContextProvider>
            <CardInfo cardInfo={ mockCardInfo }/>
        </ContextProvider>,
    ).dive();
    // Чтобы проверить наличие линков, сначала ищем все компоненты, где они формируются. А потом внутри каждого ищем Link
    // В конце фильтруем, чтобы оставить только те, которые существуют
    const links = tree.find('CardInfoRow')
        .map(component => component.dive().find('Link'))
        .filter(component => !component.isEmptyRender());
    expect(links).toMatchSnapshot();
});
