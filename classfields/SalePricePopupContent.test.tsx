import React from 'react';
import { shallow } from 'enzyme';

import { Currency } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import contextMock from 'autoru-frontend/mocks/contextMock';

import offerCarsMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import SalePricePopupContent from './SalePricePopupContent';

const offerObj = cloneOfferWithHelpers(offerCarsMock)
    .withMarketPrice({ price: 1000000, currency: Currency.RUR });

describe('правильно сформирует текст в шапке, если', () => {

    it('цена выше рынка более чем на 15%', () => {
        const offer = offerObj.withPrice(1200000).value();
        const tree = shallowRenderComponent(offer);

        expect((tree.instance() as any).getHeadText()).toMatchSnapshot();
    });

    it('цена выше рынка менее чем на 15%', () => {
        const offer = offerObj.withPrice(1050000).value();
        const tree = shallowRenderComponent(offer);

        expect((tree.instance() as any).getHeadText()).toMatchSnapshot();
    });

    it('цена ниже рынка', () => {
        const offer = offerObj.withPrice(800000).value();
        const tree = shallowRenderComponent(offer);

        expect((tree.instance() as any).getHeadText()).toMatchSnapshot();
    });

});

function shallowRenderComponent(offer: Offer) {
    return shallow(
        <SalePricePopupContent offer={ offer } marketIsStable={ true }/>,
        { context: { ...contextMock, store: mockStore({}) } },
    );
}
