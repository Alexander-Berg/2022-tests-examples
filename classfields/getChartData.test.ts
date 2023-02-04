jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.resolve({ items: [ { counters: [] } ] })),
    };
});
import MockDate from 'mockdate';

import gateApi from 'auto-core/react/lib/gateApi';

import dayjs from 'auto-core/dayjs';

import type { CabinetOffer } from 'www-cabinet/react/types';

import getChartData from './getChartData';
import daily_counters from './mocks/daily_counters.mock';
import classifieds from './mocks/classifieds.mock';

it('должен вернуть daily_counter, если объявление было создано больше 30 дней назад', async() => {
    MockDate.set('2022-06-14');
    const offer = {
        daily_counters,
        price_history: [],
        additional_info: {
            creation_date: dayjs().add(-30, 'day').valueOf(),
        },
    } as any as CabinetOffer;

    const data = await getChartData(offer, classifieds, true);
    expect(data?.chartData).toMatchSnapshot();
});

it('должен вернуть daily_counter, если объявление было создано меньше 8 дней назад', async() => {
    MockDate.set('2022-06-14');
    const offer = {
        daily_counters,
        price_history: [],
        additional_info: {
            creation_date: dayjs().add(-4, 'day').valueOf(),
        },
    } as any as CabinetOffer;
    const data = await getChartData(offer, classifieds, true);
    expect(data?.chartData).toHaveLength(8);
});

it('должен вернуть daily_counter, если объявление было создано 15 дней назад', async() => {
    MockDate.set('2022-06-14');
    const offer = {
        daily_counters,
        price_history: [],
        additional_info: {
            creation_date: dayjs().add(-15, 'day').valueOf(),
        },
    } as any as CabinetOffer;

    const data = await getChartData(offer, classifieds, true);
    expect(data?.chartData).toHaveLength(16);
    expect(data?.chartData[15]?.date).toBe('2022-06-14');
});

it('должен сходить за daily_counter в ручку getUserOfferStats, если объявление снято с продажи', async() => {
    MockDate.set('2022-06-14');
    const offer = {
        daily_counters,
        price_history: [],
        additional_info: {
            creation_date: dayjs().add(-15, 'day').valueOf(),
        },
        recall_info: {
            recall_timestamp: dayjs().add(-100, 'day'),
        },
    } as any as CabinetOffer;

    await getChartData(offer, classifieds, true);
    expect(gateApi.getResource).toHaveBeenCalled();
});

it('должен сходить за daily_counter в ручку getUserOfferStats, если объявление удалено', async() => {
    MockDate.set('2022-06-14');
    const offer = {
        daily_counters,
        price_history: [],
        additional_info: {
            creation_date: dayjs().add(-15, 'day').valueOf(),
        },
        removed: dayjs().add(-100, 'day'),
    } as any as CabinetOffer;

    await getChartData(offer, classifieds, true);
    expect(gateApi.getResource).toHaveBeenCalled();
});
