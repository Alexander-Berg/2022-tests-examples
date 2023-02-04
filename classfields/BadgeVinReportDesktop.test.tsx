import React from 'react';
import { shallow } from 'enzyme';

import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';
import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import Badge from 'auto-core/react/components/common/Badges/Badge/Badge';

import BadgeVinReportDesktop from './BadgeVinReportDesktop';

const ContextProvider = createContextProvider(contextMock);

it('не должен отрисовать бейдж о наличии отчёта, если это не указано в vin_resolution', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSection('used')
        .withVinResolution(Status.INVALID)
        .value();

    const tree = shallow(
        <ContextProvider>
            <BadgeVinReportDesktop offer={ offer } color={ Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA }/>
        </ContextProvider>,
    ).dive();
    expect(tree).toBeEmptyRender();
});

it('не должен отрисовать бейдж для неактивного оффера', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withStatus(OfferStatus.INACTIVE)
        .withVinResolution(Status.OK)
        .value();

    const tree = shallow(
        <ContextProvider>
            <BadgeVinReportDesktop offer={ offer } color={ Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA }/>
        </ContextProvider>,
    ).dive();
    expect(tree).toBeEmptyRender();
});

it('не должен отрисовать бейдж для заброннированного оффера', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withBookingStatus('BOOKED')
        .withVinResolution(Status.OK)
        .value();

    const tree = shallow(
        <ContextProvider>
            <BadgeVinReportDesktop offer={ offer } color={ Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA }/>
        </ContextProvider>,
    ).dive();
    expect(tree).toBeEmptyRender();
});
