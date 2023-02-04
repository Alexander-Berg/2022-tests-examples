import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import contextMock from 'autoru-frontend/mocks/contextMock';

import offerCarsUsed from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import offerTrucksLcv from 'auto-core/react/dataDomain/card/mocks/card.lcv.mock';
import offerTrucksArtic from 'auto-core/react/dataDomain/card/mocks/card.artic.mock';
import offerTrucksTruck from 'auto-core/react/dataDomain/card/mocks/card.truck.mock';
import offerTrucksBus from 'auto-core/react/dataDomain/card/mocks/card.bus.mock';
import offerTrucksTrailer from 'auto-core/react/dataDomain/card/mocks/card.trailer.mock';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import OfferSnippetRoyalSpec from './OfferSnippetRoyalSpec';

type TestCase = { name: string; offer: Offer; expected: Array<string> };

const offerCarsNew = cloneOfferWithHelpers(offerCarsUsed).withSection('new').value();

const TEST_CASES = [
    { name: 'CARS NEW', offer: offerCarsNew, expected: [ 'complectationOrEquipmentCount', 'modification', 'drive', 'bodytype', 'engineType', 'vin' ] },
    { name: 'CARS USED', offer: offerCarsUsed, expected: [ 'kmAge', 'modification', 'drive', 'bodytype', 'engineType', 'vin' ] },
    { name: 'TRUCK LCV', offer: offerTrucksLcv, expected: [ 'bodytype', 'engine', 'transmission', 'drive', 'vin' ] },
    { name: 'TRUCK ARTIC', offer: offerTrucksArtic, expected: [ 'kmAge', 'engine', 'customs', 'vin' ] },
    { name: 'TRUCK TRUCK', offer: offerTrucksTruck, expected: [ 'kmAge', 'engine', 'customs', 'bodytype', 'vin' ] },
    { name: 'TRUCK BUS', offer: offerTrucksBus, expected: [ 'kmAge', 'engine', 'customs', 'busType', 'vin' ] },
    { name: 'TRUCK TRAILER', offer: offerTrucksTrailer, expected: [ 'kmAge', 'customs', 'bodytype', 'vin' ] },
];

describe('вернет правильный набор спеков для каждой категории-секции', () => {
    TEST_CASES.forEach(makeTest);
});

function makeTest({ expected, name, offer }: TestCase) {
    it(name, () => {
        const tree = shallowRenderComponent(offer);
        expect((tree.instance() as OfferSnippetRoyalSpec).getSpecNames()).toEqual(expected);
    });
}

function shallowRenderComponent(offer: Offer) {
    return shallow(
        <OfferSnippetRoyalSpec isNarrow={ false } offer={ offer }/>,
        { context: { ...contextMock } },
    );
}
