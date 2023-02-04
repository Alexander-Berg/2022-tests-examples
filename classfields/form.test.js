const { changeForm } = require('./form');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const initialState = { formFields: { data: {} } };

it('отправит CHANGE_FORM с отформатированными данными и распарсит json в phones, если поле валидное', () => {
    const store = mockStore(initialState);
    store.dispatch(changeForm({ field1: 'value1', phones: '[{"phone":"79999999999","call_from":"9","call_till":"21"}]' }));
    expect(store.getActions()[0]).toEqual({
        type: 'CHANGE_FORM',
        payload: {
            field1: { value: 'value1' },
            phones: { value: [
                {
                    phone: '79999999999',
                    call_from: '9',
                    call_till: '21',
                },
            ] },
        },
    });
});

it('отправит CHANGE_FORM с отформатированными данными и phones=[], если есть поле phones с ошибкой в json', () => {
    const store = mockStore(initialState);
    store.dispatch(changeForm({ field1: 'value1', phones: '[{"phone":"79999999999","call_from":"9","call_till":"21"' }));
    expect(store.getActions()[0]).toEqual({
        type: 'CHANGE_FORM',
        payload: {
            field1: { value: 'value1' },
            phones: { value: [] },
        },
    });
});

it('отправит CHANGE_FORM с отформатированными данными и phones=[], если есть поле phones не json', () => {
    const store = mockStore(initialState);
    store.dispatch(changeForm({ field1: 'value1', phones: [ { phone: '123' } ] }));
    expect(store.getActions()[0]).toEqual({
        type: 'CHANGE_FORM',
        payload: {
            field1: { value: 'value1' },
            phones: { value: [] },
        },
    });
});
