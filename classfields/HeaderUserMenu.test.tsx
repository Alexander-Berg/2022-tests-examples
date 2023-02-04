import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import userMock from 'auto-core/react/dataDomain/user/mocks';

import type { Props } from './HeaderUserMenu';
import HeaderUserMenu from './HeaderUserMenu';

let props: Props;

beforeEach(() => {
    props = {
        favoritesCount: 42,
        compareCount: 1,
        getAuthUrl: (path) => `authUrl/${ path }`,
        pageParams: {
            category: 'cars',
        },
        pageType: 'index',
        user: userMock.withAuth(true).value().data,
        onUserLinkClick: jest.fn(),
    };
});

describe('меню пользователя', () => {
    it('правильно формирует линки для обычного парня', () => {
        const page = shallowRenderComponent({ props });
        const instance = page.instance() as HeaderUserMenu;
        const links = instance.getUserLinks();
        expect(links).toMatchSnapshot();
    });
});

describe('рендер аватара', () => {
    it('рендерит, если он пришел', () => {
        const user = userMock.withAuth(true).value().data;

        const page = shallowRenderComponent({ props: { ...props, user } });
        const userPic = page.find('.HeaderUserMenu__userPic');

        expect(userPic).toExist();
    });

    it('не рендерит, если он не пришел', () => {
        const user = userMock.withAuth(true).value().data;
        delete user.profile!.autoru!.userpic;

        const page = shallowRenderComponent({ props: { ...props, user } });
        const userPic = page.find('.HeaderUserMenu__userPic');

        expect(userPic).not.toExist();
    });

    it('не рендерит, если не пришел объект sizes', () => {
        const user = userMock.withAuth(true).value().data;
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        delete user.profile?.autoru?.userpic?.sizes;

        const page = shallowRenderComponent({ props: { ...props, user } });
        const userPic = page.find('.HeaderUserMenu__userPic');

        expect(userPic).not.toExist();
    });
});

describe('текст на кнопке добавления объявления', () => {
    it('правильно формирует для обычного парня', () => {
        const page = shallowRenderComponent({ props });
        const addButton = page.find('.HeaderUserMenu__addButton');

        expect(addButton.children().text()).toBe('Разместить бесплатно');
    });

    it('правильно формирует для анона', () => {
        const user = userMock.withAuth(false).value().data;
        const page = shallowRenderComponent({ props: { ...props, user } });
        const addButton = page.find('.HeaderUserMenu__addButton');

        expect(addButton.children().text()).toBe('Разместить бесплатно');
    });

    it('правильно формирует для перекупа', () => {
        const user = userMock.withReseller(true).value().data;
        const page = shallowRenderComponent({ props: { ...props, user } });
        const addButton = page.find('.HeaderUserMenu__addButton');

        expect(addButton.children().text()).toBe('Продать');
    });

    it('правильно формирует для дилера', () => {
        const user = userMock.withDealer(true).value().data;
        const page = shallowRenderComponent({ props: { ...props, user } });
        const addButton = page.find('.HeaderUserMenu__addButton');

        expect(addButton.children().text()).toBe('Продать');
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow(
        <HeaderUserMenu { ...props }/>
        ,
        { context: contextMock },
    );

    return page;
}
