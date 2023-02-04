const reducer = require('./reducer');

const actionTypes = require('./actionTypes');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');
const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');

const DEFAULT_BOOKING_CODE = 111;

const bookingList = {
    items: [
        { code: DEFAULT_BOOKING_CODE, status: 'PAID' },
        { code: 222, status: 'PAID' },
    ],
    pagination: {
        page_num: 1,
        total_page_count: 1,
    },
};

it('должен обновлять список заявок на бронирование при диспатче экшена PAGE_LOADING_SUCCESS', () => {
    const state = {};

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            [ROUTES.booking]: { bookingList },
        },
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({ bookingList });
});

it('должен обновлять список заявок на бронирование при диспатче экшена UPDATE_BOOKING_LIST', () => {
    const state = { bookingList };
    const updatedBookingList = { ...bookingList, items: [ { code: 333 } ] };

    const action = {
        type: actionTypes.UPDATE_BOOKING_LIST,
        payload: updatedBookingList,
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({ bookingList: updatedBookingList });
});

it('должен обновлять статус заявки на бронирование при диспатче экшена CHANGE_BOOKING_STATUS', () => {
    const state = { bookingList };
    const newStatus = 'CONFIRMED';
    const expectedList = [
        { code: DEFAULT_BOOKING_CODE, status: newStatus },
        { code: 222, status: 'PAID' },
    ];

    const action = {
        type: actionTypes.CHANGE_BOOKING_STATUS,
        payload: { bookingCode: DEFAULT_BOOKING_CODE, status: newStatus },
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({ bookingList: { ...bookingList, items: expectedList } });
});
