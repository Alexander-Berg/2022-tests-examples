jest.mock('auto-core/react/lib/cookie');

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import cookie from 'auto-core/react/lib/cookie';

import SafeDealParanjaPopup from './SafeDealParanjaPopup';

const Context = createContextProvider(contextMock);

const onClose = jest.fn();

function renderComponent() {
    return shallow(
        <Context>
            <SafeDealParanjaPopup visible onPopupClose={ onClose }/>
        </Context>,
    ).dive();
}

it('правильно сформирует ссылку в кнопке подробнее', () => {
    const wrapper = renderComponent();
    expect(wrapper.find('Button').at(0).prop('url')).toEqual('link/safe-deal-promo/?');
});

it('просто поставит куки при клике на кнопку подробнее', () => {
    const wrapper = renderComponent();
    wrapper.find('Button').at(0).simulate('click');

    expect(cookie.setForever).toHaveBeenCalledWith('safe_deal_promo', '-1');
    expect(onClose).toHaveBeenCalledTimes(0);
});

it('поставит в куки -1 и закроет попап при клике на кнопку понятно', () => {

    const wrapper = renderComponent();
    wrapper.find('Button').at(1).simulate('click');

    expect(cookie.setForever).toHaveBeenCalledWith('safe_deal_promo', '-1');
    expect(onClose).toHaveBeenCalledTimes(1);
});

describe('правильно отправляет метрику', () => {
    it('при показе', () => {
        jest.spyOn(React, 'useEffect').mockImplementationOnce(f => f());
        renderComponent();
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'safe_deal_paranja', 'show' ]);
    });
    it('при клике на подробнее', () => {
        const wrapper = renderComponent();
        wrapper.find('Button').at(0).dive().simulate('click');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'safe_deal_paranja', 'more_button', 'click' ]);
    });
    it('при клике на понятно', () => {
        const wrapper = renderComponent();
        wrapper.find('Button').at(1).simulate('click');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'safe_deal_paranja', 'close_button', 'click' ]);
    });
});
