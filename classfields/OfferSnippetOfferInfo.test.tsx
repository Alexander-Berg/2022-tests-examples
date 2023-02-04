import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import { OfferStatus, OfferPosition_OrderedPosition_Sort } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';
import type { OfferPosition } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createLinkMock from 'autoru-frontend/mocks/createLinkMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import OfferSnippetOfferInfo from './OfferSnippetOfferInfo';

const customContext = {
    ...contextMock,
    linkToDesktop: createLinkMock('linkCabinet'),
};

const searchPositionsMock: Array<OfferPosition> = [ {
    positions: [
        { position: 100, sort: OfferPosition_OrderedPosition_Sort.RELEVANCE, total_count: 0 },
        { position: 5, sort: OfferPosition_OrderedPosition_Sort.PRICE, total_count: 0 },
    ],
    total_count: 200,
} ];

describe('не покажет поисковые позиции, если', () => {
    const offerObj = cloneOfferWithHelpers(offerMock)
        .withSearchPositions(searchPositionsMock)
        .withDaysInStock(5)
        .withDaysOnSale(6);

    it('объявление не размещено на авто.ру', () => {
        const offer = offerObj
            .withStatus(OfferStatus.ACTIVE)
            .withMultiposting({ status: OfferStatus.ACTIVE, classifieds: [] })
            .value();

        const tree = shallow(
            <OfferSnippetOfferInfo offer={ offer }/>,
            { context: { ...customContext } },
        );

        const titles = tree.find('.OfferSnippetOfferInfo__title');

        const searchPosRelevance = titles.findWhere(node => node.text() === 'В поиске');
        expect(searchPosRelevance.isEmptyRender()).toBe(true);

        const searchPosPrice = titles.findWhere(node => node.text() === 'По цене');
        expect(searchPosPrice.isEmptyRender()).toBe(true);
    });

    it('объявление не активно', () => {
        const offer = offerObj
            .withStatus(OfferStatus.INACTIVE)
            .withMultiposting()
            .value();

        const tree = shallow(
            <OfferSnippetOfferInfo offer={ offer }/>,
            { context: { ...customContext } },
        );

        const titles = tree.find('.OfferSnippetOfferInfo__title');

        const searchPosRelevance = titles.findWhere(node => node.text() === 'В поиске');
        expect(searchPosRelevance.isEmptyRender()).toBe(true);

        const searchPosPrice = titles.findWhere(node => node.text() === 'По цене');
        expect(searchPosPrice.isEmptyRender()).toBe(true);
    });
});
