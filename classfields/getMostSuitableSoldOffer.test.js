const getMostSuitableSoldOffer = require('./getMostSuitableSoldOffer');

const offerMock = require('autoru-frontend/mockData/responses/offer.mock').offer;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const baseOffer = cloneOfferWithHelpers(offerMock)
    .withStatus('INACTIVE');
const offerWithRecallDate = cloneOfferWithHelpers(offerMock)
    .withStatus('INACTIVE')
    .withCreationDate(1586581933000)
    .withRecallInfo({ recall_timestamp: '1586841133000' });

const TEST_CASE = [
    {
        description: 'Не падает, если нет проданных офферов',
        params: [],
        result: null,
    },
    {
        description: 'Возвращает null, если нет офферов с известным временем продажи',
        params: [ baseOffer ],
        result: null,
    },
    {
        description: 'Если есть всего один оффер с известным временем продажи, вернет его',
        params: [ offerWithRecallDate ],
        result: offerWithRecallDate,
    },
];

TEST_CASE.forEach(({ description, params, result }) => {
    it(description, () => {
        expect(getMostSuitableSoldOffer(params)).toEqual(result);
    });
});
