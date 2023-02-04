import mockdate from 'mockdate';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import dayjs from 'auto-core/dayjs';

import creationDate from './creationDate';

beforeEach(() => {
    mockdate.set('2020-11-10');
});

it('должен вернуть число и месяц, если объявление создано в этом году', () => {
    const offer = cloneOfferWithHelpers(offerMock).withCreationDate(dayjs('2020-11-05').valueOf());
    expect(creationDate(offer.value())).toEqual('5 ноября');
});

it('должен вернуть число, месяц и год, если объявление создано не в этом году', () => {
    const offer = cloneOfferWithHelpers(offerMock).withCreationDate(dayjs('2019-11-05').valueOf());
    expect(creationDate(offer.value())).toEqual('5 ноября 2019');
});
