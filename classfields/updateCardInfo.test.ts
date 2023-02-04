import _ from 'lodash';

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.resolve()),
    };
});
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';

import gateApi from 'auto-core/react/lib/gateApi';
import { showAutoclosableMessage } from 'auto-core/react/dataDomain/notifier/actions/notifier';

import { USER_TIED_CARD_UPDATE } from '../types';

import updateCardInfo from './updateCardInfo';

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;
const params = {
    card_id: '123456|4321',
    preferred: true,
    payment_system_id: 'yandexkassa_v3',
};

let store: ThunkMockStore<Record<string, never>>;
beforeEach(() => {
    store = mockStore();
});

it('вызовет правильный ресурс с переданными параметрами', () => {
    store.dispatch(updateCardInfo(params));
    expect(getResource).toHaveBeenCalledTimes(1);
    expect(getResource).toHaveBeenCalledWith('updateCardInfoWithUserFormat', params);
});

describe('при успешном ответе', () => {
    it('создаст экшн "USER_TIED_CARD_UPDATE" и передаст в него результат', () => {
        const response = [ { foo: 'bar' } ];
        const gateApiPromise = Promise.resolve(response);
        getResource.mockImplementation(() => gateApiPromise);
        const expectedAction = { type: USER_TIED_CARD_UPDATE, payload: response };

        store.dispatch(updateCardInfo(params));

        return gateApiPromise
            .then(() => {
                expect(store.getActions()).toEqual(expect.arrayContaining([ expect.objectContaining(expectedAction) ]));
            });
    });

    it('покажет нотификацию с правильным сообщением если пользователь устанавливает флаг у карты', () => {
        const response = [ { foo: 'bar' } ];
        const gateApiPromise = Promise.resolve(response);
        getResource.mockImplementation(() => gateApiPromise);

        store.dispatch(updateCardInfo(params));

        return gateApiPromise
            .then(() => {
                expect(showAutoclosableMessage).toHaveBeenCalledTimes(1);
                expect(showAutoclosableMessage).toHaveBeenCalledWith({ message: 'Теперь карта основная', view: 'success' });
            });
    });

    it('покажет нотификацию с правильным сообщением если пользователь удаляет флаг у карты', () => {
        const response = [ { foo: 'bar' } ];
        const gateApiPromise = Promise.resolve(response);
        getResource.mockImplementation(() => gateApiPromise);

        const paramsToPass = _.cloneDeep(params);
        paramsToPass.preferred = false;
        store.dispatch(updateCardInfo(paramsToPass));

        return gateApiPromise
            .then(() => {
                expect(showAutoclosableMessage).toHaveBeenCalledTimes(1);
                expect(showAutoclosableMessage).toHaveBeenCalledWith({ message: 'Карта больше не основная', view: 'success' });
            });
    });
});
