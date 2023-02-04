import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import userWithoutAuthMock from 'auto-core/react/dataDomain/user/mocks/withoutAuth.mock';
import reportsBundlesMock from 'auto-core/react/dataDomain/reportsBundles/mocks/reportsBundles.mock.json';
import reportsBundlesWithQuotaMock from 'auto-core/react/dataDomain/reportsBundles/mocks/reportsBundlesWithQuota10.mock.json';
import reportsBundlesWithQuotaAndPurchaseMock from 'auto-core/react/dataDomain/reportsBundles/mocks/reportsBundlesWithQuota5.mock.json';

import HistoryByVinPackagePromoMini from './HistoryByVinPackagePromoMini';

const mockFunction = () => {};

const store = mockStore({
    user: userWithoutAuthMock,
});

it('правильно отправляет метрику на маунт', () => {
    shallow(
        <HistoryByVinPackagePromoMini reportsBundles={ reportsBundlesMock } sendEventsToMarketing={ mockFunction }/>,
        { context: { ...contextMock, store } },
    );

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'history_purchase', 'show_promo' ]);
});

it('не рисует промо, если купить пакет нельзя', () => {
    const wrapper = shallow(
        <HistoryByVinPackagePromoMini reportsBundles={ reportsBundlesWithQuotaMock } sendEventsToMarketing={ mockFunction }/>,
        { context: { ...contextMock, store } },
    );

    expect(wrapper).toBeEmptyRender();
});

it('прокидывает кнопке правильный from', () => {
    const wrapper = shallow(
        <HistoryByVinPackagePromoMini reportsBundles={ reportsBundlesWithQuotaAndPurchaseMock } sendEventsToMarketing={ mockFunction }/>,
        { context: { ...contextMock, store } },
    );
    expect(wrapper.find('Connect(VinHistoryPaywallButton)').props()).toHaveProperty('from', 'vincheck_bundle_sidebar');
});

it('прокидывает кнопкам с пакетами правильный from', () => {
    const wrapper = shallow(
        <HistoryByVinPackagePromoMini reportsBundles={ reportsBundlesWithQuotaAndPurchaseMock } sendEventsToMarketing={ mockFunction }/>,
        { context: { ...contextMock, store } },
    );
    expect(wrapper.find('Connect(HistoryByVinPackagePurchaseHOC)').props()).toHaveProperty('from', 'vincheck_bundle_sidebar');
});

it('отправляет правильную метрику на клик', () => {
    const wrapper = shallow(
        <HistoryByVinPackagePromoMini reportsBundles={ reportsBundlesMock } sendEventsToMarketing={ mockFunction }/>,
        { context: { ...contextMock, store } },
    );

    wrapper.find('Connect(VinHistoryPaywallButton)[purchaseCount="5"]').simulate('click');
    wrapper.find('Connect(VinHistoryPaywallButton)[purchaseCount="10"]').simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenNthCalledWith(1, [ 'history_purchase', 'show_promo' ]);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenNthCalledWith(2, [ 'history_purchase', 'click_package5_button' ]);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenNthCalledWith(3, [ 'history_purchase', 'click_package10_button' ]);
});
