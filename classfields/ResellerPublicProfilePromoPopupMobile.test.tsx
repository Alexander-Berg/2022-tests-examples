jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');
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
import { showAutoclosableMessage } from 'auto-core/react/dataDomain/notifier/actions/notifier';

import ResellerPublicProfilePromoPopupMobile from './ResellerPublicProfilePromoPopupMobile';

const showAutoclosableMessageMock = showAutoclosableMessage as jest.MockedFunction<typeof showAutoclosableMessage>;
const promiseResolve = Promise.resolve({});
const getResourceMock = getResource as jest.MockedFunction<typeof getResource>;
getResourceMock.mockImplementation(() => promiseResolve);

const INITIAL_STATE = {
    cookie: { 'reseller-public-profile-popup-shown': 'true' },
    userPromoPopup: {
        id: AutoPopupNames.RESELLER_PUBLIC_PROFILE_POPUP,
        data: { encryptedUserId: 'some_encrypted_user_id' },
    },
};
const Context = createContextProvider(contextMock);

it('отпраляет метрику на каждое открытие попапа', () => {
    const { rerender } = render(getComponent(), { wrapper: getWrapper() });
    // метрика на первое открытие попапа
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_modal', 'show' ]);

    // "закрываем" попап
    rerender(getComponent(false));
    // "открываем" попап
    rerender(getComponent());

    // метрика на повторное открытие попапа
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_modal', 'show' ]);
});

describe('клик по получить статус', () => {
    it('отправит запрос с нужными параметрами', async() => {
        const store = mockStore(INITIAL_STATE);
        render(getComponent(), { wrapper: getWrapper(store) });

        const statusButton = await screen.findByText(/получить статус/i);

        userEvent.click(statusButton);

        expect(getResource).toHaveBeenCalledWith('updateProfile', { body: '{"allow_offers_show":true}' });
    });

    it('после запроса обновит пользовательский флаг на показ офферов в стейте, покажет нотифай и перейдет к следующему виду модала', async() => {
        const store = mockStore(INITIAL_STATE);
        render(getComponent(), { wrapper: getWrapper(store) });

        const statusButton = await screen.findByText(/получить статус/i);

        userEvent.click(statusButton);

        return promiseResolve.then(async() => {
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_modal', 'success' ]);
            expect(store.getActions()).toEqual(expect.arrayContaining([
                { type: 'UPDATE_ALLOW_OFFERS_SHOW', payload: true },
            ]));
            expect(showAutoclosableMessageMock).toHaveBeenCalledWith({
                message: 'Информация сохранена',
                view: 'success',
            });
            expect(await screen.findByText(/посмотреть страницу объявлений/i)).toBeInTheDocument();
        });
    });

    it('клики по ссылкам настройки и посмотреть страницу объявлений', async() => {
        const store = mockStore(INITIAL_STATE);
        render(getComponent(), { wrapper: getWrapper(store) });

        const statusButton = await screen.findByText(/получить статус/i);

        userEvent.click(statusButton);

        return promiseResolve.then(async() => {
            const publicProfileUrl = await screen.findByText(/посмотреть страницу объявлений/i);

            userEvent.click(publicProfileUrl);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_link', 'click' ]);

            const settingsLink = await screen.findByText(/профиле/i);

            userEvent.click(settingsLink);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'settings_link', 'click', 'modal' ]);
        });
    });
});

describe('клик на кнопку позже', () => {
    it('по клику отправит метрику и вызовет тултип', async() => {
        const store = mockStore(INITIAL_STATE);
        render(getComponent(), { wrapper: getWrapper(store) });

        const laterButton = await screen.findByText(/позже/i);

        userEvent.click(laterButton);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_modal', 'later' ]);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_tooltip', 'show' ]);
        expect(showAutoclosableMessageMock).toHaveBeenCalledTimes(1);
    });
});

describe('закрытие модала', () => {
    it('отправит метрику при клике по крестику', async() => {
        const store = mockStore(INITIAL_STATE);
        render(getComponent(), { wrapper: getWrapper(store) });

        const closerButton = screen.getByLabelText('close');
        userEvent.click(closerButton);

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_promo_modal', 'close' ]);
    });
});

function getWrapper(store: any = mockStore(INITIAL_STATE)) {
    const wrapper = ({ children }: { children: JSX.Element }) => (
        <Provider store={ store }>
            <Context>
                { children }
            </Context>
        </Provider>
    );
    return wrapper;
}

function getComponent(isOpened = true) {
    return (
        <ResellerPublicProfilePromoPopupMobile
            encryptedUserId="some_encrypted_user_id"
            isOpened={ isOpened }
        />
    );
}
