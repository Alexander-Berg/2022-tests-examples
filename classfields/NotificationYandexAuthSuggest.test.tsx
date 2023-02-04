jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');
jest.mock('auto-core/react/lib/openSocialPopup');
jest.mock('auto-core/react/dataDomain/cookies/actions/set', () => {
    return jest.fn(() => () => ({}));
});

// eslint-disable-next-line @typescript-eslint/no-use-before-define
const openSocialPopupMock = openSocialPopup as jest.MockedFunction<typeof openSocialPopup>;
openSocialPopupMock.mockImplementation(() => ({ }));

import React from 'react';
import { Provider } from 'react-redux';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import { SocialProvider } from '@vertis/schema-registry/ts-types-snake/vertis/common';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import Button from 'auto-core/react/components/islands/Button/Button';
import CloseButton from 'auto-core/react/components/common/CloseButton/CloseButton';
import openSocialPopup from 'auto-core/react/lib/openSocialPopup';
import { showAutoclosableMessage, showAutoclosableErrorMessage } from 'auto-core/react/dataDomain/notifier/actions/notifier';
import setCookie from 'auto-core/react/dataDomain/cookies/actions/set';
import { AutoPopupNames } from 'auto-core/react/dataDomain/autoPopup/types';
import configMock from 'auto-core/react/dataDomain/config/mock';
import { COOKIE_NAME_CLOSED, COOKIE_NAME_SEEN } from 'auto-core/react/components/desktop/AutoPopupLoader/items/yandex_auth_suggest_notification';

import NotificationYandexAuthSuggest from './NotificationYandexAuthSuggest';
import type { AppState } from './NotificationYandexAuthSuggest';

const showAutoclosableMessageMock = showAutoclosableMessage as jest.MockedFunction<typeof showAutoclosableMessage>;
const showAutoclosableErrorMessageMock = showAutoclosableErrorMessage as jest.MockedFunction<typeof showAutoclosableErrorMessage>;

const SHOW_TIMEOUT = 5000;
const HIDE_TIMEOUT = 120000;
const DISPATCH_TIMEOUT = 500;

let state: AppState;
const eventMap: Record<string, any> = {};

beforeEach(() => {
    jest.useFakeTimers();

    state = {
        autoPopup: {
            id: AutoPopupNames.YANDEX_AUTH_SUGGEST_NOTIFICATION,
            data: {
                uid: '24171229',
                login: 'alpha-san',
                displayName: {
                    name: 'alpha-san',
                    default_avatar: '21493/24171229-933261',
                },
                defaultEmail: 'alpha-san@yandex.ru',
                attributes: {
                    'beta-tester': true,
                    has_plus: true,
                    staff: true,
                    'staff-login': 'nop',
                },
            },
        },
        config: configMock.value(),
    };

    jest.spyOn(global, 'addEventListener').mockImplementation((event, cb) => {
        eventMap[event] = cb;
    });

    openSocialPopupMock.mockClear();
    contextMock.metrika.sendParams.mockClear();
});

afterEach(() => {
    jest.clearAllTimers();
    jest.restoreAllMocks();
});

describe('компонент', () => {
    let page: ShallowWrapper;
    beforeEach(() => {
        page = shallowRenderNotificationYandexAuthSuggest();
        jest.advanceTimersByTime(SHOW_TIMEOUT);
    });

    it('правильно рисуется', () => {
        expect(shallowToJson(page)).toMatchSnapshot();
    });

    it('отправит метрику при показе', () => {
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'passport', 'suggest-yandex-join', 'show' ]);
    });

    it('скроет попап по истечение заданного промежутка времени', () => {
        jest.advanceTimersByTime(HIDE_TIMEOUT + DISPATCH_TIMEOUT);
        expect(page.find('.NotificationBase_hidden')).toHaveLength(1);
    });
});

describe('при клике на крестик', () => {
    let page: ShallowWrapper;
    beforeEach(() => {
        page = shallowRenderNotificationYandexAuthSuggest();
        jest.advanceTimersByTime(SHOW_TIMEOUT);

        const closer = page.find(CloseButton);

        closer.simulate('click');
    });

    it('скроет нотификацию', () => {
        jest.advanceTimersByTime(DISPATCH_TIMEOUT);

        expect(page.find('.NotificationBase_hidden')).toHaveLength(1);
    });

    it('правильно поставит куки', () => {
        expect(setCookie).toHaveBeenCalledTimes(2);
        expect(setCookie).toHaveBeenNthCalledWith(2, COOKIE_NAME_CLOSED, 'true', { expires: 1825 });
        expect(setCookie).toHaveBeenNthCalledWith(1, COOKIE_NAME_SEEN, 'true', { expires: 1 });
    });

    it('отправить корректную метрику', () => {
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(2, [ 'passport', 'suggest-yandex-join', 'close' ]);
    });
});

describe('при клике на кнопку', () => {
    let page: ShallowWrapper;

    beforeEach(() => {
        page = shallowRenderNotificationYandexAuthSuggest();
        jest.advanceTimersByTime(SHOW_TIMEOUT);

        const button = page.find(Button);

        button.simulate('click');
    });

    it('откроет попап соц авторизации', () => {
        expect(openSocialPopup).toHaveBeenCalledTimes(1);
        expect(openSocialPopup).toHaveBeenCalledWith(SocialProvider.YANDEX, 'https://autoru_frontend.auth_domain/social/yandex/?r=https%3A%2F%2Fauto.ru%2F');
    });

    it('отправит корректную метрику', () => {
        expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(2);
        expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(2, [ 'passport', 'suggest-yandex-join', 'yandex-login' ]);
    });

    it('при получение проверочного сообщения от попапа ответит ему', () => {
        const sourcePostMessageMock = jest.fn();
        const rnd = '123';
        const origin = 'http://auto.ru';
        eventMap.message({
            data: { type: 'auth-ping', rnd },
            source: { postMessage: sourcePostMessageMock },
            origin,
        });

        expect(sourcePostMessageMock).toHaveBeenCalledTimes(1);
        expect(sourcePostMessageMock).toHaveBeenCalledWith({ type: 'auth-pong', rnd }, origin);
    });

    describe('при получение результата от попапа', () => {
        it('если ответ удачный, покажет нотификацию вверху экрана', () => {
            eventMap.message({ data: { type: 'auth-result', result: { success: true } } });

            expect(showAutoclosableMessageMock).toHaveBeenCalledTimes(1);
            expect(showAutoclosableMessageMock).toHaveBeenCalledWith({ message: 'Аккаунты связаны', view: 'success' });
        });

        it('если ответ неудачный, покажет нотификацию об ошибке вверху экрана и отправит метрику', () => {
            eventMap.message({ data: { type: 'auth-result', result: { } } });

            expect(showAutoclosableErrorMessageMock).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(3);
            expect(contextMock.metrika.sendParams).toHaveBeenNthCalledWith(3, [ 'passport', 'suggest-yandex-join', 'error' ]);
        });

        it('скроет саму нотификацию', () => {
            eventMap.message({ data: { type: 'auth-result', result: {} } });

            jest.advanceTimersByTime(500 + DISPATCH_TIMEOUT);

            expect(page.find('.NotificationBase_hidden')).toHaveLength(1);
        });
    });
});

function shallowRenderNotificationYandexAuthSuggest() {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(state);

    const wrapper = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <NotificationYandexAuthSuggest/>
            </Provider>
        </ContextProvider>,
    );

    return wrapper.dive().dive().dive();
}
