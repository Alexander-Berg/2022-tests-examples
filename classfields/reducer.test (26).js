const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');

const reducer = require('./reducer');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');
const actionTypes = require('./actionTypes');

it('должен сохранить данные по заявкам при загрузке и апдейте страницы', () => {
    const state = {
        foo: 'foo',
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            [ROUTES.matchApplications]: {
                bar: 'bar',
            },
        },
    };

    const expected = {
        foo: 'foo',
        bar: 'bar',
    };

    const newState = reducer(state, action);
    expect(newState).toEqual(expected);
});

it('должен обновлять заявки', () => {
    const state = {
        applicationsList: {
            items: [ 1, 2, 3 ],
            paging: { foo: 123 },
        },
    };

    const payload = {
        items: [ 2, 3, 4, 5 ],
        paging: { bar: 123 },
    };

    const action = {
        type: actionTypes.UPDATE_MATCH_APPLICATIONS,
        payload: payload,
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({
        applicationsList: payload,
    });
});

it('должен обработать кейс апдейта продукта', () => {
    const state = {
        dealerCampaignProducts: {
            items: [
                { isActive: true },
            ],
        },
    };

    const payload = { isActive: true };

    const action = {
        type: actionTypes.UPDATE_DEALER_CAMPAIGN_PRODUCT,
        payload: payload,
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({
        dealerCampaignProducts: {
            items: [
                payload,
            ],
        },
    });
});

it('должен обработать кейс обновления телефонов', () => {
    const state = {
        applicationsPhones: [ '+79998887766' ],
    };

    const payload = [ '+71112223344' ];

    const action = {
        type: actionTypes.UPDATE_APPLICATIONS_PHONES,
        payload: payload,
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({
        applicationsPhones: payload,
    });
});
