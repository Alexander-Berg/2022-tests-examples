const reducer = require('./reducer');
const counts = require('./mock/counts');
const tariffs = require('./mock/tariffs');
const emptyTariffs = require('./mock/emptyTariffs');

const actionTypes = require('./actionTypes');

describe('должен обновлять счетчики', () => {
    const state = {
        tariffsCounts: counts,
    };

    it('штучные размещения (single placements)', () => {
        const updatingCounts = {
            '0-300000': 0,
            '300000-500000': 0,
            '500000-800000': 1,
            '800000-1500000': 0,
            '1500000': 0,
        };

        const action = {
            type: actionTypes.UPDATE_PLACEMENTS_COUNTS,
            payload: {
                categoryHash: 'CARS_USED',
                value: updatingCounts,
            },
        };

        const newState = reducer(state, action);
        const carsUsedCounts = newState.tariffsCounts.CARS_USED.placements;

        expect(carsUsedCounts).toEqual(updatingCounts);
    });

    it('количество звонков в день', () => {
        const updatingCounts = 3;

        const action = {
            type: actionTypes.UPDATE_PLACEMENTS_COUNTS,
            payload: {
                categoryHash: 'CARS_NEW',
                value: updatingCounts,
            },
        };

        const newState = reducer(state, action);
        const carsUsedCounts = newState.tariffsCounts.CARS_NEW.placements;

        expect(carsUsedCounts).toEqual(updatingCounts);
    });

    it('количество применяемых услуг в день', () => {
        const updatingCounts = {
            boost: 0,
            special: 2,
            premium: 2,
            badges: 0,
        };

        const action = {
            type: actionTypes.UPDATE_SERVICES_COUNTS,
            payload: {
                categoryHash: 'TRUCKS_LCV_USED',
                value: updatingCounts,
            },
        };

        const newState = reducer(state, action);
        const carsUsedCounts = newState.tariffsCounts.TRUCKS_LCV_USED.services;

        expect(carsUsedCounts).toEqual(updatingCounts);
    });
});

describe('должен обновлять параметры тарифа', () => {
    const state = {
        tariffs: tariffs,
    };

    it('отключение и включение тарифа', () => {
        expect(state.tariffs.CARS_USED.enabled).toEqual(true);

        const disablingAction = {
            type: actionTypes.REMOVE_TARIFF,
            payload: {
                categoryHash: 'CARS_USED',
            },
        };

        const newState1 = reducer(state, disablingAction);

        expect(newState1.tariffs.CARS_USED.enabled).toEqual(false);

        const enablingAction = {
            type: actionTypes.ADD_TARIFF,
            payload: {
                categoryHash: 'CARS_USED',
            },
        };

        const newState2 = reducer(state, enablingAction);

        expect(newState2.tariffs.CARS_USED.enabled).toEqual(true);
    });

    it('подключение квоты', () => {
        const newQuotaList = [
            {
                size: 10,
                price: 10000,
                enabled: false,
            },
            {
                size: 25,
                price: 10000,
                enabled: false,
            },
            {
                size: 50,
                price: 15000,
                enabled: false,
            },
            {
                size: 75,
                price: 15000,
                enabled: false,
            },
            {
                size: 100,
                price: 15000,
                enabled: true,
            },
        ];

        const action = {
            type: actionTypes.UPDATE_QUOTA_TARIFF,
            payload: {
                categoryHash: 'MOTO',
                quotaList: newQuotaList,
            },
        };

        const newState = reducer(state, action);
        expect(newState.tariffs.MOTO.quotas).toEqual(newQuotaList);
    });

    it('суточный лимит на звонки', () => {
        const newLimits = {
            coming_daily: { funds: 1000 },
            current_daily: { funds: 2000 },
        };

        const action = {
            type: actionTypes.UPDATE_CALL_LIMIT,
            payload: {
                categoryHash: 'CARS_NEW',
                limits: newLimits,
            },
        };

        const newState = reducer(state, action);

        expect(newState.tariffs.CARS_NEW.calls.limits).toEqual(newLimits);
    });
});

it('должен обновлять период расчета', () => {
    const state = {
        summaryPeriod: 30,
    };

    const newPeriod = 60;

    const action = {
        type: actionTypes.UPDATE_PERIOD,
        payload: newPeriod,
    };

    const newState = reducer(state, action);

    expect(newState.summaryPeriod).toEqual(newPeriod);
});

it('должен обновлять тарифы', () => {
    const state = {
        tariffs: tariffs,
    };

    const action = {
        type: actionTypes.UPDATE_TARIFFS,
        payload: emptyTariffs,
    };

    const newState = reducer(state, action);
    expect(newState.tariffs).toEqual(emptyTariffs);
});
