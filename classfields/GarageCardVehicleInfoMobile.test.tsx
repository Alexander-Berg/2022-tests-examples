import { noop } from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import type { PricePredict } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';
import { CardTypeInfo_CardType } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/garage/garage_api_model';
import { OfferStatus, AdditionalInfo_ProvenOwnerStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import IconSvg from 'auto-core/react/components/islands/IconSvg/IconSvg';
import Button from 'auto-core/react/components/islands/Button/Button';
import type { StateGarageCard } from 'auto-core/react/dataDomain/garageCard/types';
import { nbsp } from 'auto-core/react/lib/html-entities';

import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';
import garageCardMock from 'auto-core/models/garageCard/mocks/mockChain';

const Context = createContextProvider(contextMock);

import GarageCardVehicleInfoItemMobile from './GarageCardVehicleInfoItemMobile/GarageCardVehicleInfoItemMobile';
import GarageCardVehicleInfoMobile from './GarageCardVehicleInfoMobile';

let state: { garageCard: StateGarageCard };
beforeEach(() => {
    state = {
        garageCard: {
            pending: false,
            state: 'VIEW',
        },
    };
});

it('рендерит один item - "Параметры" с кнопкой изменить, нет текста, так как нет tech_param', async() => {
    const card = {} as Card;

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <GarageCardVehicleInfoMobile
                garageCard={ card }
                onSwitchToEdit={ noop }
                onSwitchToExpanded={ noop }
            />
        </Provider>,
    ).dive().dive();

    expect(wrapper.find(GarageCardVehicleInfoItemMobile)).toHaveLength(1);
    expect(wrapper.find(Button)).toHaveLength(1);
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).prop('title')).toEqual('Параметры');
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).prop('text')).toEqual('');
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).prop('iconType')).toEqual('cars');
    expect(wrapper.find(Button).at(0).children().text()).toEqual('Изменить');
});

it('рендерит один item - "Параметры" с иконкой, нет текста, так как нет tech_param', async() => {
    const card = {
        is_shared_view: true,
    } as Card;

    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <GarageCardVehicleInfoMobile
                garageCard={ card }
                onSwitchToEdit={ noop }
                onSwitchToExpanded={ noop }
            />
        </Provider>,
    ).dive().dive();

    expect(wrapper.find(GarageCardVehicleInfoItemMobile)).toHaveLength(1);
    expect(wrapper.find(IconSvg)).toHaveLength(1);
    expect(wrapper.find(Button)).toHaveLength(0);
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).prop('title')).toEqual('Параметры');
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).prop('text')).toEqual('');
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).prop('iconType')).toEqual('cars');
});

it('рендерит один item - "Параметры" с иконкой, цена выключена', () => {
    const card = garageCardMock
        .withCardType(CardTypeInfo_CardType.CURRENT_CAR)
        .withOfferInfo()
        .value();
    const wrapper = shallow(
        <Context>
            <Provider store={ mockStore(state) }>
                <GarageCardVehicleInfoMobile
                    garageCard={ card }
                    showPrice={ false }
                    onSwitchToEdit={ noop }
                    onSwitchToExpanded={ noop }
                />
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(wrapper.find(GarageCardVehicleInfoItemMobile)).toHaveLength(1);
    expect(wrapper.find(Button)).toHaveLength(1);
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).prop('title')).toEqual('Параметры');
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).prop('text')).toEqual('2005, 1.6 л, 107 л.с.');
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).prop('iconType')).toEqual('cars');
    expect(wrapper.find(Button).at(0).children().text()).toEqual('Изменить');
});

it('рендерит второй item с заголовком "Продаётся за" и текстом с ценой, с кнопкой "Объявление", так как есть активное объявление', () => {
    const card = garageCardMock
        .withCardType(CardTypeInfo_CardType.CURRENT_CAR)
        .withOfferInfo()
        .value();
    const wrapper = shallow(
        <Context>
            <Provider store={ mockStore(state) }>
                <GarageCardVehicleInfoMobile
                    garageCard={ card }
                    onSwitchToEdit={ noop }
                    onSwitchToExpanded={ noop }
                />
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(wrapper.find(GarageCardVehicleInfoItemMobile)).toHaveLength(2);
    expect(wrapper.find(Button)).toHaveLength(2);
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).at(1).prop('title')).toEqual('Продаётся за');
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).at(1).prop('text')).toEqual(`200${ nbsp }000${ nbsp }₽`);
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).at(1).prop('iconType')).toEqual('ruble');
    expect(wrapper.find(Button).at(1).children().text()).toEqual('Объявление');
});

it('рендерит второй item с заголовком "Стоимость" и текстом с ценой, есть кнопка "Продать", так как нет объвления', () => {
    const card = garageCardMock
        .withCardType(CardTypeInfo_CardType.CURRENT_CAR)
        .value();

    card.price_predict = {
        predict: {
            market: {
                price: 220000,
            },
        },
    } as PricePredict;

    const wrapper = shallow(
        <Context>
            <Provider store={ mockStore(state) }>
                <GarageCardVehicleInfoMobile
                    garageCard={ card }
                    onSwitchToEdit={ noop }
                    onSwitchToExpanded={ noop }
                />
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(wrapper.find(GarageCardVehicleInfoItemMobile)).toHaveLength(2);
    expect(wrapper.find(Button)).toHaveLength(2);
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).at(1).prop('title')).toEqual('Стоимость');
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).at(1).prop('text')).toEqual(`~ 220 000 ₽`);
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).at(1).prop('iconType')).toEqual('ruble');
    expect(wrapper.find(Button).at(1).children().text()).toEqual('Продать');
});

it('рендерит второй item с заголовком "Стоимость" и текстом с ценой, есть кнопка "Продать", так как есть активное объявление', () => {
    const OFFER_INFO = {
        offer_id: '1114113983-2db403c7',
        status: OfferStatus.STATUS_UNKNOWN,
        price: 200000,
        proven_owner_status: AdditionalInfo_ProvenOwnerStatus.OK,
    };
    const card = garageCardMock
        .withCardType(CardTypeInfo_CardType.CURRENT_CAR)
        .withOfferInfo(OFFER_INFO)
        .value();

    card.price_predict = {
        predict: {
            market: {
                price: 220000,
            },
        },
    } as PricePredict;

    const wrapper = shallow(
        <Context>
            <Provider store={ mockStore(state) }>
                <GarageCardVehicleInfoMobile
                    garageCard={ card }
                    onSwitchToEdit={ noop }
                    onSwitchToExpanded={ noop }
                />
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(wrapper.find(GarageCardVehicleInfoItemMobile)).toHaveLength(2);
    expect(wrapper.find(Button)).toHaveLength(2);
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).at(1).prop('title')).toEqual('Стоимость');
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).at(1).prop('text')).toEqual(`~ 220 000 ₽`);
    expect(wrapper.find(GarageCardVehicleInfoItemMobile).at(1).prop('iconType')).toEqual('ruble');
    expect(wrapper.find(Button).at(1).children().text()).toEqual('Продать');
});
