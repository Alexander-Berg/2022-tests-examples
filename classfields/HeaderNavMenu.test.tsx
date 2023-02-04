/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import React from 'react';
import type { Store } from 'redux';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import closeNavMenu from 'auto-core/react/dataDomain/state/actions/navMenuClose';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import userStateMock from 'auto-core/react/dataDomain/user/mocks';
import type { StateGeo } from 'auto-core/react/dataDomain/geo/StateGeo';

import type { ReduxState } from './HeaderNavMenu';
import HeaderNavMenu from './HeaderNavMenu';

jest.mock('auto-core/react/dataDomain/state/actions/navMenuClose', () => {
    return jest.fn(() => ({ type: 'MOCK_ACTION' }));
});

declare var global: { location: URL };
const { location } = global;

describe('Без авторизации', () => {
    let store: Store;
    beforeEach(() => {
        store = mockStore({
            config: configStateMock
                .withDesktopUrl('https://auto.ru/')
                .withPageType('sales')
                .value(),
            state: {
                activeNavMenu: true,
            },
            user: userStateMock.withAuth(false).value(),
        });
    });

    it('пункт "Войти" должен вести на /login/', async() => {
        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(
            wrapper.find('.HeaderNavMenu__item').at(0),
        ).toHaveProp('url', 'https://autoru_frontend.auth_domain/login/?r=http%3A%2F%2Flocalhost%2F');
    });

    it('пункт "Сообщение" должен вести на /chat-auth/', async() => {
        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(
            wrapper.find('.HeaderNavMenu__item').at(4),
        ).toHaveProp('url', 'link/chat-auth/?r2=http%3A%2F%2Flocalhost%2F');
    });
});

describe('С авторизацией', () => {
    let store: Store;
    beforeEach(() => {
        store = mockStore({
            config: configStateMock
                .withDesktopUrl('https://auto.ru/')
                .withPageType('sales')
                .value(),
            state: {
                activeNavMenu: true,
            },
            user: userStateMock.withAuth(true).value(),
        });
    });

    it('пункт "Выйти" должен вести на /logout/', async() => {
        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: { ...contextMock, store } },
        ).dive();

        expect(
            wrapper.find('.HeaderNavMenu__logout').at(0),
        ).toHaveProp('url', 'https://autoru_frontend.auth_domain/logout/?r=http%3A%2F%2Flocalhost%2F');
    });

    describe('Клик на пункт "Сообщение"', () => {
        let chatItem: ShallowWrapper;
        beforeEach(() => {
            type VertisChat = typeof window.vertis_chat;
            window.vertis_chat = {
                show: jest.fn(),
            } as Partial<VertisChat> as VertisChat;

            const wrapper = shallow(
                <HeaderNavMenu/>,
                { context: { ...contextMock, store } },
            ).dive();

            chatItem = wrapper.find('.HeaderNavMenu__item').at(4);
            chatItem.simulate('click');
        });

        it('должен отправить метрику "chat_open_menu"', async() => {
            expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'chat_open_menu' ]);
        });

        it('должен открыть чат', async() => {
            expect(window.vertis_chat.show).toHaveBeenCalled();
        });

        it('должен закрыть меню', async() => {
            expect(closeNavMenu).toHaveBeenCalled();
        });
    });
});

describe('Моя тачка', () => {
    let store: ReduxState;
    beforeEach(() => {
        store = {
            config: configStateMock.withPageType('card').value(),
            state: {
                activeNavMenu: true,
                authModal: { isOpened: false },
            },
            user: userStateMock.withAuth(true).value(),
            geo: {} as StateGeo,
        };

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        delete global.location;
        global.location = new URL('https://auto.ru/?foo=bar');
    });

    afterEach(() => {
        global.location = location;
    });

    it('должен добавить ссылку "Моя тачка" на карточке легковых', () => {
        global.location.pathname = '/cars/used/sale/vaz/2170/1113786050-e92ae383/';

        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: { ...contextMock, store: mockStore(store) } },
        ).dive();
        const myCarLink = wrapper.find('.HeaderNavMenu__item_myCar');

        expect(myCarLink).toHaveProp('children', 'Моя тачка!');
        expect(myCarLink).toHaveProp(
            'url', '/login/cars/1113786050/?r=https%3A%2F%2Fauto.ru%2Fcars%2Fused%2Fsale%2Fvaz%2F2170%2F1113786050-e92ae383%2F%3Ffoo%3Dbar',
        );
    });

    it('должен добавить ссылку "Моя тачка" на карточке LCV', () => {
        global.location.pathname = '/lcv/used/sale/mazda/bongo/19224629-8b5459b4/';

        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: { ...contextMock, store: mockStore(store) } },
        ).dive();
        const myCarLink = wrapper.find('.HeaderNavMenu__item_myCar');

        expect(myCarLink).toHaveProp('children', 'Моя тачка!');
        expect(myCarLink).toHaveProp(
            'url', '/login/trucks/19224629/?r=https%3A%2F%2Fauto.ru%2Flcv%2Fused%2Fsale%2Fmazda%2Fbongo%2F19224629-8b5459b4%2F%3Ffoo%3Dbar',
        );
    });

    it('не должен добавить ссылку "Моя тачка" на карточке новых', () => {
        store.config = configStateMock.withPageType('card-new').value();
        global.location.pathname = '/cars/new/group/bmw/x5/21718431/21935248/1098091482-55e74d29/';

        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: { ...contextMock, store: mockStore(store) } },
        ).dive();
        const myCarLink = wrapper.find('.HeaderNavMenu__item_myCar');

        expect(myCarLink).not.toExist();
    });
});

describe('Гараж', () => {
    let store: ReduxState;
    beforeEach(() => {
        store = {
            config: configStateMock.withPageType('card').value(),
            state: {
                activeNavMenu: true,
                authModal: { isOpened: false },
            },
            user: userStateMock.withAuth(true).value(),
            geo: {} as StateGeo,
        };

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        delete global.location;
        global.location = new URL('https://auto.ru/?foo=bar');
    });

    afterEach(() => {
        global.location = location;
    });

    it('должен добавить ссылку "Гараж" если пользователь НЕ дилер', () => {
        global.location.pathname = '/cars/used/sale/vaz/2170/1113786050-e92ae383/';

        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: { ...contextMock, store: mockStore(store) } },
        ).dive();
        const garageLink = wrapper.findWhere(node => node.name() === 'Link' && node.prop('children') === 'Гараж');

        expect(garageLink).toHaveProp(
            'url', 'link/garage/?',
        );
    });

    it('не должен добавлять ссылку "Гараж" если пользователь дилер', () => {
        global.location.pathname = '/cars/used/sale/vaz/2170/1113786050-e92ae383/';
        store.user = userStateMock.withDealer(true).value();

        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: { ...contextMock, store: mockStore(store) } },
        ).dive();
        const garageLink = wrapper.findWhere(node => node.name() === 'Link' && node.prop('children') === 'Гараж');

        expect(garageLink).not.toExist();
    });
});

describe('Выкуп', () => {
    let store: ReduxState;
    beforeEach(() => {
        store = {
            config: configStateMock.withPageType('card').value(),
            state: {
                activeNavMenu: true,
                authModal: { isOpened: false },
            },
            user: userStateMock.withAuth(true).value(),
            geo: {} as StateGeo,
        };

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        delete global.location;
        global.location = new URL('https://auto.ru/?foo=bar');
    });

    afterEach(() => {
        global.location = location;
    });

    it('Показывает ссылку на Выкуп в Московской области', () => {
        const storeMock = {
            ...store,
            geo: {
                gids: [ 1 ],
                geoParents: [ { id: 225 } ],
            },
        };
        const wrapper = shallow(
            <HeaderNavMenu/>,
            {
                context: {
                    ...contextMock,
                    store: mockStore(storeMock),
                },
            },
        ).dive();

        const auctionLink = wrapper.findWhere(node => node.name() === 'Link' && node.prop('children') === 'Выкуп');

        expect(auctionLink).toExist();
    });

    it('Не показывает ссылку на Выкуп в любом регионе кроме Московского', () => {
        const storeMock = {
            ...store,
            geo: {
                gids: [ 2 ],
                geoParents: [ { id: 10174 }, { id: 225 } ],
            },
        };
        const wrapper = shallow(
            <HeaderNavMenu/>,
            {
                context: {
                    ...contextMock,
                    store: mockStore(storeMock),
                },
            },
        ).dive();

        const auctionLink = wrapper.findWhere(node => node.name() === 'Link' && node.prop('children') === 'Выкуп');

        expect(auctionLink).not.toExist();

    });
});

describe('ссылка на отчёты с метриками', () => {
    let store: Store;
    beforeEach(() => {
        store = mockStore({
            config: configStateMock
                .withDesktopUrl('https://auto.ru/')
                .withPageType('index')
                .value(),
            state: {
                activeNavMenu: true,
            },
            user: userStateMock.withAuth(true).value(),
        });
    });

    it('рендерится как ПроАвто', () => {
        const page = shallow(
            <HeaderNavMenu/>,
            { context: { ...contextMock, store } },
        ).dive();

        const link = page.find('Link[url="link/proauto-landing/?"]').dive().find('a');
        expect(link.text()).toEqual('ПроАвто');

        link.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'history', 'burger_click' ]);
    });

    it('в экспе рендерится как Отчёты', () => {
        const page = shallow(
            <HeaderNavMenu/>,
            {
                context: {
                    ...contextMock,
                    store,
                    hasExperiment: (flag: string) => flag === 'AUTORUFRONT-21544_rename_proauto',
                },
            },
        ).dive();

        const link = page.find('Link[url="link/proauto-landing/?"]').dive().find('a');
        expect(link.text()).toEqual('Отчёты');

        link.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'history', 'burger_click', 'reports' ]);
    });
});

describe('Добавить объявление', () => {
    let store: ReduxState;
    beforeEach(() => {
        store = {
            config: configStateMock.withPageType('card').value(),
            state: {
                activeNavMenu: true,
                authModal: { isOpened: false },
            },
            addOfferNavigateModal: {
                isVisible: false,
                hasAnimation: false,
                placeToCall: {},
            },
            user: userStateMock.withAuth(true).value(),
            geo: {} as StateGeo,
        };

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        delete global.location;
        global.location = new URL('https://auto.ru/?foo=bar');
    });

    afterEach(() => {
        global.location = location;
    });

    it('если НЕ дилер и эксп с поффером, ссылка ведет на провязку', () => {
        global.location.pathname = '/cars/used/sale/vaz/2170/1113786050-e92ae383/';

        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: {
                ...contextMock,
                store: mockStore(store),
                hasExperiment: (flag: string) => flag === 'AUTORUFRONT-21494_mobile_poffer',
            } },
        ).dive();

        const addOfferLink = wrapper.findWhere(node => node.name() === 'Link' && node.prop('url') === 'link/fromWebToApp/?');

        expect(addOfferLink).not.toExist();
    });

    it('если дилер, ссылка ведет на промо', () => {
        global.location.pathname = '/cars/used/sale/vaz/2170/1113786050-e92ae383/';
        store.user = userStateMock.withDealer(true).value();

        const wrapper = shallow(
            <HeaderNavMenu/>,
            { context: {
                ...contextMock,
                store: mockStore(store),
                hasExperiment: (flag: string) => flag === 'AUTORUFRONT-21494_mobile_poffer',
            } },
        ).dive();

        const addOfferLink = wrapper.findWhere(node => node.name() === 'Link' && node.prop('url') === 'link/fromWebToApp/?');

        expect(addOfferLink).toExist();
    });
});
