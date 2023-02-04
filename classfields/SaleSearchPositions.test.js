const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const offerNewMock = require('autoru-frontend/mockData/state/newCard.mock');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const SaleSearchPositions = require('./SaleSearchPositions');

const searchPositionsMock = [ {
    total_count: 188,
    positions: [
        { position: 100, sort: 'RELEVANCE' },
        { position: 5, sort: 'PRICE' },
    ],
} ];

it('должен правильно формировать ссылки на выдачу для б/у оффера', () => {
    const offer = _.cloneDeep(offerMock);
    offer.additional_info.search_positions = searchPositionsMock;

    const tree = shallow(
        <SaleSearchPositions offer={ offer }/>,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен правильно формировать ссылки на выдачу для нового оффера', () => {
    const offer = _.cloneDeep(offerNewMock.card);
    offer.additional_info.search_positions = searchPositionsMock;

    const tree = shallow(
        <SaleSearchPositions offer={ offer }/>,
        { context: contextMock },
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});
