jest.mock('auto-core/lib/event-log/statApi');

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import userWithoutAuthMock from 'auto-core/react/dataDomain/user/mocks/withoutAuth.mock';
import reportsBundlesMock from 'auto-core/react/dataDomain/reportsBundles/mocks/reportsBundles.mock.json';
import reportsBundlesWithQuotaMock from 'auto-core/react/dataDomain/reportsBundles/mocks/reportsBundlesWithQuota10.mock.json';
import reportsBundlesWithQuotaAndPurchaseMock from 'auto-core/react/dataDomain/reportsBundles/mocks/reportsBundlesWithQuota5.mock.json';

import HistoryByVinPackagePromoMobile from './HistoryByVinPackagePromoMobile';

const mockFunction = () => {};

const store = mockStore({
    user: userWithoutAuthMock,
});

it('правильно отправляет метрику и эвент-лог на маунт, если это промо', () => {
    shallow(
        <HistoryByVinPackagePromoMobile reportsBundles={ reportsBundlesMock } sendEventsToMarketing={ mockFunction }/>,
        { context: { ...contextMock, store } },
    );

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'history_purchase', 'show_promo' ]);
    expect(statApi.logImmediately).toHaveBeenCalledTimes(2);
    expect(statApi.logImmediately).toHaveBeenNthCalledWith(1, {
        vas_show_event: {
            context_page: 'PAGE_PROAUTO',
            context_block: 'BLOCK_REPORT_PACKAGE_PROMO_HORIZONTAL',
            context_service: 'SERVICE_AUTORU',
            product: 'REPORTS_5',
            base_price: 59900,
            effective_price: 59900,
        },
    });
    expect(statApi.logImmediately).toHaveBeenNthCalledWith(2, {
        vas_show_event: {
            context_page: 'PAGE_PROAUTO',
            context_block: 'BLOCK_REPORT_PACKAGE_PROMO_HORIZONTAL',
            context_service: 'SERVICE_AUTORU',
            product: 'REPORTS_10',
            base_price: 99000,
            effective_price: 99000,
        },
    });
});

it('правильно отправляет метрику на маунт, если это остаток пакета', () => {
    shallow(
        <HistoryByVinPackagePromoMobile reportsBundles={ reportsBundlesWithQuotaMock } sendEventsToMarketing={ mockFunction }/>,
        { context: { ...contextMock, store } },
    );

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'history_purchase', 'show_quota' ]);
});

it('прокидывает кнопке правильный from', () => {
    const wrapper = shallow(
        <HistoryByVinPackagePromoMobile reportsBundles={ reportsBundlesWithQuotaAndPurchaseMock } sendEventsToMarketing={ mockFunction }/>,
        { context: { ...contextMock, store } },
    );
    expect(wrapper.find('Connect(VinHistoryPaywallButton)').props()).toHaveProperty('from', 'api_m_vincheck_bundle');
});

it('отправляет правильную метрику и эвент-лог на клик', () => {
    const wrapper = shallow(
        <HistoryByVinPackagePromoMobile reportsBundles={ reportsBundlesMock } sendEventsToMarketing={ mockFunction }/>,
        { context: { ...contextMock, store } },
    );

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledTimes(2);

    wrapper.find('Connect(VinHistoryPaywallButton)[purchaseCount="5"]').simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenNthCalledWith(2, [ 'history_purchase', 'click_package5_button' ]);
    expect(statApi.logImmediately).toHaveBeenCalledTimes(3);
    expect(statApi.logImmediately).toHaveBeenNthCalledWith(3, {
        vas_click_event: {
            context_page: 'PAGE_PROAUTO',
            context_block: 'BLOCK_REPORT_PACKAGE_PROMO_HORIZONTAL',
            context_service: 'SERVICE_AUTORU',
            product: 'REPORTS_5',
            base_price: 59900,
            effective_price: 59900,
        },
    });

    wrapper.find('Connect(VinHistoryPaywallButton)[purchaseCount="10"]').simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(3);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenNthCalledWith(3, [ 'history_purchase', 'click_package10_button' ]);
    expect(statApi.logImmediately).toHaveBeenCalledTimes(4);
    expect(statApi.logImmediately).toHaveBeenNthCalledWith(4, {
        vas_click_event: {
            context_page: 'PAGE_PROAUTO',
            context_block: 'BLOCK_REPORT_PACKAGE_PROMO_HORIZONTAL',
            context_service: 'SERVICE_AUTORU',
            product: 'REPORTS_10',
            base_price: 99000,
            effective_price: 99000,
        },
    });
});
