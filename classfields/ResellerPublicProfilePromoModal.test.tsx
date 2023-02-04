jest.mock('auto-core/react/lib/gateApi');

import React from 'react';
import { Provider } from 'react-redux';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import '@testing-library/jest-dom';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { getResource } from 'auto-core/react/lib/gateApi';
import { AutoPopupNames } from 'auto-core/react/dataDomain/autoPopup/types';

import ResellerPublicProfilePromoModal from './ResellerPublicProfilePromoModal';

const promiseResolve = Promise.resolve({});
const getResourceMock = getResource as jest.MockedFunction<typeof getResource>;
getResourceMock.mockImplementation(() => promiseResolve);

const INITIAL_STATE = {
    cookie: { 'reseller-public-profile-popup-shown': 'true' },
    autoPopup: {
        id: AutoPopupNames.RESELLER_PUBLIC_PROFILE_POPUP,
        data: { encryptedUserId: 'some_encrypted_user_id' },
    },
    searchID: {
        searchID: 'searchID',
        parentSearchId: 'parentSearchID',
    },
};
const Context = createContextProvider(contextMock);

it('отправит метрику на показ модала', () => {
    render(getComponentWithWrapper(mockStore(INITIAL_STATE)));

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_modal', 'show' ]);
});

describe('клик по получить статус', () => {
    it('отправит запрос с нужными параметрами', async() => {
        const store = mockStore(INITIAL_STATE);
        render(getComponentWithWrapper(store));

        const statusButton = await screen.findByText(/получить статус/i);

        userEvent.click(statusButton);

        expect(getResource).toHaveBeenCalledWith('updateProfile', { body: '{"allow_offers_show":true}' });
    });

    it('после запроса обновит пользовательский флаг на показ офферов в стейте, покажет нотифай и перейдет к следующему виду модала', async() => {
        const store = mockStore(INITIAL_STATE);
        render(getComponentWithWrapper(store));

        const statusButton = await screen.findByText(/получить статус/i);

        userEvent.click(statusButton);

        return promiseResolve.then(async() => {
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_modal', 'success' ]);
            expect(store.getActions()).toEqual(expect.arrayContaining([
                { type: 'UPDATE_ALLOW_OFFERS_SHOW', payload: true },
                {
                    type: 'NOTIFIER_SHOW_MESSAGE',
                    payload: {
                        message: 'Информация сохранена',
                        view: 'success',
                    },
                },
            ]));
            expect(await screen.findByText(/посмотреть страницу объявлений/i)).toBeInTheDocument();
        });
    });

    it('клики по ссылкам настройки и посмотреть страницу объявлений', async() => {
        const store = mockStore(INITIAL_STATE);
        render(getComponentWithWrapper(store));

        const statusButton = await screen.findByText(/получить статус/i);

        userEvent.click(statusButton);

        return promiseResolve.then(async() => {
            const publicProfileUrl = await screen.findByText(/посмотреть страницу объявлений/i);

            userEvent.click(publicProfileUrl);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_link', 'click' ]);

            const settingsLink = await screen.findByText(/настройках/i);

            userEvent.click(settingsLink);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'settings_link', 'click', 'modal' ]);
        });
    });
});

describe('клик на кнопку позже', () => {
    it('по клику отправит метрику и вызовет тултип', async() => {
        const store = mockStore(INITIAL_STATE);
        render(getComponentWithWrapper(store));

        const laterButton = await screen.findByText(/позже/i);

        userEvent.click(laterButton);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_modal', 'later' ]);
        expect(store.getActions()).toEqual(expect.arrayContaining([
            {
                type: 'SHOW_RESELLER_PUBLIC_PROMO_TOOLTIP',
            },
        ]));
    });
});

function getComponentWithWrapper(store: any) {
    return (
        <Provider store={ store }>
            <Context>
                <ResellerPublicProfilePromoModal/>
            </Context>
        </Provider>
    );
}
