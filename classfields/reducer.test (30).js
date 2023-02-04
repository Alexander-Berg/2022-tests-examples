const _ = require('lodash');

const reducer = require('./reducer');
const MockDate = require('mockdate');
const actionTypes = require('./actionTypes');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

beforeEach(() => {
    MockDate.set('1988-01-03');
});

it('должен вернуть корректный объект, если есть ' +
    'deactivationAllowed и activatedBy', () => {
    expect(reducer({
        items: [
            {
                saleId: 123,
                service_turbo: {
                    active: false,
                    isFetching: false,
                },
                service_premium: {
                    active: false,
                    isFetching: false,
                },
            },
        ],
    }, {
        type: actionTypes.APPLY_SERVICE_RESOLVED,
        payload: {
            key: 'turbo',
            saleId: 123,
            service: 'turbo',
            active: true,
            deactivationAllowed: true,
            activatedBy: 'owner',
        },
    })).toEqual({
        items: [
            {
                saleId: 123,
                service_turbo: {
                    active: true,
                    isFetching: false,
                    date: '568166400000',
                    deactivationAllowed: true,
                    activatedBy: 'owner',
                },
                service_premium: {
                    active: false,
                    isFetching: false,
                },
            },
        ],
    });
});

it('должен правильно обновить нужный оффер при диспатче экшена CHANGE_BOOKING_ALLOWED', () => {
    const saleId = '16064346';

    const action = {
        type: actionTypes.CHANGE_BOOKING_ALLOWED,
        payload: { saleId, allowed: true },
    };

    const state = {
        items: [
            { additional_info: {}, saleId: '111' },
            {
                additional_info: {
                    booking: {},
                    creation_date: '1591874511000',
                },
                saleId,
            },
            { additional_info: {}, saleId: '333' },
        ],
    };

    const expected = _.cloneDeep(state);
    expected.items[1].additional_info.booking = { allowed: true };

    const newState = reducer(state, action);
    expect(newState).toEqual(expected);
});

it('не должен обновлять items и pagination, если передан resetSales === false', () => {
    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            sales: {
                userOffers: {
                    search_parameters: { resetSales: 'false' },
                    items: [
                        { additional_info: {}, saleId: '111' },
                    ],
                    pagination: {
                        page: 1,
                        page_size: 10,
                        total_offers_count: 1,
                        total_page_count: 1,
                    },
                },
            },
        },
    };

    const state = {
        items: [
            { additional_info: {}, saleId: '444' },
        ],
        pagination: {
            size: 8, current: 1, max: 1, total: 2,
        },
    };

    const newState = reducer(state, action);
    expect(newState.items).toEqual(state.items);
    expect(newState.pagination).toEqual(state.pagination);
});
