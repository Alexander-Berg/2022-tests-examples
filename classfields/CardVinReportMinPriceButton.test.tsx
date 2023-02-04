import 'jest-enzyme';
import React from 'react';
import _ from 'lodash';
import { shallow } from 'enzyme';

import { ReportType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import { ContextBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';
import Button from 'auto-core/react/components/islands/Button/Button';

import { TBillingFrom } from 'auto-core/types/TBilling';

import CardVinReportMinPriceButton from './CardVinReportMinPriceButton';
import type { Props } from './CardVinReportMinPriceButton';

beforeEach(() => {
    contextMock.metrika.sendPageEvent.mockClear();
});

const shallowRenderComponent = (props?: Partial<Props>) => {
    return shallow(
        <CardVinReportMinPriceButton
            color={ Button.COLOR.RED }
            from={ TBillingFrom.DESKTOP_CARD }
            name="SomeButton"
            needAuth={ false }
            text="Смотреть что там"
            size={ Button.SIZE.L }
            vinReport={ cardVinReportFree }
            contextBlock={ ContextBlock.BLOCK_CARD }
            { ...props }
        />,
        { context: { ...contextMock, store: mockStore({
            config: {
                data: {
                    pageType: 'card',
                    url: '/cars/used/sale/bmw/x5/1103151838-88710a71/',
                    host: 'auto.ru',
                },
            },
            user: { data: {} },
        }) } },
    );
};

it('у кнопки CardVinReportMinPriceButton должна быть правильная ссылка', () => {
    const vinReport = _.cloneDeep(cardVinReportFree);
    vinReport.report!.report_type = ReportType.PAID_REPORT;

    const wrapper = shallowRenderComponent({ vinReport });

    expect(wrapper.dive().prop('href')).toEqual('link/proauto-report/?history_entity_id=1084368429-e9a4c888');
});

it('у кнопки CardVinReportMinPriceButton должен быть текст "ОТ х руб"', () => {
    const wrapper = shallowRenderComponent();

    expect(wrapper.dive().dive().dive().find('Price').dive().text()).toEqual('Смотреть что там от 40 ₽');
});

it('у кнопки CardVinReportMinPriceButton должен быть текст "ЗА х руб"', () => {
    const vinReport = _.cloneDeep(cardVinReportFree);
    vinReport.billing && (vinReport.billing.service_prices[0].price = 10);

    const wrapper = shallowRenderComponent({ vinReport });

    expect(wrapper.dive().dive().dive().find('Price').dive().text()).toEqual('Смотреть что там за 10 ₽');
});

it('не показывать цену, если нет авторизации', () => {
    const wrapper = shallowRenderComponent({ needAuth: true });
    const contentElement = wrapper.dive().dive().find('.CardVinReportMinPriceButton__content');
    expect(contentElement.text()).toEqual('Смотреть что там');
});

it('CardVinReportMinPriceButton должен отправить метрику при переходе на отчет', () => {
    const vinReport = _.cloneDeep(cardVinReportFree);
    vinReport.report!.report_type = ReportType.PAID_REPORT;

    const wrapper = shallowRenderComponent({ vinReport, metrikaParam: 'test_metrika_param' });
    wrapper.simulate('click');

    expect(contextMock.metrika.sendPageEvent.mock.calls[0][0]).toEqual([ 'test_metrika_param', 'click_go_to_standalone' ]);
});
