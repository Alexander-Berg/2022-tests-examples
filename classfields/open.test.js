const mockStore = require('autoru-frontend/mocks/mockStore').default;
const initialState = require('../mocks/initialState');
const openParamsMock = require('../mocks/openParams');

const open = require('./open');

it('должен вызвать action октрытия автостратегии и экшены изменения формы автостратегии', () => {
    const store = mockStore(initialState);
    store.dispatch(open(openParamsMock));
    expect(store.getActions()).toMatchSnapshot();
});
