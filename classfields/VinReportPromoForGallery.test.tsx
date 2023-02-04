/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/event-log/statApi');

import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import statApi from 'auto-core/lib/event-log/statApi';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import userWithAuthMock from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';
import type Button from 'auto-core/react/components/islands/Button/Button';

import VinReportPromoForGallery from './VinReportPromoForGallery';

it('должен отправлять метрику показа VinReportPromoForGallery', () => {
    const context = _.cloneDeep(contextMock);

    shallow(
        <VinReportPromoForGallery
            isAuth={ false }
            vinReport={ cardVinReportFree }
        />,
        { context },
    );

    const expectedResult = [ 'gallery_history', 'view' ];
    expect(context.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(context.metrika.sendPageEvent.mock.calls[0][0]).toEqual(expectedResult);
});

it('должен отправить событие paid_report_view_event во фронтлог', () => {
    const wrapper = shallow(
        <VinReportPromoForGallery
            isAuth={ false }
            vinReport={ cardVinReportFree }
        />,
        {
            context: {
                ...contextMock,
                store: mockStore({
                    card: offerMock,
                    config: configStateMock.withPageType('card').value(),
                    user: userWithAuthMock,
                }),
            },
        },
    );

    // expect(wrapper
    //     .find('CardVinReportButtons').dive()
    //     .findWhere(node => node.prop('name') === 'CardVinReportSingleButton').dive().dive()).toMatchSnapshot();

    const VinHistoryPaywall = wrapper
        .find('CardVinReportButtons').dive()
        .findWhere(node => node.prop('name') === 'CardVinReportSingleButton').dive().dive()
        .find('VinHistoryPaywallButton').dive().instance() as unknown as Button;

    VinHistoryPaywall.onClick();

    expect(statApi.logImmediately).toHaveBeenCalledTimes(1);
    expect(statApi.logImmediately).toHaveBeenCalledWith({
        paid_report_view_event: {
            card_from: 'SERP',
            card_id: '1085562758-1970f439',
            category: 'CARS',
            context_block: 'BLOCK_GALLERY',
            context_page: 'PAGE_CARD',
            search_query_id: '',
            section: 'USED',
            self_type: 'TYPE_SINGLE',
            trade_in_allowed: false,
        },
    });
});
