/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
import _ from 'lodash';

import type { ThunkMockStore } from 'autoru-frontend/mocks/mockStore';
import mockStore from 'autoru-frontend/mocks/mockStore';

import gateApi from 'auto-core/react/lib/gateApi';
import actionTypes from 'auto-core/react/dataDomain/credit/actionTypes';

import createCreditApplication from './createCreditApplication';

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

let store: ThunkMockStore<Record<string, unknown>>;

beforeEach(() => {
    store = mockStore({});
});

const draftCreditApplicationData = {
    phone: '79998887766',
    email: 'email@example.com',
    fullName: 'Фамилия Имя Отчество',
    gids: [ 1 ],
};

it('сделает запрос в дадату для разбора ФИО', () => {
    getResource.mockImplementationOnce(() => Promise.resolve({}));
    getResource.mockImplementationOnce(() => Promise.resolve({}));

    store.dispatch(createCreditApplication(draftCreditApplicationData));

    return Promise.resolve().then(() => {
        expect(getResource).toHaveBeenCalledWith('getFioSuggestions', { value: draftCreditApplicationData.fullName, count: 1 });
    });
});

it('сделает запрос на создание заявки, задиспатчит успех', () => {
    const suggestResponse = {
        surname: 'Surname',
        name: 'Name',
        patronymic: 'Patronymic',
        gender: 'male',
    };

    const expectedActions = [
        { type: actionTypes.CREDIT_APPLICATION_CREATE_PENDING },
        {
            type: actionTypes.CREDIT_APPLICATION_CREATE_SUCCESS,
            payload: {},
        },
    ];

    getResource.mockImplementationOnce(() => Promise.resolve({
        suggestions: [ { data: suggestResponse } ],
    }));
    getResource.mockImplementationOnce(() => Promise.resolve({}));

    store.dispatch(createCreditApplication(draftCreditApplicationData));

    return Promise.resolve().then(() => {
        expect(getResource).toHaveBeenCalledTimes(2);
        expect(getResource).toHaveBeenNthCalledWith(2, 'createCreditApplication', { data: _.assign(draftCreditApplicationData, suggestResponse) });
        setTimeout(() => {
            expect(store.getActions()).toEqual(expectedActions);
        }, 100);
    });
});

it('анкета создастся даже если дадата недоступна или отвечает фигню', () => {
    const expectedActions = [
        { type: actionTypes.CREDIT_APPLICATION_CREATE_PENDING },
        {
            type: actionTypes.CREDIT_APPLICATION_CREATE_SUCCESS,
            payload: {},
        },
    ];

    getResource.mockImplementationOnce(() => Promise.resolve());
    getResource.mockImplementationOnce(() => Promise.resolve({}));

    store.dispatch(createCreditApplication(draftCreditApplicationData));

    return Promise.resolve().then(() => {
        expect(getResource).toHaveBeenCalledTimes(2);
        expect(getResource).toHaveBeenNthCalledWith(2, 'createCreditApplication', { data: draftCreditApplicationData });
        setTimeout(() => {
            expect(store.getActions()).toEqual(expectedActions);
        }, 100);
    });
});
