const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');

const reducer = require('./reducer');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

const actionTypes = require('./actionTypes');

it('должен сохранить данные по звонкам при загрузке и апдейте страницы', () => {
    const state = {
        foo: 'foo',
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            [ROUTES.calls]: {
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

it('должен обновлять звонки', () => {
    const state = {
        callsList: {
            calls: [ 1, 2, 3 ],
            pagination: { foo: 123 },
        },
    };

    const payload = {
        calls: [ 2, 3, 4, 5 ],
        pagination: { bar: 123 },
    };

    const action = {
        type: actionTypes.UPDATE_CALLS,
        payload: payload,
    };

    const newState = reducer(state, action);
    expect(newState).toEqual({
        callsList: payload,
    });
});

it('должен обновлять стейт плеера при экшене play', () => {
    const state = {
        foo: {},
        callsPlayer: {},
    };

    const payload = { call_id: '123' };

    const action = {
        type: actionTypes.CALL_RECORD_PLAY,
        payload: payload,
    };

    const newState = reducer(state, action);

    expect(newState).toEqual({
        foo: {},
        callsPlayer: {
            data: payload,
            isPlaying: true,
            isVisible: true,
        },
    });
});

it('должен обновлять стейт плеера при экшене pause', () => {
    const data = { call_id: '123' };

    const state = {
        foo: {},
        callsPlayer: {
            data: data,
            isPlaying: true,
            isVisible: true,
        },
    };

    const action = { type: actionTypes.CALL_RECORD_PAUSE };

    const newState = reducer(state, action);

    expect(newState).toEqual({
        foo: {},
        callsPlayer: {
            data: data,
            isPlaying: false,
            isVisible: true,
        },
    });
});

it('должен очищать стейт плеера при экшене hide', () => {
    const data = { call_id: '123' };

    const state = {
        foo: {},
        callsPlayer: {
            data: data,
            isPlaying: true,
            isVisible: true,
        },
    };

    const action = { type: actionTypes.HIDE_PLAYER };

    const newState = reducer(state, action);

    expect(newState).toEqual({
        foo: {},
        callsPlayer: {},
    });
});
