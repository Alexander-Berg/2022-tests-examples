import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import mockStore from 'autoru-frontend/mocks/mockStore';

import DealerPlacementReportPDF from './DealerPlacementReportPDF';

const storeMock = {
    placementReport: {
        filters: {
            period: {
                from: '2019-10-01',
                to: '2019-10-31',
            },
            category: 'CARS',
            section: 'USED',
        },
        dealerName: 'west coast customs' as string | undefined,
        dealerInfo: {
            multiposting_enabled: true,
        },
        callsSettings: {
            calltracking_enabled: true,
        },
        uniqueOffersCount: {
            autoru_active: 0,
            autoru_inactive: 0,
            avito_active: 0,
            avito_inactive: 0,
            drom_active: 0,
            drom_inactive: 0,
            total_active: 0,
            total_inactive: 0,
            total_removed: 0,
        },
        callsTotalStatsMultiposting: {},
        dealerOffersDailyStats: {},
        dealerProductAvitoTotalStats: {},
        dealerProductTotalStats: {},
        warehouseDailyState: {},
    },
};

it('покажет правильный период на титульной странице', () => {
    const tree = shallowRenderComponent();
    expect(tree.find('.DealerPlacementReportPDF__firstPageTitle').text()).toEqual('Отчёт о размещении легковых б/уза 1 октября 2019 – 31 октября 2019');
});

it('не покажет название дилера, если его нет', () => {
    const store = _.cloneDeep(storeMock);
    delete store.placementReport.dealerName;
    const tree = shallowRenderComponent(store);

    expect(tree.find('.DealerPlacementReportPDF__firstPageSalon')).not.toExist();
});

function shallowRenderComponent(store = storeMock) {
    return shallow(
        <DealerPlacementReportPDF/>,
        { context: { store: mockStore(store) } },
    ).dive();
}
