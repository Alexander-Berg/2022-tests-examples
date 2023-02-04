/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import type { Complectation } from '@vertis/schema-registry/ts-types-snake/auto/api/catalog_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import { cardGroupComplectations } from 'autoru-frontend/mockData/state/cardGroupComplectations.mock';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';

import type { Offer, TOfferVehicleInfo } from 'auto-core/types/proto/auto/api/api_offer_model';

import CardComplectation from './CardComplectation';

let offer: Offer;
let store: any;

beforeEach(() => {
    offer = cloneOfferWithHelpers({})
        .withCategory('cars')
        .withMarkInfo({ code: 'RENAULT', name: 'RENAULT' })
        .withMilage(100)
        .withModelInfo({ code: 'DUSTER', name: 'DUSTER' })
        .withSellerGeoParentsIds([ '1', '10174' ])
        .withYear(2018)
        .value();
    offer.vehicle_info.equipmentGroups = [
        {
            name: 'Мультимедиа',
            values: [ 'Система «старт-стоп»' ],
        },
    ];

    store = mockStore({
        bunker: getBunkerMock([ 'common/yandex_auto_cars' ]),
        cardGroupComplectations,
    });
});

it('должен отрисовать плашку Яндес.Авто, если объвление подходит по условие', () => {
    const wrapper = shallow(
        <CardComplectation
            offer={ offer }
            isMobile
        />,
        { context: { ...contextMock, store } },
    ).dive();

    expect(wrapper.find('CardComplectationTitleBanner')).toExist();
});

it('должен отрисовать плашку Яндес.Авто, если объвление не подходит по условие', () => {
    offer = cloneOfferWithHelpers(offer)
        .withModelInfo({ code: 'DUSTER1', name: 'DUSTER1' })
        .value();

    const wrapper = shallow(
        <CardComplectation
            offer={ offer }
        />,
        { context: { ...contextMock, store } },
    ).dive();

    expect(wrapper.find('CardComplectationTitleBanner')).not.toExist();
});

describe('ссылка на сравнение комплектаций', () => {
    const linkToComplectationsComparison = 'link/card-group-options/' +
        '?category=cars&mark=RENAULT&model=DUSTER&section=new&configuration_id=123' +
        '&super_gen=321&tab_id=compare';
    const vehicleInfoMock = {
        mark_info: { code: 'RENAULT', name: 'RENAULT' },
        model_info: { code: 'DUSTER', name: 'DUSTER' },
        configuration: { id: '123', body_type_group: 'ALL_ROAD_5_DOORS', human_name: 'configuration' },
        complectation: { id: '456', name: 'Comfort' },
        super_gen: { id: '321', name: 'super_gen' },
    } as unknown as TOfferVehicleInfo;

    it('должна быть без дополнительных параметров в ней', () => {
        offer = cloneOfferWithHelpers(offer)
            .withCategory('cars')
            .withSection('new')
            .withVehicleInfo(vehicleInfoMock)
            .value();

        const wrapper = shallow(
            <CardComplectation offer={ offer }/>,
            { context: { ...contextMock, store } },
        ).dive();

        const link = wrapper.find('.CardComplectation__compareAll');

        expect(link).toExist();
        expect(link.props()).toHaveProperty('url', linkToComplectationsComparison);
    });

    it('не должна быть, если это Individual комплектация и она единственная для этой конфигурации', () => {
        const storeWithUniqueComplectation = mockStore({
            bunker: getBunkerMock([ 'common/yandex_auto_cars' ]),
            cardGroupComplectations: {
                data: {
                    complectations: [
                        { complectation_id: '0', complectation_name: 'Индивидуальная' },
                    ],
                },
            },
        });
        offer = cloneOfferWithHelpers(offer)
            .withCategory('cars')
            .withSection('new')
            .withVehicleInfo({
                ...vehicleInfoMock,
                complectation: { id: '0', name: 'Индивидуальная' } as Complectation,
            })
            .value();

        const wrapper = shallow(
            <CardComplectation offer={ offer }/>,
            { context: { ...contextMock, store: storeWithUniqueComplectation } },
        ).dive();

        const link = wrapper.find('.CardComplectation__compareAll');

        expect(link).not.toExist();
    });

    it('не должна быть, если категория не cars', () => {
        offer = cloneOfferWithHelpers(offer)
            .withCategory('moto')
            .withSection('new')
            .value();

        const wrapper = shallow(
            <CardComplectation offer={ offer }/>,
            { context: { ...contextMock, store } },
        ).dive();

        const link = wrapper.find('.CardComplectation__compareAll');

        expect(link).not.toExist();
    });

    it('не должна быть, если тачка не новая', () => {
        offer = cloneOfferWithHelpers(offer)
            .withSection('used')
            .value();

        const wrapper = shallow(
            <CardComplectation offer={ offer }/>,
            { context: { ...contextMock, store } },
        ).dive();

        const link = wrapper.find('.CardComplectation__compareAll');

        expect(link).not.toExist();
    });

    it('не должна быть в мобилке', () => {
        offer = cloneOfferWithHelpers(offer)
            .withCategory('cars')
            .withSection('new')
            .value();

        const wrapper = shallow(
            <CardComplectation offer={ offer } isMobile/>,
            { context: { ...contextMock, store } },
        ).dive();

        const link = wrapper.find('.CardComplectation__compareAll');

        expect(link).not.toExist();
    });

});
