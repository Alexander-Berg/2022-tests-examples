import reducer from './reducer';
import actionTypes from './actionTypes';

it('должен создать оффер и обновить поле isPriceChanging', () => {
    expect(reducer({}, {
        type: actionTypes.CMEXPERT_WIDGET_UPDATE,
        payload: {
            offerID: '1099645682-fc3d3a6d',
            isPriceChanging: true,
        },
    })).toEqual({
        '1099645682-fc3d3a6d': {
            offerID: '1099645682-fc3d3a6d',
            isPriceChanging: true,
        },
    });
});

it('должен обновить поле isPriceChanging, если уже есть в сторе', () => {
    expect(reducer({
        '1099645682-fc3d3a6d': {
            offerID: '1099645682-fc3d3a6d',
            price: 2000,
            isPriceChanging: false,
        },
    }, {
        type: actionTypes.CMEXPERT_WIDGET_UPDATE,
        payload: {
            offerID: '1099645682-fc3d3a6d',
            isPriceChanging: true,
        },
    })).toEqual({
        '1099645682-fc3d3a6d': {
            offerID: '1099645682-fc3d3a6d',
            price: 2000,
            isPriceChanging: true,
        },
    });
});

it('Должен обновить цену', () => {
    expect(reducer({
        '1099645682-fc3d3a6d': {
            offerID: '1099645682-fc3d3a6d',
            price: 100500,
            isPriceChanging: false,
        },
    }, {
        type: 'UPDATE_PRICE',
        payload: {
            offerID: '1099645682-fc3d3a6d',
            price: 100400,
        },
    })).toEqual({
        '1099645682-fc3d3a6d': {
            offerID: '1099645682-fc3d3a6d',
            price: 100400,
        },
    });
});
