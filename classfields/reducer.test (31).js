const settingsReducer = require('./reducer');

const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

it('должен мерджить правильный payload в стейт при диспатче экшена PAGE_LOADING_SUCCESS', () => {
    const state = {
        foo: 'foo',
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            client: {
                result: {
                    salon: {
                        allow_photo_reorder: 1,
                        overdraft_enabled: 'false',
                    },
                },
            },
        },
    };

    const newState = settingsReducer(state, action);
    expect(newState).toMatchSnapshot();
});
