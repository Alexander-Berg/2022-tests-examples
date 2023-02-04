jest.mock('auto-core/react/dataDomain/journal/actions/fetch');

import 'jest-enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import fetchJournalData from 'auto-core/react/dataDomain/journal/actions/fetch';
import journalMock from 'auto-core/react/dataDomain/journal/mocks/defaultState.mock';

import type { AppState } from './HeaderMagMenu';
import HeaderMagMenu from './HeaderMagMenu';

const fetchJournalDataMock = fetchJournalData as jest.MockedFunction<typeof fetchJournalData>;
fetchJournalDataMock.mockReturnValue(() => Promise.resolve(null));

let baseState: AppState;
beforeEach(() => {
    baseState = {
        journalWidget: journalMock,
    };
});

it('правильно формирует ссылки', () => {
    const wrapper = renderWrapper({ initialState: baseState });
    const sideNavLinks = wrapper.find('.HeaderMagMenu__sideNavLink') as any; // у ShallowWrapper-а не определен иттератор
    const buttonMore = wrapper.find('.HeaderMagMenu__buttonMore') as any;
    const leadArticle = wrapper.find('.HeaderMagMenu__lead Link') as any;
    const leadArticleTheme = wrapper.find('.HeaderMagMenu__leadTheme') as any;
    const articleLinks = wrapper.find('.HeaderMagMenu__articleItemLink') as any;
    expect([ ...sideNavLinks, ...buttonMore, ...leadArticle, ...leadArticleTheme, ...articleLinks ]).toMatchSnapshot();
});

it('при рендере контента отправит метрику показа', () => {
    renderWrapper({ initialState: baseState });

    expect(contextMock.metrika.sendParams).toHaveBeenCalledTimes(4);
    expect(contextMock.metrika.sendParams.mock.calls).toMatchSnapshot();
});

function renderWrapper(
    { initialState }:
    { initialState: AppState },
) {
    const store = mockStore(initialState);
    const Context = createContextProvider(contextMock);

    const page = shallow(
        <Context>
            <Provider store={ store }>
                <HeaderMagMenu/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    return page;
}
