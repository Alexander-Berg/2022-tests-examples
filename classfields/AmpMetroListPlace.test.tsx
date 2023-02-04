import React from 'react';
import renderer from 'react-test-renderer';

import type { Location } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import type { RegionInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import AmpMetroListPlace from './AmpMetroListPlace';

it('должен отрендерить название города', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerLocation({
            region_info: { name: 'Москва' } as RegionInfo,
        });

    const tree = renderer.create(
        <AmpMetroListPlace
            offer={ offer.value() }
        />,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('должен добавить класс AmpMetroListPlace_ellipsis, если ellipsis=true', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerLocation({
            region_info: { name: 'Москва' } as RegionInfo,
        });

    const tree = renderer.create(
        <AmpMetroListPlace
            offer={ offer.value() }
            ellipsis
        />,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('должен отрендерить детей', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerLocation({
            region_info: { name: 'Москва' } as RegionInfo,
        });

    const tree = renderer.create(
        <AmpMetroListPlace
            offer={ offer.value() }
        >
            6 минут назад
        </AmpMetroListPlace>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

describe('метро', () => {
    describe('showMetroOrRegionName=false', () => {
        it('должен отрендерить метро и название города', () => {
            const offer = cloneOfferWithHelpers(offerMock)
                .withSellerLocation({
                    metro: [
                        { rid: '1', name: 'Парк Культуры', distance: 0, lines: [ { color: '#f00', name: '' } ] },
                    ],
                    region_info: { name: 'Москва' } as RegionInfo,
                });

            const tree = renderer.create(
                <AmpMetroListPlace
                    offer={ offer.value() }
                />,
            ).toJSON();
            expect(tree).toMatchSnapshot();
        });
        it('должен отрендерить метро, название города и детей', () => {
            const offer = cloneOfferWithHelpers(offerMock)
                .withSellerLocation({
                    metro: [
                        { rid: '1', name: 'Парк Культуры', distance: 0, lines: [ { color: '#f00', name: '' } ] },
                    ],
                    region_info: { name: 'Москва' } as RegionInfo,
                });

            const tree = renderer.create(
                <AmpMetroListPlace
                    offer={ offer.value() }
                >
                    6 минут назад
                </AmpMetroListPlace>,
            ).toJSON();
            expect(tree).toMatchSnapshot();
        });
    });

    describe('showMetroOrRegionName=true', () => {
        it('должен отрендерить метро без города, если оно есть', () => {
            const offer = cloneOfferWithHelpers(offerMock)
                .withSellerLocation({
                    metro: [
                        { rid: '1', name: 'Парк Культуры', distance: 0, lines: [ { color: '#f00', name: '' } ] },
                    ],
                    region_info: { name: 'Москва' } as RegionInfo,
                });

            const tree = renderer.create(
                <AmpMetroListPlace
                    showMetroOrRegionName={ true }
                    offer={ offer.value() }
                />,
            ).toJSON();
            expect(tree).toMatchSnapshot();
        });

        it('должен отрендерить метро без города и детей, если оно есть', () => {
            const offer = cloneOfferWithHelpers(offerMock)
                .withSellerLocation({
                    metro: [
                        { rid: '1', name: 'Парк Культуры', distance: 0, lines: [ { color: '#f00', name: '' } ] },
                    ],
                    region_info: { name: 'Москва' } as RegionInfo,
                });

            const tree = renderer.create(
                <AmpMetroListPlace
                    showMetroOrRegionName={ true }
                    offer={ offer.value() }
                >
                    6 минут назад
                </AmpMetroListPlace>,
            ).toJSON();
            expect(tree).toMatchSnapshot();
        });

        it('должен отрендерить название города, если нет метро', () => {
            const offer = cloneOfferWithHelpers(offerMock)
                .withSellerLocation({
                    region_info: { name: 'Москва' } as RegionInfo,
                });

            const tree = renderer.create(
                <AmpMetroListPlace
                    showMetroOrRegionName={ true }
                    offer={ offer.value() }
                />,
            ).toJSON();
            expect(tree).toMatchSnapshot();
        });

        it('должен отрендерить название города и детей, если нет метро', () => {
            const offer = cloneOfferWithHelpers(offerMock)
                .withSellerLocation({
                    region_info: { name: 'Москва' } as RegionInfo,
                });

            const tree = renderer.create(
                <AmpMetroListPlace
                    showMetroOrRegionName={ true }
                    offer={ offer.value() }
                >
                    6 минут назад
                </AmpMetroListPlace>,
            ).toJSON();
            expect(tree).toMatchSnapshot();
        });
    });
});

describe('address', () => {
    let location: Location;
    beforeEach(() => {
        location = {
            address: 'Льва Толстого 16',
            region_info: { name: 'Москва' } as RegionInfo,
        } as Location;
    });

    it('должен отрендерить адрес, если showAddress=true', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerLocation(location);

        const tree = renderer.create(
            <AmpMetroListPlace
                offer={ offer.value() }
                showAddress={ true }
            />,
        ).toJSON();
        expect(tree).toMatchSnapshot();
    });

    it('не должен отрендерить адрес, если showAddress=false', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerLocation(location);

        const tree = renderer.create(
            <AmpMetroListPlace
                offer={ offer.value() }
            />,
        ).toJSON();
        expect(tree).toMatchSnapshot();
    });
});
