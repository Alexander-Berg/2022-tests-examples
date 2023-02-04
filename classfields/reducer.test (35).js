const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');
const { PAGE_LOADING_SUCCESS } = require('auto-core/react/actionTypes');

const walletReducer = require('./reducer');

it(`должен мерджить правильный payload в стейт при диспатче экшена PAGE_LOADING_SUCCESS`, () => {
    const state = {
        isLoading: false,
    };

    const action = {
        type: PAGE_LOADING_SUCCESS,
        payload: {
            [ROUTES.wallet]: {
                historyData: {
                    items: [ 1 ],
                },
                totalStats: [ 2 ],
                balanceStats: [ 3 ],
                walletParams: {
                    from: '2011-01-02',
                    to: '2011-01-03',
                    pageType: 'recharges',
                    viewType: 'offers',
                    products: [ 'placement' ],
                    historyType: 'offers',
                },
            },
        },
    };

    const newState = walletReducer(state, action);
    expect(newState).toMatchSnapshot();
});
