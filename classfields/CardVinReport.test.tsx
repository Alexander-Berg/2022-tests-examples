/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/actions/scroll', () => jest.fn());
jest.mock('auto-core/lib/event-log/statApi');
jest.mock('auto-core/react/dataDomain/state/actions/authModalClose', () => jest.fn());
jest.mock('auto-core/react/dataDomain/state/actions/authModalOpen', () => jest.fn());

import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import { ReportType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

// Mocks
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import type { StateVinReportData } from 'auto-core/react/dataDomain/vinReport/types';
import type Button from 'auto-core/react/components/islands/Button/Button';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';
import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import userWithAuthMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import userWithoutAuthMock from 'auto-core/react/dataDomain/user/mocks/withoutAuth.mock';
import authModalClose from 'auto-core/react/dataDomain/state/actions/authModalClose';
import authModalOpen from 'auto-core/react/dataDomain/state/actions/authModalOpen';

import { TBillingFrom } from 'auto-core/types/TBilling';

import type { Props } from './CardVinReport';
import CardVinReport from './CardVinReport';

type CardVinReportState = {
    showReportAfterLogin: boolean;
    showReport: boolean;
}

const REQUIRED_PROPS: Props = {
    from: TBillingFrom.DESKTOP_CARD,
    saleFrom: TBillingFrom.DESKTOP_CARD,
    unAuthFrom: TBillingFrom.DESKTOP_CARD,
    vinReport: cardVinReportFree,
};

const state = {
    bunker: {},
    card: offer,
    config: configStateMock.withPageType('card').value(),
    state: {},
    user: userWithAuthMock,
};

const createStore = (customState = {}) => mockStore({ ...state, ...customState });

let context: typeof contextMock;
let store: any;

beforeEach(() => {
    context = _.cloneDeep(contextMock);
    store = createStore();
});

const renderComponent = (props: Props, customStore?: any) => {
    return shallow(
        <CardVinReport { ...props }/>,
        { context: { ...context, store: customStore || store } },
    ).dive().dive();
};

it('должен развернуть отчёт по клику на "показать отчёт" для авторизованного юзера', () => {
    const wrapper = renderComponent(REQUIRED_PROPS);
    wrapper.find('ForwardRef(CardVinReportTemplate)').dive()
        .find('CardVinReportCompactContent').dive().find('Button').simulate('click');
    expect((wrapper.state() as CardVinReportState).showReport).toBe(true);
});

it('должен показывать наличие бесплатного отчета', () => {
    const wrapper = renderComponent(REQUIRED_PROPS);
    const button = wrapper.find('ForwardRef(CardVinReportTemplate)').dive()
        .find('CardVinReportCompactContent').dive().find('Button');
    expect(button.exists()).toBe(true);
});

it('не должен показывать бесплатный отчет, если нет бесплатных пунктов', () => {
    const cardVinReport = _.cloneDeep(cardVinReportFree);
    cardVinReport.report!.content!.items.forEach(item => {
        item.available_for_free = false;
    });

    const wrapper = renderComponent({ ...REQUIRED_PROPS, vinReport: cardVinReport });

    const freeItemElements = wrapper.find('ForwardRef(CardVinReportTemplate)').dive().find('CardVinReportCompactContent').dive().find('VinReportFreeBlockItem');
    expect(freeItemElements.exists()).toBe(false);
});

it('не должен показывать бесплатный отчёт, если передан проп showFreeBlock = false', () => {
    const wrapper = renderComponent({ ...REQUIRED_PROPS, showFreeBlock: false });
    const button = wrapper.find('ForwardRef(CardVinReportTemplate)').dive().find('CardVinReportCompactContent').dive().find('Button');
    expect(button.exists()).toBe(false);
});

it('должен открыть модал авторизации по клику на "показать отчёт" для неавторизованного юзера', () => {
    const wrapper = renderComponent(REQUIRED_PROPS, createStore({ user: userWithoutAuthMock }));
    wrapper.setProps({ dispatch: () => {} } as any);
    wrapper.find('ForwardRef(CardVinReportTemplate)').dive().find('CardVinReportCompactContent').dive().find('Button').simulate('click');
    expect(authModalOpen).toHaveBeenCalledWith({ screen: 'auth' });
    expect((wrapper.state() as CardVinReportState).showReport).toBe(false);
});

it('должен передавать авторизацию в кнопки покупки отчёта', () => {
    const wrapper = renderComponent({ ...REQUIRED_PROPS });
    wrapper.find('ForwardRef(CardVinReportTemplate)').dive().find('CardVinReportCompactContent').dive().find('Button').simulate('click');
    expect((wrapper.find('ForwardRef(CardVinReportTemplate)').dive()
        .find('CardVinReportButtons').props() as { isAuth: boolean }).isAuth).toBe(true);
});

it('не должен закрыть модал авторизации после успешной авторизации, если это не просмотр бесплатного отчёта', () => {
    const wrapper = renderComponent(REQUIRED_PROPS, createStore({ user: userWithoutAuthMock }));

    wrapper.setProps({ isAuth: true } as any);
    expect(store.getActions()).toEqual([]);
    expect((wrapper.state() as CardVinReportState).showReport).toBe(false);
});

it('должен закрыть модал авторизации и показать отчёт после успешной авторизации', () => {
    const wrapper = renderComponent(REQUIRED_PROPS, createStore({ user: userWithoutAuthMock }));

    wrapper.setState({ showReportAfterLogin: true });
    wrapper.setProps({ isAuth: true, dispatch: () => {} } as any);
    expect(authModalClose).toHaveBeenCalled();
    expect((wrapper.state() as CardVinReportState).showReport).toBe(true);
});

it('должен менять стейт по клику на "скрыть отчёт"', () => {
    const wrapper = renderComponent(REQUIRED_PROPS, createStore({ user: userWithoutAuthMock }));
    wrapper.setState({ showReport: true });
    wrapper.find('ForwardRef(CardVinReportTemplate)').dive().find('SpoilerLink').simulate('click');
    expect((wrapper.state() as CardVinReportState).showReport).toBe(false);
});

it('не должен перерисовать компонент после покупки отчёта', () => {
    const cardVinReportPaid = _.cloneDeep(cardVinReportFree);
    cardVinReportPaid.report!.report_type = ReportType.PAID_REPORT;

    const wrapper = renderComponent(REQUIRED_PROPS, createStore({ user: userWithoutAuthMock, state: { tryToBuyReport: true } }));
    expect((wrapper.instance() as any).shouldComponentUpdate({ vinReport: cardVinReportPaid })).toBe(false);
});

it('должен перерисовать компонент после смены статуса, если это не покупка', () => {
    const cardVinReportPaid = _.cloneDeep(cardVinReportFree);
    cardVinReportPaid.report!.report_type = ReportType.PAID_REPORT;

    const wrapper = renderComponent(REQUIRED_PROPS, createStore({ user: userWithoutAuthMock }));
    expect((wrapper.instance() as any).shouldComponentUpdate({ vinReport: cardVinReportPaid })).toBe(true);
});

it('показываем превью платного, если отчёт оплачен', () => {
    const cardVinReportPaid = _.cloneDeep(cardVinReportFree);
    cardVinReportPaid.report!.report_type = ReportType.PAID_REPORT;
    delete cardVinReportPaid.billing;

    const wrapper = renderComponent({ ...REQUIRED_PROPS, vinReport: cardVinReportPaid }, createStore({ user: userWithoutAuthMock }));

    expect(
        wrapper.find('ForwardRef(CardVinReportTemplate)').dive().find('CardVinReportCompactContent')
            .dive().find('.CardVinReportCompactContent__buttonShowFree'),
    ).toExist();
});

it('должен развернуть бесплатный отчет и вызвать про это экшн, если в стейте showFreeReport', () => {
    const wrapper = renderComponent(REQUIRED_PROPS);
    wrapper.setProps({ showFreeReport: true } as any);
    expect(store.getActions()).toEqual([ { type: 'HAS_SHOW_FREE_REPORT' } ]);
    expect((wrapper.state() as CardVinReportState).showReport).toBe(true);
});

it('должен отправить событие paid_report_view_event во фронтлог', () => {
    const wrapper = renderComponent(REQUIRED_PROPS);

    (wrapper
        .find('ForwardRef(CardVinReportTemplate)').dive()
        .find('CardVinReportButtons').dive()
        .find('CardVinReportMinPriceButton').dive()
        .findWhere(node => node.prop('name') === 'CardVinReportSingleButton').dive().dive()
        .instance() as unknown as Button).onClick();

    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({
        paid_report_view_event: {
            card_from: 'SERP',
            card_id: '1085562758-1970f439',
            category: 'CARS',
            context_block: 'BLOCK_CARD',
            context_page: 'PAGE_CARD',
            search_query_id: '',
            section: 'USED',
            self_type: 'TYPE_SINGLE',
            trade_in_allowed: false,
        },
    });
});

describe('кнопка покупки отчета', () => {
    it('должен отрендерить CardVinReportButtonsUnauthorized для неавторизованного юзера', () => {
        const wrapper = renderComponent(REQUIRED_PROPS, createStore({ user: userWithoutAuthMock }));

        expect(wrapper.find('ForwardRef(CardVinReportTemplate)').dive().find('CardVinReportButtonsUnauthorized').exists()).toBe(true);
    });

    it('не должен отрендерить CardVinReportButtonsUnauthorized для авторизованного юзера', () => {
        const wrapper = renderComponent(REQUIRED_PROPS);

        expect(wrapper.find('ForwardRef(CardVinReportTemplate)').dive().find('CardVinReportButtonsUnauthorized').exists()).toBe(false);
    });
});

describe('распродажа отчетов VinReportSalePromo получает vinReportPaymentParams', () => {
    it('на карточке', () => {
        const wrapper = renderComponent(REQUIRED_PROPS, createStore({ user: userWithoutAuthMock }));

        const params = wrapper.find('Connect(VinReportSalePromo)').prop('vinReportPaymentParams') as StateVinReportData['paymentParams'];
        expect(params!.hasOwnProperty('offerId')).toBe(true);
    });

    it('в гараже', () => {
        const vinReportPaymentParams = { vin_or_license_plate: 'SKRRT' };
        const wrapper = renderComponent({ ...REQUIRED_PROPS, vinReportPaymentParams }, createStore({ user: userWithoutAuthMock }));

        const params = wrapper.find('Connect(VinReportSalePromo)').prop('vinReportPaymentParams') as StateVinReportData['paymentParams'];
        expect(params!.hasOwnProperty('vin_or_license_plate')).toBe(true);
    });
});

describe('метрики', () => {
    it('если в адресе нет action=showVinReport, отправит метрику о показе мини', () => {
        renderComponent(REQUIRED_PROPS);
        expect(context.metrika.sendPageEvent).toHaveBeenCalled();
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toStrictEqual([ 'history_report_mini', 'view', 'free', 'not_owner' ]);
    });

    // Заодно проверяем showReport и что там метрика отправляется при раскрытии бесплатного отчета
    it('если в адресе есть action=showVinReport, отправит метрику о показе', () => {
        const customStore = createStore({
            config: configStateMock.withPageParams({ action: 'showVinReport' }).value(),
        });

        renderComponent(REQUIRED_PROPS, customStore);

        expect(context.metrika.sendPageEvent).toHaveBeenCalled();
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toStrictEqual([ 'history_report_full', 'view', 'free', 'not_owner' ]);
    });

    describe('клик "показать бесплатный отчет"', () => {
        const renderWrapper = (props: Props, store?: any) => {
            const wrapper = renderComponent(props, store);

            wrapper.setProps({ dispatch: () => {} } as any);

            const button = wrapper.find('ForwardRef(CardVinReportTemplate)')
                .dive().find('CardVinReportCompactContent').dive().find('Button');

            button.simulate('click');

            return wrapper;
        };

        describe('на десктопе', () => {
            it('незалогин', () => {
                renderWrapper({ ...REQUIRED_PROPS }, createStore({ user: userWithoutAuthMock }));

                expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(2);
                expect(context.metrika.sendPageEvent.mock.calls[1][0]).toStrictEqual([ 'history_report_mini', 'click_show_free_report', 'no_login' ]);
            });

            it('не владелец', () => {
                renderWrapper(REQUIRED_PROPS);

                expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(3);
                expect(context.metrika.sendPageEvent.mock.calls[2][0]).toStrictEqual([ 'history_report_mini', 'click_show_free_report', 'not_owner' ]);
            });

            it('владелец', () => {
                renderWrapper({ ...REQUIRED_PROPS, isOwner: true });

                expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(3);
                expect(context.metrika.sendPageEvent.mock.calls[2][0]).toStrictEqual([ 'history_report_mini', 'click_show_free_report', 'owner' ]);
            });
        });
    });

    describe('отправка цели при клике на "показать бесплатный отчет"', () => {
        it('отправляет цель при клике на кнопку "показать бесплатный отчет"', () => {
            const wrapper = renderComponent(REQUIRED_PROPS).find('ForwardRef(CardVinReportTemplate)')
                .dive().find('CardVinReportCompactContent').dive().find('Button');

            wrapper.simulate('click');

            expect(context.metrika.reachGoal).toHaveBeenCalledWith('CLICK_FREE_REPORT_CARD');
        });
    });
});
