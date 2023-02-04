const reducer = require('./reducer');

it('PAGE_LOADING_SUCCESS', () => {
    expect(reducer(null, {
        type: 'PAGE_LOADING_SUCCESS',
        payload: {
            clients: {
                clientsListData: {
                    items: [],
                    pagination: {},
                    search_params: {},
                },
                dashboardPresets: {
                    presets: [],
                },
            },
        },
    })).toEqual({
        items: [],
        pagination: {},
        search_params: {},
        presets: [],
    });
});

it('REMOVE_CLIENTS_ITEM', () => {
    expect(reducer({
        items: [ { clientData: { id: 'clientId' } }, { clientData: { id: 'clientId2' } } ],
    }, {
        type: 'REMOVE_CLIENTS_ITEM',
        payload: 'clientId',
    })).toEqual({
        items: [ { clientData: { id: 'clientId2' } } ],
    });
});

it('UPDATE_CLIENTS_ITEM_AUTOPAY', () => {
    expect(reducer({
        items: [ { clientData: { id: 'clientId' } }, { clientData: { id: 'clientId2' } } ],
    }, {
        type: 'UPDATE_CLIENTS_ITEM_AUTOPAY',
        payload: { id: 'clientId', autopay: { minValue: 10, rechargeValue: 10 } },
    })).toEqual(
        {
            items: [ { clientData: { id: 'clientId' }, autopay: { minValue: 10, rechargeValue: 10 } }, { clientData: { id: 'clientId2' } } ],
        },
    );
});
