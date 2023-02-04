import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import MockDate from 'mockdate';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import type { TOfferMock } from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import CardDatesAndStats from './CardDatesAndStats';

beforeEach(() => {
    MockDate.set('2020-04-17');
});

it('должен отрендерить дату и количество происмотров, если есть данные', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withIsOwner(false)
        .withCounters()
        .withCreationDate(1586581933000)
        .value() as Offer;

    const wrapper = shallow(<CardDatesAndStats offer={ offer }/>);

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отрендерить дату без количества происмотров, если нет данных', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withIsOwner(false)
        .withCounters({ all: 0, daily: 0 })
        .withCreationDate(1586581933000)
        .value() as Offer;

    const wrapper = shallow(<CardDatesAndStats offer={ offer }/>);

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

describe('для владельца', () => {
    let offerDefault: TOfferMock;
    beforeEach(() => {
        offerDefault = cloneOfferWithHelpers(offerMock)
            .withCreationDate(1586581933000)
            .withExpireDate('2020-04-20')
            .withIsOwner(true)
            .withSellerTypePrivate()
            .withStatus(OfferStatus.ACTIVE);
    });

    it('если объява не активна покажет только дату создания', () => {
        const offer = offerDefault
            .withStatus(OfferStatus.INACTIVE)
            .value();
        const wrapper = shallow(<CardDatesAndStats offer={ offer }/>);

        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('если у объявы есть добавления в избранное покажет их кол-во', () => {
        const offer = offerDefault
            .withCounters({ favorite_total_all: 123 })
            .value();
        const wrapper = shallow(<CardDatesAndStats offer={ offer }/>);

        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('если у объявы подключено автопродление размещения, напишет "до продления"', () => {
        const offer = offerDefault
            .withCustomVas({ service: TOfferVas.PLACEMENT, prolongation_forced_not_togglable: true })
            .withActiveVas([ TOfferVas.PLACEMENT ], { prolongable: true })
            .value();
        const wrapper = shallow(<CardDatesAndStats offer={ offer }/>);

        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('если у объявы не подключено автопродление, напишет "до снятия"', () => {
        const offer = offerDefault.value();

        const wrapper = shallow(<CardDatesAndStats offer={ offer }/>);

        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('не нарисует статистику для дилера', () => {
        const offer = offerDefault
            .withSellerTypeCommercial()
            .value();

        const wrapper = shallow(<CardDatesAndStats offer={ offer }/>);

        expect(wrapper).toBeEmptyRender();
    });
});
