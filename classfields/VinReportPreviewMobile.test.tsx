/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

import React from 'react';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import statApi from 'auto-core/lib/event-log/statApi';

import type Button from 'auto-core/react/components/islands/Button/Button';
import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock.js';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import userWithAuthMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import vinReportMock from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';

import VinReportPreviewMobile from './VinReportPreviewMobile';

const PREVIEW_INFO = {
    title: 'Nissan Almera, 2015',
    year: '2011 г.',
    color: 'Серый',
    markLogo: undefined,
    vin: 'Z0NZWE000****41234',
};

it('должен передавать правильный from для покупки отчёта', () => {
    const wrapper = shallow(
        <VinReportPreviewMobile
            vinReport={ vinReportMock }
            previewInfo={ PREVIEW_INFO }
            vinReportPaymentParams={ '{"payment_param": "111"}' }
            offer={ cardMock }
        />,
        { context: contextMock },
    );
    expect(wrapper.find('CardVinReportButtons').props().from).toEqual('api_m_vincheck');
});

it('должен отправить событие paid_report_view_event во фронтлог', () => {
    const wrapper = shallow(
        <VinReportPreviewMobile
            vinReport={ vinReportMock }
            previewInfo={ PREVIEW_INFO }
            vinReportPaymentParams={ '{"payment_param": "111"}' }
            offer={ cardMock }
            isAuth
        />,
        {
            context: {
                ...contextMock,
                store: mockStore({
                    card: cardMock,
                    config: configStateMock.withPageType('history-by-vin').value(),
                    user: userWithAuthMock,
                }),
            },
        },
    );

    wrapper.instance().setState({ showStickyButton: true });
    const VinHistoryPaywall = wrapper
        .find('CardVinReportButtons').first().dive()
        .findWhere(node => node.prop('name') === 'CardVinReportSingleButton').dive()
        .find('Connect(VinHistoryPaywallButton)').dive().dive()
        .instance() as unknown as Button;

    VinHistoryPaywall.onClick();

    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({
        paid_report_view_event: {
            card_from: 'SERP',
            card_id: '1085562758-1970f439',
            category: 'CARS',
            context_block: 'BLOCK_HISTORY',
            context_page: 'PAGE_HISTORY',
            search_query_id: '',
            section: 'USED',
            self_type: 'TYPE_SINGLE',
            trade_in_allowed: false,
        },
    });
});
