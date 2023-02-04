import React from 'react';
import { shallow } from 'enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import ListingItemCertificate from './ListingItemCertificate';

it('должен отрисовать лейбл о сертификации', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withTags([])
        .withBrandCertInfo()
        .value();

    const tree = shallow(
        <ListingItemCertificate offer={ offer }/>,
    );

    expect(tree).not.toBeEmptyRender();
});

it('не должен отрисовать лейбл о сертификации, если в оффере нет такой инфы', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .value();

    const tree = shallow(
        <ListingItemCertificate offer={ offer }/>,
    );

    expect(tree).toBeEmptyRender();
});

it('не должен отрисовать лейбл о сертификации, если оффер забронирован', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withBookingStatus('BOOKED')
        .value();

    const tree = shallow(
        <ListingItemCertificate offer={ offer }/>,
    );

    expect(tree).toBeEmptyRender();
});
