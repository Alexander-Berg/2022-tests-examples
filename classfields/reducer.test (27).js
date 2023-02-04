const offerReducer = require('./reducer');

const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

it('должен мерджить правильный payload в стейт при диспатче экшена PAGE_LOADING_SUCCESS', () => {
    const state = {
        foo: 'foo',
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            [ROUTES.offer]: {
                metricParams: {
                    fromDate: '2018-02-09',
                    toDate: '2018-02-10',
                },
            },
        },
    };

    const newState = offerReducer(state, action);
    expect(newState).toMatchSnapshot();
});
