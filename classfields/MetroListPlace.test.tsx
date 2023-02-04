import React from 'react';
import renderer from 'react-test-renderer';

import type { RegionInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import type { Location } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import type { RegionWithLinguistics } from 'auto-core/react/dataDomain/geo/StateGeo';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import MetroListPlace from './MetroListPlace';

it('должен отрендерить название города', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerLocation({
            region_info: { name: 'Москва' } as RegionInfo,
        });

    const tree = renderer.create(
        <MetroListPlace
            offer={ offer.value() }
        />,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('должен добавить класс MetroListPlace_ellipsis, если ellipsis=true', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSellerLocation({
            region_info: { name: 'Москва' } as RegionInfo,
        });

    const tree = renderer.create(
        <MetroListPlace
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
        <MetroListPlace
            offer={ offer.value() }
        >
            6 минут назад
        </MetroListPlace>,
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
                <MetroListPlace
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
                <MetroListPlace
                    offer={ offer.value() }
                >
                    6 минут назад
                </MetroListPlace>,
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
                <MetroListPlace
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
                <MetroListPlace
                    showMetroOrRegionName={ true }
                    offer={ offer.value() }
                >
                    6 минут назад
                </MetroListPlace>,
            ).toJSON();
            expect(tree).toMatchSnapshot();
        });

        it('должен отрендерить название города, если нет метро', () => {
            const offer = cloneOfferWithHelpers(offerMock)
                .withSellerLocation({
                    region_info: { name: 'Москва' } as RegionInfo,
                });

            const tree = renderer.create(
                <MetroListPlace
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
                <MetroListPlace
                    showMetroOrRegionName={ true }
                    offer={ offer.value() }
                >
                    6 минут назад
                </MetroListPlace>,
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
            <MetroListPlace
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
            <MetroListPlace
                offer={ offer.value() }
            />,
        ).toJSON();
        expect(tree).toMatchSnapshot();
    });

    it('не должен отрендерить адрес, если showAddress=true, но рендерит расстояние до города', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerLocation({
                distance_to_selected_geo: [ {
                    geobase_id: '2',
                    distance: 613,
                    region_info: { id: '2', name: 'Санкт-Петербург', genitive: 'Санкт-Петербурга' } as RegionInfo,
                } ],
                region_info: { id: '213', name: 'Москва' } as RegionInfo,
            });

        const tree = renderer.create(
            <MetroListPlace
                geo={{
                    gidsInfo: [ { id: 2 } as RegionWithLinguistics ],
                    radius: 200,
                    shouldShowGeoRadiusExceeded: false,
                }}
                offer={ offer.value() }
                showDistanceFromSelectedCity
                showAddress
            />,
        ).toJSON();
        expect(tree).toMatchSnapshot();
    });

    it('должен отрендерить только название города, если переданы showMetro=false и showAddress=false', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerLocation({
                distance_to_selected_geo: [ {
                    geobase_id: '2',
                    distance: 613,
                    region_info: { id: '2', name: 'Санкт-Петербург', genitive: 'Санкт-Петербурга' } as RegionInfo,
                } ],
                metro: [
                    { rid: '1', name: 'Парк Культуры', distance: 0, lines: [ { color: '#f00', name: '' } ] },
                ],
                address: 'Льва Толстого 16',
                region_info: { name: 'Москва' } as RegionInfo,
            })
            .value();

        const tree = renderer.create(
            <MetroListPlace
                showMetro={ false }
                showAddress={ false }
                offer={ offer }
            />,
        ).toJSON();
        expect(tree).toMatchSnapshot();
    });
});

describe('showDistanceFromSelectedCity', () => {
    it('должен отрендерить дистанцию рядом с названием города', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerLocation({
                distance_to_selected_geo: [ {
                    geobase_id: '2',
                    distance: 613,
                    region_info: { id: '2', name: 'Санкт-Петербург', genitive: 'Санкт-Петербурга' } as RegionInfo,
                } ],
                region_info: { id: '213', name: 'Москва' } as RegionInfo,
            });

        const tree = renderer.create(
            <MetroListPlace
                geo={{
                    gidsInfo: [ { id: 2 } as RegionWithLinguistics ],
                    radius: 200,
                    shouldShowGeoRadiusExceeded: false,
                }}
                offer={ offer.value() }
                showDistanceFromSelectedCity
                showMetroOrRegionName={ true }
            />,
        ).toJSON();
        expect(tree).toMatchSnapshot();
    });

    it('должен отрендерить дистанцию рядом с названием города и infoPopup для растояния больше георадиуса', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerLocation({
                distance_to_selected_geo: [ {
                    geobase_id: '2',
                    distance: 613,
                    region_info: { id: '2', name: 'Санкт-Петербург', genitive: 'Санкт-Петербурга' } as RegionInfo,
                } ],
                region_info: { id: '213', name: 'Москва' } as RegionInfo,
            });

        const tree = renderer.create(
            <MetroListPlace
                geo={{
                    gidsInfo: [ { id: 2 } as RegionWithLinguistics ],
                    radius: 200,
                    shouldShowGeoRadiusExceeded: true,
                }}
                offer={ offer.value() }
                showDistanceFromSelectedCity
                showMetroOrRegionName={ true }
            />,
        ).toJSON();
        expect(tree).toMatchSnapshot();
    });
});
