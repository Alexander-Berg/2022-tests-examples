/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import userStateMock from 'auto-core/react/dataDomain/user/mocks';

import IndexHeader from './IndexHeader';

declare var global: { location: Record<string, any> };
const { location } = global;

beforeEach(() => {
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.location;
    global.location = {
        assign: jest.fn(),
    };
});

afterEach(() => {
    global.location = location;
});

const defaultProps = {
    pageParams: { category: 'moto' },
    searchTagsDictionary: [],
    mmmInfo: [],
};

it('должен рисовать прелоадер при сабмите фильтров и перейти в листинг', () => {
    const store = mockStore({ user: { data: userStateMock.value() } });

    const wrapper = shallow(
        <IndexHeader { ...defaultProps }/>,
        { context: { ...contextMock, store } },
    ).dive();
    wrapper.setState({ showFiltersPopup: true });
    wrapper.find('Loadable').dive().find('Connect(ListingFiltersPopup)').simulate('submit');
    expect(wrapper.find('Preloader').isEmptyRender()).toBe(false);
    expect(window.location.href).toBe('link/listing/?');

});

it('прокидывает isDealerUser=true если пользователь авторизован под дилером', () => {
    const store = mockStore({ user: userStateMock.withDealer(true).value() });

    const wrapper = shallow(
        <IndexHeader { ...defaultProps }/>,
        { context: { ...contextMock, store } },
    ).dive();

    const indexTabs = wrapper.find('IndexTabs');

    expect(indexTabs.prop('isDealerUser')).toEqual(true);
});

it('прокидывает isDealerUser=false если пользователь не авторизован под дилером', () => {
    const store = mockStore({ user: userStateMock.value() });

    const wrapper = shallow(
        <IndexHeader { ...defaultProps }/>,
        { context: { ...contextMock, store } },
    ).dive();

    const indexTabs = wrapper.find('IndexTabs');

    expect(indexTabs.prop('isDealerUser')).toEqual(false);
});
