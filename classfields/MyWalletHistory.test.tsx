jest.mock('www-desktop-lk/react/dataDomain/payments/actions/fetchPayments');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import fetchPayments from 'www-desktop-lk/react/dataDomain/payments/actions/fetchPayments';
import { paymentStateMock, paymentMock } from 'www-desktop-lk/react/dataDomain/payments/mocks';

import MyWalletHistory from './MyWalletHistory';
import type { AppState } from './MyWalletHistory';

let initialState: AppState;

const fetchPaymentsMock = fetchPayments as jest.MockedFunction<typeof fetchPayments>;
fetchPaymentsMock.mockImplementationOnce(() => () => {});

beforeEach(() => {
    initialState = {
        payments: paymentStateMock.withPayments([ paymentMock.value() ]).value(),
    };
});

it('при клике на страницу запросит данные с корректными параметрами', () => {
    const page = shallowRenderComponent({ initialState });

    const pagination = page.find('ListingPagination');
    pagination.simulate('click', 5);

    expect(fetchPaymentsMock).toHaveBeenCalledTimes(1);
    expect(fetchPaymentsMock).toHaveBeenCalledWith({ page: 5, page_size: 10 });
});

it('правильно формирует ссылку на оффер', () => {
    const page = shallowRenderComponent({ initialState });
    const link = page.find('Link');

    expect(link.prop('url')).toBe('link/internal-card-redirect/?sale_id=16784443-9a3954b8');
    expect(link.prop('target')).toBe('_blank');
    expect(link.prop('metrika')).toBe('go_to_card');
});

function shallowRenderComponent({ initialState }: { initialState: AppState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <MyWalletHistory/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
