const _ = require('lodash');

const hasVinReportButton = require('./hasVinReportButton').default;
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

it('hasVinReportButton возвратит true, если есть данные о наличии отчёта, машина не забронирована и объявление активно', () => {
    const offer = cardMock;
    expect(hasVinReportButton(offer)).toBe(true);
});

it('hasVinReportButton возвратит true, если нет данных о наличии отчёта', () => {
    const offer = _.cloneDeep(cardMock);
    delete offer.documents.vin_resolution;
    expect(hasVinReportButton(offer)).toBe(false);
});

it('hasVinReportButton возвратит false, если есть данные о наличии отчёта, но статус INVALID', () => {
    const offer = _.cloneDeep(cardMock);
    offer.documents.vin_resolution = 'INVALID';
    expect(hasVinReportButton(offer)).toBe(false);
});

it('hasVinReportButton возвратит false, если оффер не активен', () => {
    const offer = _.cloneDeep(cardMock);
    offer.status = 'INACTIVE';
    expect(hasVinReportButton(offer)).toBe(false);
});
