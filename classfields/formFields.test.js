const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');
const { CHANGE_FORM, FORM_CHECK_RESULT } = require('auto-core/react/dataDomain/formFields/types');
const mockdate = require('mockdate');

const reducer = require('./formFields');

describe('CHANGE_FORM', () => {
    let changeFormAction;
    let state;
    beforeEach(() => {
        changeFormAction = {
            type: CHANGE_FORM,
            payload: {
                fields: { mark: { value: 'AUDI' } },
                clearFields: [ 'model', 'year' ],
            },
        };
        state = reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload: {} });
    });

    it('должен записать значение в стор', () => {
        expect(reducer(state, changeFormAction)).toMatchObject({
            data: {
                mark: { value: 'AUDI', touched: true },
            },
            formCheckTimestamp: 0,
        });
    });

    it('должен удалить значения из стора согласно payload.clearFields и сохранить дефолты', () => {
        // заполняем state данными
        state = reducer(state, {
            type: CHANGE_FORM,
            payload: {
                fields: {
                    mark: { value: 'AUDI' },
                    model: { value: 'A4' },
                    year: { value: '2020' },
                },
            },
        });
        state = reducer(state, changeFormAction);

        expect(state.data.mark).toEqual({ value: 'AUDI', touched: true });
        expect(state.data.model).toEqual({});
        expect(state.data.year).toBeUndefined();
    });
});

describe('FORM_CHECK_RESULT', () => {
    let changeFormAction;
    let state;
    beforeEach(() => {
        mockdate.set('2021-02-02T00:00:00Z');
        changeFormAction = {
            type: FORM_CHECK_RESULT,
            payload: {
                fields: { mark: { value: 'AUDI' } },
                clearFields: [ 'model', 'year' ],
            },
        };
        state = reducer(undefined, { type: PAGE_LOADING_SUCCESS, payload: {} });
    });

    it('должен записать значение в стор и обновить formCheckTimestamp', () => {
        expect(reducer(state, changeFormAction)).toMatchObject({
            data: {
                mark: { value: 'AUDI', touched: true },
            },
            formCheckTimestamp: 1612224000000,
        });
    });
});
