const deliverySettings = require('./reducer');

it('должен вернуть корректный объект, если action === {}', () => {
    expect(deliverySettings(undefined, {})).toEqual({
        offerIDs: [],
        isVisible: false,
        regions: [],
        addressNeeded: true,
        confirmAction: undefined,
        isShowingConfirm: false,
        isShowingLoader: false,
        suggestError: '',
    });
});

it('должен вернуть корректный объект, если action.type === SHOW_DELIVERY_SETTINGS_LOADER', () => {
    expect(deliverySettings({
        isShowingLoader: false,
    }, {
        type: 'SHOW_DELIVERY_SETTINGS_LOADER',
    })).toEqual({
        isShowingLoader: true,
    });
});

it('должен вернуть корректный объект, если action.type === HIDE_DELIVERY_SETTINGS_LOADER', () => {
    expect(deliverySettings({
        isShowingLoader: true,
    }, {
        type: 'HIDE_DELIVERY_SETTINGS_LOADER',
    })).toEqual({
        isShowingLoader: false,
    });
});

it('должен вернуть корректный объект, если action.type === SHOW_DELIVERY_SETTINGS', () => {
    expect(deliverySettings({
        isVisible: false,
    }, {
        type: 'SHOW_DELIVERY_SETTINGS',
        payload: {
            offerIDs: [ '123' ],
            addressNeeded: true,
        },
    })).toEqual({
        offerIDs: [ '123' ],
        isVisible: true,
        addressNeeded: true,
    });
});

it('должен вернуть корректный объект, если action.type === HIDE_DELIVERY_SETTINGS', () => {
    expect(deliverySettings({
        offerIDs: [ '123' ],
        isVisible: false,
        addressNeeded: true,
        isShowingConfirm: false,
        confirmAction: undefined,
    }, {
        type: 'HIDE_DELIVERY_SETTINGS',
    })).toEqual({
        offerIDs: [],
        isVisible: false,
        addressNeeded: true,
        isShowingConfirm: false,
        confirmAction: undefined,
    });
});

describe('должен вернуть корректный объект, если actions.type === RESET_DELIVERY_SETTINGS_REGIONS', () => {
    it('и !action.payload', () => {
        expect(deliverySettings({
            regions: [ 1, 2, 3 ],
        }, {
            type: 'RESET_DELIVERY_SETTINGS_REGIONS',
        })).toEqual({
            regions: [],
        });
    });

    it('и action.payload', () => {
        expect(deliverySettings({
            regions: [ 1, 2, 3 ],
        }, {
            type: 'RESET_DELIVERY_SETTINGS_REGIONS',
            payload: [ 3, 2, 1 ],
        })).toEqual({
            regions: [ 3, 2, 1 ],
        });
    });
});

it('должен вернуть корректный объект, если action.type === ADD_DELIVERY_SETTINGS_REGION', () => {
    expect(deliverySettings({
        regions: [ 1, 2, 3 ],
    }, {
        type: 'ADD_DELIVERY_SETTINGS_REGION',
        payload: 'newRegion'
        ,
    })).toEqual({
        regions: [ 'newRegion', 1, 2, 3 ],
    });
});

it('должен вернуть корректный объект, если action.type === UPDATE_DELIVERY_SETTINGS_REGION_BY_COORD', () => {
    expect(deliverySettings({
        regions: [
            { coord: { latitude: 1, longitude: 1 } },
            { coord: { latitude: 2, longitude: 2 } },
            { coord: { latitude: 3, longitude: 3 } },
        ],
    }, {
        type: 'UPDATE_DELIVERY_SETTINGS_REGION_BY_COORD',
        payload: {
            coord: { latitude: 2, longitude: 2 },
            fields: { deleted: true },
        }
        ,
    })).toEqual({
        regions: [
            { coord: { latitude: 1, longitude: 1 } },
            { coord: { latitude: 2, longitude: 2 }, deleted: true },
            { coord: { latitude: 3, longitude: 3 } },
        ],
    });
});

it('должен вернуть корректный объект, если action.type === SHOW_DELIVERY_SETTINGS_CONFIRM', () => {
    expect(deliverySettings({
        isShowingConfirm: false,
        confirmAction: undefined,
    }, {
        type: 'SHOW_DELIVERY_SETTINGS_CONFIRM',
        payload: 'delete',
    })).toEqual({
        isShowingConfirm: true,
        confirmAction: 'delete',
    });
});

it('должен вернуть корректный объект, если action.type === HIDE_DELIVERY_SETTINGS_CONFIRM', () => {
    expect(deliverySettings({
        isShowingConfirm: true,
        confirmAction: 'delete',
    }, {
        type: 'HIDE_DELIVERY_SETTINGS_CONFIRM',
        payload: 'delete',
    })).toEqual({
        isShowingConfirm: false,
        confirmAction: undefined,
    });
});
