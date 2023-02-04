const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CarouselNewForTradeInItem = require('./CarouselNewForTradeInItem');

it('должен передать параметры визита при клике', () => {
    const tree = shallow(
        <CarouselNewForTradeInItem
            offer={{ category: 'cars', id: 1 }}
            metrikaClick="item-click"
        />,
        { context: contextMock },
    );
    tree.find('Link').dive().find('a').simulate('click');
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'item-click' ]);
});
