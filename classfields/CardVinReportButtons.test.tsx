jest.mock('auto-core/lib/event-log/statApi');

import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import { ContextBlock, ContextPage } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';
import { ReportType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';

import { TBillingFrom } from 'auto-core/types/TBilling';

import CardVinReportButtons, { PRESETS } from './CardVinReportButtons';

const context = _.cloneDeep(contextMock);
const logImmediately = statApi.logImmediately as jest.MockedFunction<typeof statApi.logImmediately>;

const REQUIRED_PROPS = {
    contextPage: ContextPage.PAGE_UNDEFINED,
    contextBlock: ContextBlock.BLOCK_UNDEFINED,
    from: TBillingFrom.DESKTOP_DEFAULT,
    metrikaParam: 'test',
};

beforeEach(() => {
    context.metrika.sendPageEvent.mockClear();
});

describe('если отчёт оплачен', () => {
    const cardVinReportPaid = _.cloneDeep(cardVinReportFree);
    cardVinReportPaid.report!.report_type = ReportType.PAID_REPORT;
    const wrapper = shallow(
        <CardVinReportButtons
            { ...REQUIRED_PROPS }
            vinReport={ cardVinReportPaid }
            isAuth
        />, { context },
    );

    it('должен правильно рисовать кнопку полного отчёта', () => {
        const fullReportButtons = wrapper.find('.CardVinReportButtons');
        expect(fullReportButtons.find('Button').prop('url')).toEqual('link/proauto-report/?history_entity_id=1084368429-e9a4c888');
        expect(fullReportButtons.find('Button').prop('children')).toEqual('Посмотреть полный отчёт');
    });

    it('должен отправить метрику click_go_to_standalone и логи на клик', () => {
        wrapper.find('Button').simulate('click');
        const expectedResult = [ 'test', 'click_go_to_standalone' ];
        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);

        expect(logImmediately).toHaveBeenCalledTimes(1);
        expect(logImmediately).toHaveBeenNthCalledWith(1, {
            vas_click_navig_event: {
                category: 'CARS',
                product: 'REPORTS',
                context_page: 'PAGE_UNDEFINED',
                context_block: 'BLOCK_UNDEFINED',
            },
        });
    });
});

describe('если есть квота', () => {
    const cardVinReportWithQuota = _.cloneDeep(cardVinReportFree);
    cardVinReportWithQuota.billing!.quota_left = 5;
    const wrapper = shallow(
        <CardVinReportButtons
            { ...REQUIRED_PROPS }
            vinReport={ cardVinReportWithQuota }
            isAuth
        />, { context },
    );

    it('должен правильно рисовать кнопку полного отчёта', () => {
        const fullReportButtons = wrapper.find('VinReportActionButton');
        expect(fullReportButtons).toHaveLength(1);
        expect(fullReportButtons).toHaveProp('servicePrice', undefined);
    });

    it('должен отправить метрику click_quota_button на клик', () => {
        wrapper.find('VinReportActionButton').simulate('click');
        const expectedResult = [ 'test', 'click_quota_button', 'not_owner' ];
        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });
});

describe('если нет квоты, но есть пакет', () => {
    const renderComponent = (props = {}) => {
        const store = mockStore({ state: {} });
        return shallow(
            <CardVinReportButtons
                { ...REQUIRED_PROPS }
                vinReport={ cardVinReportFree }
                isAuth={ false }
                { ...props }
            />, { context: { ...contextMock, store } },
        );
    };

    it('по умолчанию столько кнопок, сколько пришло сервисов', () => {
        const wrapper = renderComponent();
        const fullReportButtons = wrapper.find('VinReportActionButton');

        expect(fullReportButtons).toHaveLength(3);
        expect(fullReportButtons.at(0)).toHaveProp('servicePrice', {
            service: 'offers-history-reports',
            price: 500,
            original_price: 999,
            currency: 'RUR',
            paid_reason: 'FREE_LIMIT',
            recommendation_priority: 0,
            need_confirm: false,
            counter: '10',
        });
        expect(fullReportButtons.at(1)).toHaveProp('servicePrice', {
            service: 'offers-history-reports',
            price: 1999,
            original_price: 2999,
            currency: 'RUR',
            paid_reason: 'FREE_LIMIT',
            recommendation_priority: 0,
            need_confirm: false,
            counter: '50',
        });
        expect(fullReportButtons.at(2)).toHaveProp('servicePrice', {
            service: 'offers-history-reports',
            price: 99,
            original_price: 197,
            currency: 'RUR',
            paid_reason: 'FREE_LIMIT',
            recommendation_priority: 0,
            need_confirm: false,
            counter: '1',
        });
    });

    it('preset=PROMO', () => {
        const wrapper = renderComponent({ preset: PRESETS.PROMO });
        const fullReportButtons = wrapper.find('VinReportActionButton');

        expect(fullReportButtons).toHaveLength(2);
        // Первая кнопка без serivePrice - тогда берется по умолчанию 1 отчет
        expect(fullReportButtons.at(0)).toHaveProp('servicePrice', undefined);
        // Вторая кнопка должна продавать самый большой отчет
        expect(fullReportButtons.at(1)).toHaveProp('servicePrice', {
            service: 'offers-history-reports',
            price: 1999,
            original_price: 2999,
            currency: 'RUR',
            paid_reason: 'FREE_LIMIT',
            recommendation_priority: 0,
            need_confirm: false,
            counter: '50',
        });
    });

    it('preset=MIN_PRICE', () => {
        const wrapper = renderComponent({ preset: PRESETS.MIN_PRICE });
        const fullReportButtons = wrapper.find('CardVinReportMinPriceButton');

        expect(fullReportButtons).toHaveLength(1);
    });

    it('preset=SINGLE_BUTTON_ONLY', () => {
        const wrapper = renderComponent({ preset: PRESETS.SINGLE_BUTTON_ONLY });
        const fullReportButtons = wrapper.find('VinReportActionButton');

        expect(fullReportButtons).toHaveLength(1);
        // Единственная кнопка должна цену за 1 отчет
        expect(fullReportButtons.at(0)).toHaveProp('servicePrice', {
            service: 'offers-history-reports',
            price: 99,
            original_price: 197,
            currency: 'RUR',
            paid_reason: 'FREE_LIMIT',
            recommendation_priority: 0,
            need_confirm: false,
            counter: '1',
        });
    });

    it('должен отправить метрику click_bundle_button на клик', () => {
        const wrapper = renderComponent({ isAuth: true });
        wrapper.find('VinReportActionButton').first().simulate('click');
        const expectedResult = [ 'test', 'click_bundle_button', 'not_owner' ];
        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });
});

describe('при авторизации', () => {
    const renderButtons = () => shallow(
        <CardVinReportButtons
            { ...REQUIRED_PROPS }
            vinReport={ cardVinReportFree }
            isAuth={ false }
        />, { context },
    );

    it('должен прокидывать инфу про авторизацию в кнопки покупки отчёта', () => {
        type Props = { needAuth?: boolean };

        const wrapper = renderButtons();
        expect((wrapper.find('VinReportActionButton').at(0).props() as Props).needAuth).toBe(true);
        expect((wrapper.find('VinReportActionButton').at(1).props() as Props).needAuth).toBe(true);
        wrapper.setProps({ isAuth: true });
        expect((wrapper.find('VinReportActionButton').at(0).props() as Props).needAuth).toBe(false);
        expect((wrapper.find('VinReportActionButton').at(1).props() as Props).needAuth).toBe(false);
    });

    it('сначала должен отправить метрику no_login на клик', () => {
        const wrapper = renderButtons();
        wrapper.find('VinReportActionButton').first().simulate('click');
        const expectedResult = [ 'test', 'click_bundle_button', 'no_login' ];
        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });

    it('потом должен отправить метрику owner на клик', () => {
        const wrapper = renderButtons();
        wrapper.setProps({ isAuth: true, isOwner: true });
        wrapper.find('VinReportActionButton').first().simulate('click');
        const expectedResult = [ 'test', 'click_bundle_button', 'owner' ];
        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });
});

describe('если нет квоты и нет покета', () => {
    const cardVinReportNoBundle = _.cloneDeep(cardVinReportFree);
    cardVinReportNoBundle.billing!.service_prices.splice(1, 2);
    const wrapper = shallow(
        <CardVinReportButtons
            { ...REQUIRED_PROPS }
            vinReport={ cardVinReportNoBundle }
            isAuth
        />, { context },
    );

    it('должен правильно рисовать 1 кнопку покупки отчёта', () => {
        const fullReportButtons = wrapper.find('VinReportActionButton');
        expect(fullReportButtons).toHaveLength(1);
        expect(fullReportButtons.at(0)).toHaveProp('servicePrice', undefined);
        expect(fullReportButtons.at(0)).toHaveProp('withSummary', false);
    });

    it('должен отправить метрику click_single_button на клик', () => {
        wrapper.find('VinReportActionButton').simulate('click');
        const expectedResult = [ 'test', 'click_single_button', 'not_owner' ];
        expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
        expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
    });

    it('должен отправить цель на клик', () => {
        wrapper.find('VinReportActionButton').simulate('click');
        expect(context.metrika.reachGoal).toHaveBeenCalledWith('CLICK_BUY_REPORT');
    });
});
