import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import Badge from 'auto-core/react/components/common/Badges/Badge/Badge';

import BadgeForExclusiveOfferDesktop from './BadgeForExclusiveOfferDesktop';
import type { Props } from './BadgeForExclusiveOfferDesktop';

function shallowRenderComponent(props: Props) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <BadgeForExclusiveOfferDesktop
                { ...props }/>
        </ContextProvider>,
    ).dive();
}

it('не отрисует бейдж для оффера без флага эксклюзивности', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([])
        .value();
    const tree = shallowRenderComponent({ offer, color: Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA });

    expect(tree).toBeEmptyRender();
});

it('не отрисует бейдж для забронированного оффера', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([ 'autoru_exclusive' ])
        .withBookingStatus('BOOKED')
        .value();
    const tree = shallowRenderComponent({ offer, color: Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA });

    expect(tree).toBeEmptyRender();
});

it('при маунте отправит метрику для офера с флагом эксклюзивности', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([ 'autoru_exclusive' ])
        .value();
    shallowRenderComponent({ offer, color: Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA });

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'exclusive', 'shows' ]);
});

it('при маунте не отправит метрику для забронированного офера с флагом эксклюзивности', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withBookingStatus('BOOKED')
        .withTags([ 'autoru_exclusive' ])
        .value();
    shallowRenderComponent({ offer, color: Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA });

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
});

it('при маунте не отправит метрику для офера без флага эксклюзивности', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([])
        .value();
    shallowRenderComponent({ offer, color: Badge.COLOR.BLUE_GRAY_LIGHT_EXTRA });

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
});
