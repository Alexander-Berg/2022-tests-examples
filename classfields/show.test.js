global.metrika = {
    reachGoal: jest.fn(),
};

const getState = () => ({
    sales: {
        items: [
            {
                id: '3230706',
                hash: '438c4bd5',
                category: 'moto',
            },
        ],
    },
});
const show = require('./show');

it('должен вернуть корректный набор actions', () => {
    const dispatch = jest.fn(() => 'SUCCESS');

    show({ offerID: '3230706-438c4bd5' })(dispatch, getState);
    expect(dispatch.mock.calls).toEqual([
        [ {
            type: 'SHOW_BADGES_SETTINGS',
            payload: {
                saleId: '3230706',
            },
        } ],
    ]);
});
