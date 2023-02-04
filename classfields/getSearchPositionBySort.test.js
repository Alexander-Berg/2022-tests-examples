const _ = require('lodash');

const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const getSearchPositionBySort = require('./getSearchPositionBySort');

const searchPositionsMock = [ {
    positions: [
        { position: 100, sort: 'RELEVANCE' },
        { position: 5, sort: 'PRICE' },
    ],
} ];

it('должен селектить позицию в поиске', () => {
    const offer = _.cloneDeep(offerMock);
    offer.additional_info.search_positions = searchPositionsMock;

    expect(getSearchPositionBySort(offer, 'PRICE')).toBe(5);
});
