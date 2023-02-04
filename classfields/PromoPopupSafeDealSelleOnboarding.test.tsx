jest.mock('auto-core/react/dataDomain/cookies/actions/set', () => {
    return jest.fn(() => () => ({}));
});

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import mockStore from 'autoru-frontend/mocks/mockStore';

import setCookie from 'auto-core/react/dataDomain/cookies/actions/set';

import PromoPopupSafeDealSellerOnboarding from './PromoPopupSafeDealSellerOnboarding';

const Context = createContextProvider(contextMock);

const defaultState = {
    autoPopup: {},
};

function renderComponent() {
    const store = mockStore(defaultState);

    return shallow(
        <Context>
            <Provider store={ store }>
                <PromoPopupSafeDealSellerOnboarding/>
            </Provider>
        </Context>,
    ).dive().dive().dive();
}

it('правильно сформирует ссылку в кнопке подробнее', () => {
    const wrapper = renderComponent();
    expect(wrapper.find('Button').at(0).prop('url')).toEqual('link/safe-deal-promo/?');
});

it('поставит в куки -1 и закроет попап', () => {

    const wrapper = renderComponent();
    wrapper.find('Button').at(1).simulate('click');

    expect(setCookie).toHaveBeenCalledTimes(1);
    expect(setCookie).toHaveBeenCalledWith('safe_deal_seller_onboarding_promo', '-1', { expires: 30 });
});
