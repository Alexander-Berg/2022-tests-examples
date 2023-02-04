jest.mock('www-poffer/react_bem/lib/metrika', () => ({
    sendParams: jest.fn(),
}));

import mockStore from 'autoru-frontend/mocks/mockStore';

import configMock from 'auto-core/react/dataDomain/config/mock';
import userMock from 'auto-core/react/dataDomain/user/mocks';

import metrika from 'www-poffer/react_bem/lib/metrika';

import type { AppState } from './log';
import log from './log';

let initialState: AppState;

beforeEach(() => {
    initialState = {
        config: configMock.withPageParams({ category: 'cars', form_type: 'add' }).value(),
        user: userMock.value(),
    };
});

describe('короткая форма', () => {

    it('залогирует поле', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'mark', value: 'lada', isValid: true } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'short_form', 'mark' ]);
    });

    it('залогирует автозаполненное поле', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'year', value: '2012', isValid: true, isAutoFilled: true } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'short_form', 'year', 'autofill' ]);
    });
});

describe('длинная форма', () => {
    it('залогирует обязательное поле', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'price', value: 42, isValid: false } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'long_form', 'required', 'price', 'error' ]);
    });

    it('залогирует необязательное поле', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'username', value: 'dick_the_duck', isValid: true } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional', 'username' ]);
    });

    it('залогирует поле цвет', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'color', value: 14, isValid: true } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'long_form', 'required', 'color', 'success', 'Коричневый' ]);
    });

    it('залогирует поле кол-во владельцев', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'owners_number', value: 3, isValid: true } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'long_form', 'required', 'owners_number', 'success', '3' ]);
    });

    it('залогирует поле стс', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'sts', value: 3000111222, isValid: true } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional', 'sts', 'success' ]);
    });

    it('залогирует поле птс', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'pts', value: '1', isValid: true } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional', 'pts', 'original' ]);
    });

    it('залогирует поле с чекбоксом', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'online_view_available', value: true, isValid: true, controlTypes: [ 'checkbox' ] } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional', 'online_view_available', 'on' ]);
    });

    it('залогирует поле с изменениием названия метрики', () => {
        const store = mockStore(initialState);
        store.dispatch(log([ { field: 'notdisturb', value: true, isValid: true, controlTypes: [ 'checkbox' ] } ]));
        expect(metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'long_form', 'optional', 'trusted_dealer_calls_accepted', 'on' ]);
    });
});

it('залогирует событие из переданной строки', () => {
    const store = mockStore(initialState);
    store.dispatch(log([ { keyString: 'foo,bar' } ]));
    expect(metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_USER', 'cars', 'add', 'foo', 'bar' ]);
});

it('при логе учитывает тип пользователя, категорию и тип формы', () => {
    initialState = {
        config: configMock.withPageParams({ category: 'moto', form_type: 'edit' }).value(),
        user: userMock.withDealer(true).value(),
    };

    const store = mockStore(initialState);
    store.dispatch(log([ { field: 'mark', value: 'lada', isValid: true } ]));
    expect(metrika.sendParams).toHaveBeenCalledTimes(1);
    expect(metrika.sendParams).toHaveBeenCalledWith([ 'ADD_FORM_DEALER', 'moto', 'edit', 'short_form', 'mark' ]);
});
