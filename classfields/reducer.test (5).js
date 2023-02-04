const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');
const configReducer = require('./reducer');

it('should return same state for uknown event', () => {
    const state = {};
    const nextState = configReducer(state, { type: '@UNKNOWN_ACTION' });
    expect(nextState).toEqual(state);
});

it('should update config on PAGE_LOADING_SUCCESS', () => {
    const state = {
        isFetching: true,
        data: {
            test: 1,
            crc: 2,
        },
    };
    const nextState = configReducer(state, {
        type: PAGE_LOADING_SUCCESS,
        data: {
            config: {
                crc: 2,
            },
        },
    });
    expect(nextState).toEqual({
        isFetching: false,
        data: {
            test: 1,
            crc: 2,
        },
    });
});
