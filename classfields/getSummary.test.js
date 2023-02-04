const counts = require('../mock/counts');
const tariffs = require('../mock/tariffs');
const emptyTariffs = require('../mock/emptyTariffs');

const getSummary = require('./getSummary');
const reducer = require('../reducer');
const actionTypes = require('../actionTypes');

it('должен корректно считать сумму для существующих размещений и услуг (начальную сумму)', () => {
    const state = {
        calculator: {
            tariffsCounts: counts,
            tariffs: tariffs,
            summaryPeriod: 1,
        },
    };

    const summary = getSummary(state);
    expect(summary).toMatchSnapshot();
});

it('должен корректно считать сумму без услуг и размещений', () => {
    const state = {
        calculator: {
            tariffsCounts: counts,
            tariffs: emptyTariffs,
            summaryPeriod: 1,
        },
    };

    const summary = getSummary(state);
    expect(summary).toMatchSnapshot();
});

describe('должен корректно считать сумму размещений для разных типов категорий', () => {
    const state = {
        calculator: {
            tariffsCounts: counts,
            tariffs: emptyTariffs,
            summaryPeriod: 1,
        },
    };

    it('должен пересчитывать суммы после изменения штучных размещений', () => {
        const updatingCounts = {
            '0-300000': 0,
            '300000-500000': 0,
            '500000-800000': 1,
            '800000-1500000': 1,
            '1500000': 0,
        };

        const action = {
            type: actionTypes.UPDATE_PLACEMENTS_COUNTS,
            payload: {
                categoryHash: 'TRUCKS_LCV_NEW',
                value: updatingCounts,
            },
        };

        const newSummary = getSummary({ calculator: reducer(state.calculator, action) });

        expect(newSummary.placements.TRUCKS_LCV_NEW).toBe(200);
    });

    it('должен пересчитывать сумму после изменения размещений в легковых новых', () => {
        const action = {
            type: actionTypes.UPDATE_PLACEMENTS_COUNTS,
            payload: {
                categoryHash: 'CARS_NEW',
                value: 1,
            },
        };

        const newSummary = getSummary({ calculator: reducer(state.calculator, action) });

        expect(newSummary.placements.CARS_NEW).toBe(400);
    });

    it('должен пересчитывать сумму после изменения квоты', () => {
        const newQuotaList = state.calculator.tariffs.MOTO.quotas
            .map((item, index) => {
                if (index === 0) {
                    return { ...item, enabled: false };

                } else if (index === 1) {
                    return { ...item, enabled: true };

                } else {
                    return item;
                }
            });

        const action = {
            type: actionTypes.UPDATE_QUOTA_TARIFF,
            payload: {
                categoryHash: 'MOTO',
                quotaList: newQuotaList,
            },
        };

        const newState = { calculator: reducer(state.calculator, action) };
        const newSummary = getSummary(newState);

        expect(newSummary.placements.MOTO).toBe(175);
    });

    it('должен изменять сумму для размещений при изменении периода', () => {
        const nonEmptyState = {
            calculator: {
                tariffs: tariffs,
                tariffsCounts: counts,
                summaryPeriod: 1,
            },
        };

        const initialSum = 260;

        const periodAction = {
            type: actionTypes.UPDATE_PERIOD,
            payload: 3,
        };

        const newState = {
            ...state,
            calculator: reducer(nonEmptyState.calculator, periodAction),
        };

        const newSummary = getSummary(newState);

        expect(newSummary.placementsTotal).toBe(initialSum * 3);
    });
});

describe('должен корректно считать сумму при изменении количества услуг', () => {
    const state = {
        calculator: {
            tariffsCounts: counts,
            tariffs: emptyTariffs,
            summaryPeriod: 1,
        },
    };

    it('должен пересчитывать сумму после изменения разовых услуг', () => {
        const countsAction = {
            type: actionTypes.UPDATE_SERVICES_COUNTS,
            payload: {
                categoryHash: 'CARS_USED',
                value: {
                    boost: 3,
                    'special-offer': 0,
                    premium: 0,
                    badge: 0,
                    'turbo-package': 1,
                },
            },
        };

        const newSummary = getSummary({ calculator: reducer(state.calculator, countsAction) });

        expect(newSummary.services.CARS_USED).toBe(4850);
    });

    it('не должен изменять сумму для разовых услуг при изменении периода', () => {
        const countsAction = {
            type: actionTypes.UPDATE_SERVICES_COUNTS,
            payload: {
                categoryHash: 'CARS_USED',
                value: {
                    boost: 3,
                    'special-offer': 0,
                    premium: 0,
                    badge: 0,
                    'turbo-package': 1,
                },
            },
        };

        const oldState = {
            ...state,
            calculator: reducer(state.calculator, countsAction),
        };

        const periodAction = {
            type: actionTypes.UPDATE_PERIOD,
            payload: 3,
        };

        const newState = {
            ...state,
            calculator: reducer(oldState.calculator, periodAction),
        };

        const newSummary = getSummary(newState);

        expect(newSummary.services.CARS_USED).toBe(4850);
    });

    it('должен изменять сумму для услуг при изменении периода', () => {
        const countsAction = {
            type: actionTypes.UPDATE_SERVICES_COUNTS,
            payload: {
                categoryHash: 'CARS_USED',
                value: {
                    boost: 0,
                    'special-offer': 3,
                    premium: 0,
                    badge: 0,
                    'turbo-package': 0,
                },
            },
        };

        const oldState = {
            ...state,
            calculator: reducer(state.calculator, countsAction),
        };

        const initialSum = 360;

        const periodAction = {
            type: actionTypes.UPDATE_PERIOD,
            payload: 3,
        };

        const newState = {
            ...state,
            calculator: reducer(oldState.calculator, periodAction),
        };

        const newSummary = getSummary(newState);

        expect(newSummary.services.CARS_USED).toBe(initialSum * 3);
    });
});

describe('не должен учитывать категории, если они не выбраны (выключены)', () => {
    const state = {
        calculator: {
            tariffsCounts: counts,
            tariffs: emptyTariffs,
            summaryPeriod: 1,
        },
    };

    it('должен уменьшать сумму после отключения категории', () => {
        const action = {
            type: actionTypes.UPDATE_PLACEMENTS_COUNTS,
            payload: {
                categoryHash: 'CARS_NEW',
                value: 1,
            },
        };

        const oldState = { calculator: reducer(state.calculator, action) };

        const disablingAction = {
            type: actionTypes.REMOVE_TARIFF,
            payload: {
                categoryHash: 'CARS_NEW',
            },
        };

        const newSummary = getSummary({ calculator: reducer(oldState.calculator, disablingAction) });
        expect(newSummary.placements.CARS_NEW).toBe(0);
    });
});
