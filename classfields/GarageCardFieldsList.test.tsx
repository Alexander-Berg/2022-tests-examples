import React from 'react';
import { shallow } from 'enzyme';

import type { SuperGeneration } from '@vertis/schema-registry/ts-types-snake/auto/api/catalog_model';

import garageCardMock from 'auto-core/models/garageCard/mocks/mock-license_place';

import GarageCardFieldsList from './GarageCardFieldsList';

it('должен отрендерить блок с Поколением, если есть данные', () => {
    const wrapper = shallow(
        <GarageCardFieldsList garageCard={ garageCardMock.card }/>,
    );

    const superGen = wrapper.find('.GarageCardFieldsList__item').at(4);

    expect(superGen.text()).toContain('Поколение2003 – 2010 III (XD2) Рестайлинг');
});

it('не должен отрендерить блок с Поколением, если нет данных', () => {
    const garageCard = Object.assign({}, garageCardMock.card);
    garageCard.vehicle_info && garageCard.vehicle_info.car_info && (garageCard.vehicle_info.car_info.super_gen = {} as SuperGeneration);

    const wrapper = shallow(
        <GarageCardFieldsList garageCard={ garageCardMock.card }/>,
    );

    const superGen = wrapper.find('.GarageCardFieldsList__item').at(4);

    expect(superGen.text()).not.toContain('Поколение');
});

it('должен отрендерить в блоке Поколение только даты', () => {
    const garageCard = Object.assign({}, garageCardMock.card);
    garageCard.vehicle_info && garageCard.vehicle_info.car_info && (garageCard.vehicle_info.car_info.super_gen = {
        year_from: 2003,
        year_to: 2010,
    } as SuperGeneration);

    const wrapper = shallow(
        <GarageCardFieldsList garageCard={ garageCardMock.card }/>,
    );

    const superGen = wrapper.find('.GarageCardFieldsList__item').at(4);

    expect(superGen.text()).toContain('Поколение2003 – 2010');
});

it('должен отрендерить в блоке Поколение только название', () => {
    const garageCard = Object.assign({}, garageCardMock.card);
    garageCard.vehicle_info && garageCard.vehicle_info.car_info && (garageCard.vehicle_info.car_info.super_gen = {
        name: 'III (XD2) Рестайлинг',
    } as SuperGeneration);

    const wrapper = shallow(
        <GarageCardFieldsList garageCard={ garageCardMock.card }/>,
    );

    const superGen = wrapper.find('.GarageCardFieldsList__item').at(4);

    expect(superGen.text()).toContain('ПоколениеIII (XD2) Рестайлинг');
});
