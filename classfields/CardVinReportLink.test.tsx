import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import userMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import reportMock from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-nissan.mock';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TBillingFrom } from 'auto-core/types/TBilling';

import CardVinReportLink from './CardVinReportLink';

let context: any;

beforeEach(() => {
    context = _.cloneDeep(contextMock);
});

describe('должен отправить метрику history_report,click_vin_promo_button_mobile', () => {
    const renderComponent = (offer = {}, user = {}) => {
        const data = {
            user: _.merge(_.cloneDeep(userMock), user),
            vinReport: { data: _.cloneDeep(reportMock) },
            card: _.merge(_.cloneDeepWith(offerMock), offer),
        };

        const store = mockStore(data);

        return shallow(
            <CardVinReportLink/>,
            { context: { ...context, store } },
        ).dive().dive();
    };

    it('owner', () => {
        const wrapper = renderComponent();
        expect(wrapper.props()).toHaveProperty('metrika', 'history_report,click_vin_promo_button_mobile,owner');
    });

    it('not_owner', () => {
        const wrapper = renderComponent({ additional_info: { is_owner: false } });
        expect(wrapper.props()).toHaveProperty('metrika', 'history_report,click_vin_promo_button_mobile,not_owner');
    });

    it('no_login', () => {
        const wrapper = renderComponent(
            { additional_info: { is_owner: false } },
            { data: { auth: false } },
        );
        expect(wrapper.props()).toHaveProperty('metrika', 'history_report,click_vin_promo_button_mobile,no_login');
    });
});

describe('должен отрендерить правильный текст', () => {
    const renderComponent = ({ report = {}, user = {} }) => {
        const data = {
            user: _.merge(_.cloneDeep(userMock), user),
            vinReport: { data: _.merge(_.cloneDeep(reportMock), report) },
            card: offerMock,
        };

        const store = mockStore(data);

        return shallow(
            <CardVinReportLink/>,
            { context: { ...context, store } },
        ).dive().dive();
    };

    it('отчет от', () => {
        const wrapper = renderComponent({});
        const priceElement = wrapper.find('Price').dive();
        expect(priceElement.text()).toEqual('Показать полный VIN и госномер от 30 ₽');
    });

    it('отчет за', () => {
        const report = _.cloneDeep(reportMock);
        report.billing && (report.billing.service_prices[0].price = 10);
        const wrapper = renderComponent({ report });
        const priceElement = wrapper.find('Price').dive();
        expect(priceElement.text()).toEqual('Показать полный VIN и госномер за 10 ₽');
    });

    it('для незалогина', () => {
        const wrapper = renderComponent({
            user: { data: { auth: false } },
        }).childAt(0);
        expect(wrapper.text()).toEqual('Показать полный VIN и госномер');
    });
});

it('прокидывает правильный from в ссылку', () => {
    const store = mockStore({
        user: userMock,
        vinReport: { data: reportMock },
        card: offerMock,
    });
    const wrapper = shallow(
        <CardVinReportLink/>,
        { context: { ...context, store } },
    ).dive().dive();

    const link = wrapper.find('VinHistoryPaywallLink');
    expect((link.props() as { from: TBillingFrom }).from).toEqual(TBillingFrom.MOBILE_CARD_LINK);
});
