import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import MockDate from 'mockdate';

import contextMock from 'autoru-frontend/mocks/contextMock';

import LAST_EVENT_TYPES from 'auto-core/data/dicts/comeback-last-event-types.json';

import BackOnSaleSellingInfo from './BackOnSaleSellingInfo';
import BackOnSaleSellingInfoProps from './mocks/BackOnSaleSellingInfoProps';

beforeEach(() => {
    MockDate.set('2020-06-11T12:00:00Z');
});

afterEach(() => {
    MockDate.reset();
});

it('должен нарисовать BackOnSaleSellingInfo, если объявление продано новым на авто.ру', async() => {
    expect(shallowToJson(shallow(
        <BackOnSaleSellingInfo
            { ...BackOnSaleSellingInfoProps({
                pastOfferSection: 'NEW',
                metaLastEventType: LAST_EVENT_TYPES.autoru_offer_new.id,
                metaSellersCountAfterPast: 2,
            }) }/>,
        {
            context: contextMock,
        },
    ))).toMatchSnapshot();
});

it('должен нарисовать BackOnSaleSellingInfo, если объявление продано БУ на авто.ру', async() => {
    expect(shallowToJson(shallow(
        <BackOnSaleSellingInfo
            { ...BackOnSaleSellingInfoProps({
                metaLastEventType: LAST_EVENT_TYPES.autoru_offer_used.id,
                metaSellersCountAfterPast: 1,
            }) }/>,
        {
            context: contextMock,
        },
    ))).toMatchSnapshot();
});

it('должен нарисовать BackOnSaleSellingInfo, если объявление продано не на авто.ру', async() => {
    expect(shallowToJson(shallow(
        <BackOnSaleSellingInfo
            { ...BackOnSaleSellingInfoProps({
                pastOfferSection: 'NEW',
                metaLastEventType: LAST_EVENT_TYPES.external_sale.id,
                metaSellersCountAfterPast: 1,
                pastExternalSale: '2020-04-02T12:00:00.103Z',
            }) }/>
        , { context: contextMock },
    ))).toMatchSnapshot();
});

it('должен нарисовать BackOnSaleSellingInfo, если объявление оценивалось', async() => {
    expect(shallowToJson(shallow(
        <BackOnSaleSellingInfo
            { ...BackOnSaleSellingInfoProps({
                metaLastEventType: LAST_EVENT_TYPES.estimate.id,
                pastEstimate: '2020-06-09T12:00:00Z',
            }) }/>,
        { context: contextMock },
    ))).toMatchSnapshot();
});

it('должен нарисовать BackOnSaleSellingInfo, если машина обслуживалась на сервисе', async() => {
    expect(shallowToJson(shallow(
        <BackOnSaleSellingInfo
            { ...BackOnSaleSellingInfoProps({
                metaLastEventType: LAST_EVENT_TYPES.maintenance.id,
                pastMaintenance: '2020-06-09T12:00:00Z',
            }) }/>,
        {
            context: contextMock,
        },
    ))).toMatchSnapshot();
});

it('не должен показать текст про количество бывших владельцев, если количества нет', async() => {
    const tree = shallow(
        <BackOnSaleSellingInfo
            { ...BackOnSaleSellingInfoProps({
                metaSellersCountAfterPast: 0,
            }) }/>,
        {
            context: contextMock,
        },
    );

    expect(tree.findWhere(node => node.key() === 'sellerCount').text()).toEqual('');
});
