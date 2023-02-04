import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import noAuthUser from 'auto-core/react/dataDomain/user/mocks/withoutAuth.mock';
import authUser from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';

import type { MobileAppState } from 'www-mobile/react/MobileAppState';

import CardActions from './CardActions';

declare var global: { location: Record<string, any>};
const { location } = global;

beforeEach(() => {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.location;
    global.location = {
        assign: jest.fn(),
        href: 'http://foo.bar',
    };
});

afterEach(() => {
    global.location = location;
});

describe('при клике на заметку', () => {
    it('незалогина отправит на страницу логина', () => {
        const store = mockStore({
            config: configStateMock.value(),
            user: noAuthUser,
        }) as Partial<MobileAppState> as MobileAppState;

        const wrapper = shallow(
            <CardActions offer={ offerMock } canShowSafeDealBlock={ false }/>,
            { context: { store, ...contextMock } },
        ).dive();

        const noteButton = wrapper.find('.CardActions__button').at(0).find('.CardActions__buttonContent');
        noteButton.simulate('click');

        expect(global.location.assign).toHaveBeenCalledTimes(1);
        expect(global.location.assign).toHaveBeenCalledWith('https://autoru_frontend.auth_domain/login/?r=https%3A%2F%2Fauto.ru%2F%3Fcallback%3DshowNote');

        const noteModal = wrapper.find('Connect(OfferNoteEditModal)');
        expect(noteModal.prop('visible')).toBe(false);
    });

    it('для залогина откроет модал с заметкой', () => {
        const store = mockStore({
            config: configStateMock.value(),
            user: authUser,
        }) as Partial<MobileAppState> as MobileAppState;

        const wrapper = shallow(
            <CardActions offer={ offerMock } canShowSafeDealBlock={ false }/>,
            { context: { store, ...contextMock } },
        ).dive();

        const noteButton = wrapper.find('.CardActions__button').at(0).find('.CardActions__buttonContent');
        noteButton.simulate('click');

        const noteModal = wrapper.find('Connect(OfferNoteEditModal)');

        expect(global.location.assign).toHaveBeenCalledTimes(0);
        expect(noteModal.prop('visible')).toBe(true);
    });
});

describe('после возврата со страницы логина', () => {
    let wrapper: any;

    beforeEach(() => {
        const store = mockStore({
            config: configStateMock.withPageParams({ callback: 'showNote' }).value(),
            user: authUser,
        }) as Partial<MobileAppState> as MobileAppState;
        global.location.href = 'http://foo.bar/?baz=true&callback=showNote';

        wrapper = shallow(
            <CardActions offer={ offerMock } canShowSafeDealBlock={ false }/>,
            { context: { store, ...contextMock } },
        ).dive();
    });

    it('откроет модал с заметкой', () => {
        const noteModal = wrapper.find('Connect(OfferNoteEditModal)');
        expect(noteModal.prop('visible')).toBe(true);
    });

    it('удалит параметр callback из урла', () => {
        expect(contextMock.replaceState).toHaveBeenCalledTimes(1);
        expect(contextMock.replaceState).toHaveBeenCalledWith('http://foo.bar/?baz=true');
    });
});
