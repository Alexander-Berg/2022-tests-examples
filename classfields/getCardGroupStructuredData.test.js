const _ = require('lodash');

const getCardGroupStructuredData = require('./getCardGroupStructuredData');
const listingMock = require('autoru-frontend/mockData/state/listing');
const cardGroupComplectations = require('autoru-frontend/mockData/state/cardGroupComplectations.mock.js');
const offer = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const MockDate = require('mockdate');

describe('Разметка для групп карточек', () => {
    beforeEach(() => {
        MockDate.set('2021-12-01T21:00');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('Должен вернуть разметку', () => {
        const state = {
            ..._.cloneDeep(cardGroupComplectations),
            listing: {
                data: {
                    ...listingMock.data,
                    offers: [ {
                        ...offer,
                        additional_info: {
                            creation_date: '1553183449000',
                        },
                    } ],
                },
            },
            averageRating: {
                reviewsCount: 123,
                ratings: [
                    {
                        name: 'total',
                        value: 123.456,
                    },
                ],
            },
        };

        expect(getCardGroupStructuredData(state)).toMatchSnapshot();
    });

});
