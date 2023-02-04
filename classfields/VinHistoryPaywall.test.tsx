jest.mock('auto-core/react/actions/card', () => ({
    openHistoryPaymentModalDesktop: jest.fn(() => () => {}),
}));
jest.mock('auto-core/lib/event-log/statApi');
jest.mock(('auto-core/react/dataDomain/state/actions/buyVinReportWithQuota'), () => jest.fn(() => () => {}));
jest.mock(('auto-core/react/dataDomain/state/actions/authModalOpen'), () => jest.fn(() => () => {}));
jest.mock(('auto-core/react/dataDomain/state/actions/sellerPopupClose'), () => jest.fn(() => () => {}));

import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import { InView } from 'react-intersection-observer';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import configStateMock from 'auto-core/react/dataDomain/config/mock';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import userWithAuthMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import userWithoutAuthMock from 'auto-core/react/dataDomain/user/mocks/withoutAuth.mock';
import authModalOpen from 'auto-core/react/dataDomain/state/actions/authModalOpen';
import cardActions from 'auto-core/react/actions/card';
import buyVinReportWithQuota from 'auto-core/react/dataDomain/state/actions/buyVinReportWithQuota';
import sellerPopupClose from 'auto-core/react/dataDomain/state/actions/sellerPopupClose';

import type { TBillingFrom } from 'auto-core/types/TBilling';

import VinHistoryPaywallButton from './VinHistoryPaywallButton';
import VinHistoryPaywallLink from './VinHistoryPaywallLink';
import type { AppState, VinHistoryPaywallProps } from './VinHistoryPaywall';

const openHistoryPaymentModalDesktop = cardActions.openHistoryPaymentModalDesktop as jest.MockedFunction<typeof cardActions.openHistoryPaymentModalDesktop>;
const logImmediately = statApi.logImmediately as jest.MockedFunction<typeof statApi.logImmediately>;

let props: VinHistoryPaywallProps;
let initialState: AppState;

const context = {
    ...contextMock,
    logVasEvent: jest.fn(),
};

beforeEach(() => {
    initialState = {
        card: _.cloneDeep(cardMock),
        user: _.cloneDeep(userWithAuthMock),
        config: configStateMock.value(),
        state: { authModal: {} },
        router: {
            current: {
                data: {
                    components: [ 'lol' ],
                },
                name: 'route',
                params: {},
                url: 'https://auto.ru',
            },
            state: 'LOADED',
        },
        searchID: {
            searchID: '',
            parentSearchId: undefined,
        },
    };

    context.logVasEvent.mockClear();

    openHistoryPaymentModalDesktop.mockClear();
});

[
    { Component: VinHistoryPaywallButton, componentName: 'Button', authComponentName: 'Connect(AuthButton)' },
    { Component: VinHistoryPaywallLink, componentName: 'Link', authComponentName: 'Connect(AuthLink)' },
].forEach(({ authComponentName, Component, componentName }) => {

    function shallowRenderComponent(store: AppState = initialState) {
        const wrapper = shallow(
            <Component { ...props }>text</Component>,
            { context: { ...context, store: mockStore(store) } },
        );

        return wrapper.dive();
    }

    describe(componentName + ', если нужно авторизоваться и заплатить', () => {
        beforeEach(() => {
            initialState = {
                card: _.cloneDeep(cardMock),
                user: _.cloneDeep(userWithoutAuthMock),
                config: configStateMock.value(),
                state: { authModal: {} },
                router: {
                    current: {
                        data: {
                            components: [ 'lol' ],
                        },
                        name: 'route',
                        params: {},
                        url: 'https://auto.ru',
                    },
                    state: 'LOADED',
                },
                searchID: {
                    searchID: '',
                    parentSearchId: undefined,
                },
            };
            props = {
                onClick: jest.fn(),
                from: 'offers-history-block' as TBillingFrom,
                name: 'jest',
                price: 99,
                originalPrice: 197,
                needPay: true,
                purchaseCount: '11',
                vinReportPaymentParams: { offerId: '123' },
            };
        });
        let page;

        it('правильно вызовет модал авторизации', () => {
            page = shallowRenderComponent();
            const authComponent = page.find(authComponentName);
            authComponent.simulate('click', { preventDefault: () => {} });

            expect(authModalOpen).toHaveBeenCalledTimes(1);
            expect(authModalOpen).toHaveBeenCalledWith({
                screen: 'auth',
                paymentParams: {
                    product: [
                        {
                            count: 1,
                            name: 'offers-history-reports',
                        },
                    ],
                    purchaseCount: 11,
                    from: 'offers-history-block',
                    category: 'cars',
                    section: 'used',
                    offerId: '123',
                    forceShowBundleSelector: undefined,
                },
            });
        });

        it('правильно отправит событие показа если кнопка попала в поле видимости', () => {
            page = shallowRenderComponent();
            const observer = page.find(InView);
            observer.simulate('change', true);

            expect(context.logVasEvent).toHaveBeenCalledTimes(1);
            expect(context.logVasEvent).toHaveBeenCalledWith({
                event: 'VAS_SHOW',
                from: 'offers-history-block',
                originalPrice: 197,
                price: 99,
                serviceId: 'offers-history-reports-11',
            });
        });

        it('правильно отправит событие клика', () => {
            page = shallowRenderComponent();
            const authComponent = page.find(authComponentName);
            authComponent.simulate('click', { preventDefault: () => {} });

            expect(context.logVasEvent).toHaveBeenCalledTimes(1);
            expect(context.logVasEvent).toHaveBeenCalledWith({
                event: 'VAS_CLICK',
                from: 'offers-history-block',
                originalPrice: 197,
                price: 99,
                serviceId: 'offers-history-reports-11',
            });
        });
    });

    describe(componentName + ', если не нужно авторизовываться а нужно только заплатить', () => {
        beforeEach(() => {
            props = {
                onClick: jest.fn(),
                from: 'offers-history-block' as TBillingFrom,
                name: 'jest',
                price: 99,
                originalPrice: 197,
                needPay: true,
                purchaseCount: '11',
                forceShowBundleSelector: true,
                vinReportPaymentParams: { offerId: '123' },
            };

            logImmediately.mockClear();
        });

        let page;

        it('правильно отправит событие показа если кнопка попала в поле видимости', () => {
            page = shallowRenderComponent();
            const observer = page.find(InView);
            observer.simulate('change', true);

            expect(context.logVasEvent).toHaveBeenCalledTimes(1);
            expect(context.logVasEvent).toHaveBeenCalledWith({
                event: 'VAS_SHOW',
                from: 'offers-history-block',
                originalPrice: 197,
                price: 99,
                serviceId: 'offers-history-reports-11',
            });
        });

        it('не отправит событие показа если кнопка вне поля видимости', () => {
            page = shallowRenderComponent();
            const observer = page.find(InView);
            observer.simulate('change', false);

            expect(context.logVasEvent).toHaveBeenCalledTimes(0);
        });

        describe('при клике', () => {
            it('правильно отправит событие клика', () => {
                page = shallowRenderComponent();
                const observer = page.find(InView);
                observer.simulate('change', true);
                const component = page.find(componentName);
                component.simulate('click');

                expect(context.logVasEvent).toHaveBeenCalledTimes(2);
                expect(context.logVasEvent).toHaveBeenNthCalledWith(1, {
                    event: 'VAS_SHOW',
                    from: 'offers-history-block',
                    originalPrice: 197,
                    price: 99,
                    serviceId: 'offers-history-reports-11',
                });
                expect(context.logVasEvent).toHaveBeenNthCalledWith(2, {
                    event: 'VAS_CLICK',
                    from: 'offers-history-block',
                    originalPrice: 197,
                    price: 99,
                    serviceId: 'offers-history-reports-11',
                });

                // event-log
                expect(logImmediately).toHaveBeenCalledTimes(1);
                expect(logImmediately).toHaveBeenCalledWith({
                    paid_report_view_event: {
                        card_from: 'SERP',
                        card_id: '1085562758-1970f439',
                        category: 'CARS',
                        context_block: 'BLOCK_UNDEFINED',
                        context_page: 'PAGE_UNDEFINED',
                        search_query_id: '',
                        section: 'USED',
                        self_type: 'TYPE_SINGLE',
                        trade_in_allowed: false,
                    },
                });
            });

            it('правильно вызовет модал оплаты', () => {
                page = shallowRenderComponent();
                const observer = page.find(InView);
                observer.simulate('change', true);
                const component = page.find(componentName);
                component.simulate('click');

                expect(openHistoryPaymentModalDesktop).toHaveBeenCalledTimes(1);
                expect(openHistoryPaymentModalDesktop).toHaveBeenCalledWith({
                    offerId: '123',
                    from: props.from,
                    purchaseCount: 11,
                    platform: 'PLATFORM_DESKTOP',
                    forceShowBundleSelector: true,
                });
            });

            it('скроет модал показа телефона, перед показом модала оплаты, если тот открыт', () => {
                const customState = {
                    ...initialState,
                    state: { authModal: {}, sellerPopup: { isOpened: true } },
                };
                page = shallowRenderComponent(customState);
                const observer = page.find(InView);
                observer.simulate('change', true);
                const component = page.find(componentName);
                component.simulate('click');

                expect(sellerPopupClose).toHaveBeenCalledTimes(1);
            });
        });

        it('при клике на кнопку правильно запросит покупку по квоте и отправит события в логи', () => {
            props.quota = 5;
            page = shallowRenderComponent();
            const observer = page.find(InView);
            observer.simulate('change', true);
            const component = page.find(componentName);
            component.simulate('click');

            expect(buyVinReportWithQuota).toHaveBeenCalledTimes(1);
            expect(context.logVasEvent).toHaveBeenCalledTimes(2);
            expect(context.logVasEvent.mock.calls).toEqual([
                [ { event: 'VAS_SHOW', from: 'offers-history-block', originalPrice: 197, price: 99, serviceId: 'offers-history-reports-quota' } ],
                [ { event: 'VAS_CLICK', from: 'offers-history-block', originalPrice: 197, price: 99, serviceId: 'offers-history-reports-quota' } ],
            ]);

            // event-log
            expect(logImmediately).toHaveBeenCalledTimes(2);
            expect(logImmediately).toHaveBeenNthCalledWith(1, {
                paid_report_view_event: {
                    card_from: 'SERP',
                    card_id: '1085562758-1970f439',
                    category: 'CARS',
                    context_block: 'BLOCK_UNDEFINED',
                    context_page: 'PAGE_UNDEFINED',
                    search_query_id: '',
                    section: 'USED',
                    self_type: 'TYPE_SINGLE',
                    trade_in_allowed: false,
                },
            });
            expect(logImmediately).toHaveBeenNthCalledWith(2, {
                vas_spend_quota_event: {
                    category: 'CARS',
                    product: 'REPORTS_QUOTA',
                    context_page: 'PAGE_UNDEFINED',
                    context_block: 'BLOCK_UNDEFINED',
                },
            });
        });
    });
});
