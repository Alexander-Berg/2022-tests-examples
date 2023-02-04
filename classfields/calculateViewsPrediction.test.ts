jest.mock('auto-core/react/lib/randomIntFromInterval');

import getRandomInt from 'auto-core/react/lib/randomIntFromInterval';
import isNotEmpty from 'auto-core/react/lib/isNotEmpty';

import { createDaysMockFromViewsArray, offerStatsDayMock } from 'auto-core/models/offerStats/mocks';
import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import type { OfferStatsDay } from 'auto-core/models/offerStats/types';

import preparer, { FADING, PREDICT } from './calculateViewsPrediction';

const getRandomIntMock = getRandomInt as jest.MockedFunction<typeof getRandomInt>;
getRandomIntMock.mockReturnValue(1);

const daysMock = createDaysMockFromViewsArray([ 45, 98, 67, 55, 42, 39, 33 ]);

const getPredictForService = (counters: Array<OfferStatsDay>, serviceId: TOfferVas) =>
    preparer(counters)
        .map(({ predict }) => predict)
        .filter(isNotEmpty)
        .map((predict) => predict[serviceId]);

it('добавит затухание для обычного сервиса, если достаточно данных', () => {
    const [ first, second, third, fourth ] = getPredictForService(daysMock, TOfferVas.TURBO);

    expect(first).toBe(160);
    expect(second).toBe(Math.round(first * FADING));
    expect(third).toBe(Math.round(second * FADING));
    expect(fourth).toBe(Math.round(third * FADING));
});

it('для випа будут рандомные значения в интервале 95% - 105% от первого дня', () => {
    getRandomIntMock
        .mockReturnValueOnce(97)
        .mockReturnValueOnce(102)
        .mockReturnValueOnce(99)
        .mockReturnValue(1);
    const [ first, second, third, fourth ] = getPredictForService(daysMock, TOfferVas.VIP);

    expect(getRandomIntMock).toHaveBeenCalledTimes(3);
    expect(getRandomIntMock).toHaveBeenNthCalledWith(1, 95, 105);
    expect(first).toBe(186);
    expect(second).toBe(180);
    expect(third).toBe(189);
    expect(fourth).toBe(184);
});

it('будет учитывать сегодня если кол-во просмотров уже больше чем в предыдущие дни', () => {
    const days = createDaysMockFromViewsArray([ 12, 45, 98, 100 ]);
    const predict = getPredictForService(days, TOfferVas.TURBO);
    expect(predict).toHaveLength(4);
});

it('не будет учитывать сегодня если кол-во просмотров меньше чем в предыдущие дни', () => {
    const days = createDaysMockFromViewsArray([ 12, 45, 98, 30 ]);
    const predict = getPredictForService(days, TOfferVas.TURBO);
    expect(predict).toHaveLength(0);
});

it('ограничивает прогноз сверху', () => {
    const days = createDaysMockFromViewsArray([ 67, 1, 55, 54, 1 ]);
    const [ first ] = getPredictForService(days, TOfferVas.TURBO);
    expect(first).toBeLessThanOrEqual(Math.round(1.3 * 55 * PREDICT['package_turbo']));
});

it('если в массиве нет сегодняшнего дня, просто вернет исходный массив', () => {
    const counters = [
        offerStatsDayMock.withViews(11).value(),
        offerStatsDayMock.withViews(42).value(),
        offerStatsDayMock.withViews(35).value(),
        offerStatsDayMock.withViews(33).value(),
        offerStatsDayMock.withViews(23).value(),
    ];
    const result = preparer(counters);
    expect(result).toBe(counters);
});

it('если недостаточно данных, просто вернет исходный массив', () => {
    const counters = createDaysMockFromViewsArray([ 0, 0, 67, 55 ]);
    const result = preparer(counters);
    expect(result).toBe(counters);
});
