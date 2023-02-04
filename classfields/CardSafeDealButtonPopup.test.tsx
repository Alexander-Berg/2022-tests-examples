/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/lib/cookie');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import { AutoPopupNames } from 'auto-core/react/dataDomain/autoPopup/types';

const Context = createContextProvider(contextMock);
const store = mockStore({
    safeDeal: {},
    autoPopup: {
        id: AutoPopupNames.SAFE_DEAL_PROMO_TOOLTIP,
    },
});

import CardSafeDealButtonPopup from './CardSafeDealButtonPopup';

function renderComponent() {
    const anchor = React.createRef<HTMLDivElement>();

    return shallow(
        <Context>
            <Provider store={ store }>
                <CardSafeDealButtonPopup anchor={ anchor }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();
}

it('приавльно сформирует ссылку на промо в попапе в кнопке подробнее', () => {
    const wrapper = renderComponent();
    expect(wrapper.find('Button.CardSafeDealButtonPopup__button').prop('url')).toEqual('link/safe-deal-promo/?');
});

describe('правильно отправляет метрику', () => {
    it('при показе', () => {
        renderComponent();
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'owner_block', 'safe_deal_button', 'popup', 'show' ]);
    });
    it('при клике на подробнее', () => {
        const wrapper = renderComponent();
        wrapper.find('Button').dive().simulate('click');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'owner_block', 'safe_deal_button', 'popup', 'more_button', 'click' ]);
    });
    it('при закрытии попапа', () => {
        const wrapper = renderComponent();
        wrapper.find('Popup').dive().find('CloseButton').simulate('click');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'owner_block', 'safe_deal_button', 'popup', 'close_button', 'click' ]);
    });
});
