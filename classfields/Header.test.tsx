/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/cookie');
jest.mock('auto-core/react/dataDomain/state/actions/favoritesCurtainOpen');
jest.mock('auto-core/react/dataDomain/state/actions/headerChatAuthPopupOpen');
jest.mock('auto-core/react/dataDomain/state/actions/subscriptionPopupOpen');
jest.mock('auto-core/react/dataDomain/state/actions/subscriptionPopupClose');

import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import favoritesCurtainOpen from 'auto-core/react/dataDomain/state/actions/favoritesCurtainOpen';
import openChatAuthPopup from 'auto-core/react/dataDomain/state/actions/headerChatAuthPopupOpen';
import subscriptionPopupOpen from 'auto-core/react/dataDomain/state/actions/subscriptionPopupOpen';
import subscriptionPopupClose from 'auto-core/react/dataDomain/state/actions/subscriptionPopupClose';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';
import favoritesStateMock from 'auto-core/react/dataDomain/favorites/mocks';
import { subscriptionStateMock } from 'auto-core/react/dataDomain/subscriptions/mocks';
import geoMock from 'auto-core/react/dataDomain/geo/mocks/geo.mock';

import type { AppState, OwnProps } from './Header';
import Header from './Header';

const favoritesCurtainOpenMock = favoritesCurtainOpen as jest.MockedFunction<typeof favoritesCurtainOpen>;
favoritesCurtainOpenMock.mockReturnValue({ type: 'foo' });

const openChatAuthPopupMock = openChatAuthPopup as jest.MockedFunction<typeof openChatAuthPopup>;
openChatAuthPopupMock.mockReturnValue({ type: 'foo' });

const subscriptionPopupOpenMock = subscriptionPopupOpen as jest.MockedFunction<typeof subscriptionPopupOpen>;
subscriptionPopupOpenMock.mockReturnValue(() => {});

const subscriptionPopupCloseMock = subscriptionPopupClose as jest.MockedFunction<typeof subscriptionPopupClose>;
subscriptionPopupCloseMock.mockReturnValue({ type: 'foo' });

const eventMock = {
    stopPropagation: jest.fn(),
};
declare var global: {
    vertis_chat: {
        show: () => void;
    };
};

let baseState: AppState;
let originalWindowVertisChat: any;

beforeEach(() => {
    baseState = {
        autoPopup: { id: undefined },
        compare: {
            data: {
                catalog_card_ids: [ ],
                offers_ids_count: 3,
                models_ids_count: 9,
            },
            hasError: false,
            pending: false,
        },
        config: configStateMock.value(),
        cookies: {},
        favorites: favoritesStateMock.withOffers([]).value(),
        geo: geoMock,
        state: {
            activeNavMenu: false,
            authModal: {},
        },
        subscriptions: subscriptionStateMock.value(),
        user: userStateMock.value(),
        bunker: {},
    };

    originalWindowVertisChat = global.vertis_chat;
    global.vertis_chat = {
        show: jest.fn(),
    };
});

afterEach(() => {
    global.vertis_chat = originalWindowVertisChat;
});

describe('передает в SearchLineSuggest правильные параметры', () => {
    it('передает категорию, если она отличается от cars', () => {
        const initialState: AppState = {
            ...baseState,
            config: configStateMock.withPageParams({ category: 'moto' }).value(),
        };

        const wrapper = renderWrapper({ initialState });
        const searchLineSuggest = wrapper.find('SearchLineSuggest');

        expect(searchLineSuggest).toHaveProp('category', 'moto');
    });

    it('передает категорию, если это cars и она есть в ссылке', () => {
        const initialState: AppState = {
            ...baseState,
            config: configStateMock.withPageParams({ category: 'cars' }).withUrl('link/cars/something').value(),
        };

        const wrapper = renderWrapper({ initialState });
        const searchLineSuggest = wrapper.find('SearchLineSuggest');

        expect(searchLineSuggest).toHaveProp('category', 'cars');
    });

    it('не передает категорию, если это cars и в ссылке явно она не указана', () => {
        const initialState: AppState = {
            ...baseState,
            config: configStateMock.withPageParams({ category: 'cars' }).value(),
        };

        const wrapper = renderWrapper({ initialState });
        const searchLineSuggest = wrapper.find('SearchLineSuggest');

        expect(searchLineSuggest).toHaveProp('category', '');
    });

    it('не передает категорию, если это all', () => {
        const initialState: AppState = {
            ...baseState,
            config: configStateMock.withPageParams({ category: 'all' }).value(),
        };

        const wrapper = renderWrapper({ initialState });
        const searchLineSuggest = wrapper.find('SearchLineSuggest');

        expect(searchLineSuggest).toHaveProp('category', '');
    });
});

describe('мои ссылки', () => {
    it('правильно формирует список ссылок', () => {
        const initialState: AppState = {
            ...baseState,
            config: configStateMock.withPageType('sales').value(),
            state: {
                ...baseState.state,
                isFavoritesCurtainOpen: true,
            },
        };

        const wrapper = renderWrapper({ initialState });
        const links = wrapper.find('HeaderMyLink');

        expect(links).toMatchSnapshot();
    });

    describe('клик на избранном', () => {
        it('если попап открыт ничего не будет делать', () => {
            const initialState: AppState = {
                ...baseState,
                state: {
                    ...baseState.state,
                    isFavoritesCurtainOpen: true,
                },
            };

            const wrapper = renderWrapper({ initialState });
            const link = wrapper.find('HeaderMyLink').at(0);
            link.simulate('click', 'favorites', eventMock);

            expect(favoritesCurtainOpenMock).toHaveBeenCalledTimes(0);
        });

        it('если попап не открыт, откроет его и отправит метрику', () => {
            const wrapper = renderWrapper({ initialState: baseState });
            contextMock.metrika.sendPageEvent.mockClear();

            const link = wrapper.find('HeaderMyLink').at(0);
            link.simulate('click', 'favorites', eventMock);

            expect(favoritesCurtainOpenMock).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith('favorites_click');
        });
    });

    describe('клик на чате', () => {
        it('если попап открыт ничего не будет делать', () => {
            const initialState: AppState = {
                ...baseState,
                state: {
                    ...baseState.state,
                    isChatCurtainOpen: true,
                },
            };

            const wrapper = renderWrapper({ initialState });
            const link = wrapper.find('HeaderMyLink').at(0);
            link.simulate('click', 'messages', eventMock);

            expect(openChatAuthPopupMock).toHaveBeenCalledTimes(0);
        });

        it('если попап не открыт и пользователь залогин, откроет его и отправит метрику', () => {
            const initialState: AppState = {
                ...baseState,
                user: userStateMock.withAuth(true).value(),
            };
            const wrapper = renderWrapper({ initialState });
            contextMock.metrika.sendPageEvent.mockClear();

            const link = wrapper.find('HeaderMyLink').at(0);
            link.simulate('click', 'messages', eventMock);

            expect(global.vertis_chat.show).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith('chat_open_menu');
        });

        it('если попап не открыт и пользователь анон, откроет попап авторизации', () => {
            const initialState: AppState = {
                ...baseState,
                user: userStateMock.withAuth(false).value(),
            };
            const wrapper = renderWrapper({ initialState });
            contextMock.metrika.sendPageEvent.mockClear();

            const link = wrapper.find('HeaderMyLink').at(0);
            link.simulate('click', 'messages', eventMock);

            expect(openChatAuthPopupMock).toHaveBeenCalledTimes(1);
            expect(global.vertis_chat.show).toHaveBeenCalledTimes(0);
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        });
    });

    describe('клик на подписках', () => {
        it('если попап открыт закроет его', () => {
            const initialState: AppState = {
                ...baseState,
                state: {
                    ...baseState.state,
                    isSubscriptionPopupOpen: true,
                },
            };

            const wrapper = renderWrapper({ initialState });
            const link = wrapper.find('HeaderMyLink').at(0);
            link.simulate('click', 'searches', eventMock);

            expect(subscriptionPopupCloseMock).toHaveBeenCalledTimes(1);
        });

        it('если попап не открыт, откроет его и отправит метрику', () => {
            const wrapper = renderWrapper({ initialState: baseState });
            contextMock.metrika.sendPageAuthEvent.mockClear();

            const link = wrapper.find('HeaderMyLink').at(0);
            link.simulate('click', 'searches', eventMock);

            expect(subscriptionPopupOpenMock).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledTimes(1);
            expect(contextMock.metrika.sendPageAuthEvent).toHaveBeenCalledWith([ 'searches-head', 'open' ]);
        });
    });
});

function renderWrapper(
    { initialState, props }:
    { props?: OwnProps; initialState: AppState },
) {
    const store = mockStore(initialState);
    const Context = createContextProvider(contextMock);

    const page = shallow(
        <Context>
            <Provider store={ store }>
                <Header { ...props }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    return page;
}
