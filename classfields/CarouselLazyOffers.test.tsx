import React from 'react';
import { shallow } from 'enzyme';

import { ContextBlock, ContextPage } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';
import offersMock from 'autoru-frontend/mockData/responses/offer.mock';

// eslint-disable-next-line import/no-restricted-paths
import ListingCarouselItem from 'auto-core/react/components/desktop/ListingCarouselItem/ListingCarouselItem';
import gateApi from 'auto-core/react/lib/gateApi';

import CarouselOffers from '../CarouselOffers/CarouselOffers';
import CarouselOffersEmptyItem from '../CarouselOffersEmptyItem/CarouselOffersEmptyItem';

import CarouselLazyOffers from './CarouselLazyOffers';

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const getResource = gateApi.getResource as jest.MockedFunction<typeof gateApi.getResource>;

const renderTitle = () => <span> Какой-то заголовок </span>;
const renderFooter = () => <span> Какой-то футер </span>;

it('должен отрендерить карусель после того, как сработает intersectionObserver и будут получены данные', () => {
    const gateApiPromise = Promise.resolve(offersMock.response);
    getResource.mockImplementation(() => gateApiPromise);
    const tree = shallow(
        <CarouselLazyOffers
            contextBlock={ ContextBlock.BLOCK_CARD }
            contextPage={ ContextPage.PAGE_CARD }
            itemComponent={ ListingCarouselItem }
            itemEmptyComponent={ CarouselOffersEmptyItem }
            title={ renderTitle }
            footer={ renderFooter }
            resourceName="someName"
            resourceParams={{}}
        />,
        { context: contextMock },

    ).dive();
    tree.find('InView').simulate('change', true);
    return gateApiPromise.then(() => {
        expect(tree.find('InView').dive().find(CarouselOffers)).toHaveLength(1);
    });
});
