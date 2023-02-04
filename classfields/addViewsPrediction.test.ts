import MockDate from 'mockdate';

import type { OfferStatsDay } from 'auto-core/models/offerStats/types';
import type { PaidService } from 'auto-core/types/proto/auto/api/api_offer_model';
import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import { offerStatsDayMock } from 'auto-core/models/offerStats/mocks';

import addViewsPrediction from './addViewsPrediction';

const today = '2021-04-15';
const services = [
    { service: TOfferVas.PLACEMENT, is_active: true } as PaidService,
    { service: TOfferVas.EXPRESS, is_active: true } as PaidService,
    { service: TOfferVas.COLOR, is_active: true } as PaidService,
    { service: TOfferVas.TOP, is_active: true } as PaidService,
    { service: TOfferVas.SPECIAL, is_active: true } as PaidService,
];
let counters: Array<OfferStatsDay>;

beforeEach(() => {
    MockDate.set(today);

    counters = [
        offerStatsDayMock.withViews(15).withPrice(420000).withDateFromToday(-6).value(),
        offerStatsDayMock.withViews(75).withPrice(420000).withServices(services).withDateFromToday(-5).value(),
        offerStatsDayMock.withViews(35).withPrice(420000).withDateFromToday(-4).value(),
        offerStatsDayMock.withViews(45).withPrice(420000).withDateFromToday(-3).value(),
        offerStatsDayMock.withViews(55).withPrice(450000).withDateFromToday(-2).value(),
        offerStatsDayMock.withViews(42).withPrice(450000).withDateFromToday(-1).value(),
        offerStatsDayMock.withViews(5).withPrice(450000).withDateFromToday(0).value(),
    ];
});

afterEach(() => {
    MockDate.reset();
});

it('оставит в ответе только подключенные сервисы не входящие в пакет', () => {
    const result = addViewsPrediction({ counters });
    const trimmedServices = result[1].services?.map(({ service }) => service);

    expect(trimmedServices).toEqual([ TOfferVas.COLOR, TOfferVas.EXPRESS ]);
});

it('добавит в ответ инфу об изменении цены только для того дня когда она поменялась', () => {
    const result = addViewsPrediction({ counters });
    const daysWithPriceChange = result.filter((day) => day.price_diff);

    expect(daysWithPriceChange).toHaveLength(1);
    expect(daysWithPriceChange[0].price_diff).toBe(30000);
});

it('добавит в ответ предикт для последующих дней', () => {
    const result = addViewsPrediction({ counters });
    const daysWithPredict = result.filter((day) => day.predict);

    expect(daysWithPredict).toHaveLength(4);
    expect(daysWithPredict[0].date).toBe('2021-04-16');
    expect(daysWithPredict[3].date).toBe('2021-04-19');
});

it('для сегодняшнего дня добавит флаг про промо', () => {
    const result = addViewsPrediction({ counters });
    const todaysIndex = result.findIndex(({ date }) => date === today);

    expect(result[todaysIndex - 1].has_promo).toBe(false);
    expect(result[todaysIndex].has_promo).toBe(true);
    expect(result[todaysIndex + 1].has_promo).toBe(false);
});
