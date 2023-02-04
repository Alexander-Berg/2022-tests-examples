/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import userStateMock from 'auto-core/react/dataDomain/user/mocks';

import VideoApp from './VideoApp';

const defaultState = {
    ads: { data: {} },
    router: { current: { data: { controller: 'video' } } },
    user: userStateMock.value(),
    config: { data: {} },
};

it('прокидывает isDealerUser=true если пользователь авторизован под дилером', () => {
    const store = mockStore({ ...defaultState, user: userStateMock.withDealer(true).value() });

    const wrapper = shallow(
        <VideoApp
        />, { context: { ...contextMock, store } },
    ).dive();

    const indexTabs = wrapper.find('IndexTabs');

    expect(indexTabs.prop('isDealerUser')).toEqual(true);
});

it('прокидывает isDealerUser=false если пользователь не авторизован под дилером', () => {
    const store = mockStore(defaultState);

    const wrapper = shallow(
        <VideoApp
        />, { context: { ...contextMock, store } },
    ).dive();

    const indexTabs = wrapper.find('IndexTabs');

    expect(indexTabs.prop('isDealerUser')).toEqual(false);
});
