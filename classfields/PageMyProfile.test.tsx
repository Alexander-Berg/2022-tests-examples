jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn().mockResolvedValue({}),
}));
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');

import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { showAutoclosableMessage, VIEW } from 'auto-core/react/dataDomain/notifier/actions/notifier';
import gateApi from 'auto-core/react/lib/gateApi';
import userWithAuth from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import userMock from 'auto-core/react/dataDomain/user/mocks';

import PageMyProfile from './PageMyProfile';

import '@testing-library/jest-dom';

const ContextProvider = createContextProvider(contextMock);

it('должен показать имя, если оно не совпадает с id', () => {
    const store = mockStore({
        user: userWithAuth,
    });
    render(
        <ContextProvider>
            <Provider store={ store }>
                <PageMyProfile/>
            </Provider>
        </ContextProvider>,
    );

    const name = document.querySelector('.PageMyProfile__name');

    expect(name).not.toBeNull();
});

it('не должен показать имя, если оно совпадает с id', () => {
    const store = mockStore({
        user: {
            data: {
                ...userWithAuth.data,
                name: undefined,
                profile: {
                    autoru: {
                        ...userWithAuth.data?.profile?.autoru,
                        alias: 'id123',
                    },
                },
            },
        },
    });
    render(
        <ContextProvider>
            <Provider store={ store }>
                <PageMyProfile/>
            </Provider>
        </ContextProvider>,
    );

    const name = document.querySelector('.PageMyProfile__name');

    expect(name).toBeNull();
});

it('отправляет запрос на переключение публичного профиля', async() => {
    const store = mockStore({
        user: userMock.withAuth(true).withReseller(true).withSocialProfiles([]).value(),
    });
    render(
        <ContextProvider>
            <Provider store={ store }>
                <PageMyProfile/>
            </Provider>
        </ContextProvider>,
    );

    const checkbox = await screen.findByRole('checkbox');
    userEvent.click(checkbox);

    await waitFor(() => {
        expect(gateApi.getResource).toHaveBeenCalledWith('updateProfile', { body: '{"allow_offers_show":true}' });
    });
    await waitFor(() => {
        expect(showAutoclosableMessage).toHaveBeenCalledWith({ view: VIEW.SUCCESS, message: 'Информация сохранена' });
    });
});

describe('метрики публичного профиля перекупа', () => {
    const INITIAL_STATE = {
        user: userMock.withAuth(true).withReseller().value(),
    };

    it('на показ', async() => {
        const store = mockStore(INITIAL_STATE);
        render(
            <ContextProvider>
                <Provider store={ store }>
                    <PageMyProfile/>
                </Provider>
            </ContextProvider>,
        );

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([
            'public_profile',
            'show',
        ]);
    });

    it('на клик', async() => {
        const store = mockStore(INITIAL_STATE);
        render(
            <ContextProvider>
                <Provider store={ store }>
                    <PageMyProfile/>
                </Provider>
            </ContextProvider>,
        );

        const checkbox = await screen.findByRole('checkbox');
        userEvent.click(checkbox);

        await waitFor(() => {
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([
                'public_profile',
                'click',
            ]);
        });
    });

    it('на клик когда чекбокс уже активен', async() => {
        const store = mockStore({
            user: userMock.withAuth(true).withReseller().withAllowOffersShow(true).value(),
        });
        render(
            <ContextProvider>
                <Provider store={ store }>
                    <PageMyProfile/>
                </Provider>
            </ContextProvider>,
        );

        const checkbox = await screen.findByRole('checkbox');
        userEvent.click(checkbox);

        await waitFor(() => {
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([
                'public_profile',
                'click',
                'remove',
            ]);
        });
    });
});
