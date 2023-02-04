jest.mock('auto-core/react/lib/getCardListingContext');

import React from 'react';
import { shallow } from 'enzyme';

import { ContextBlock, ContextPage } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import getCardListingContext from 'auto-core/react/lib/getCardListingContext';
import type {
    TListingContextOffer,
    TListingContext,
} from 'auto-core/react/lib/listingContext';

import CardPrevNext from './CardPrevNext';
import type {
    AbstractProps as PrevNextProps,
    AbstractState as PrevNextState,
} from './CardPrevNext';

const getCardListingContextMock = getCardListingContext as jest.MockedFunction<typeof getCardListingContext>;

class ComponentMock extends CardPrevNext<PrevNextProps, PrevNextState> {
    render() {
        return <div/>;
    }
}

it('должен отреднерить правильные ссылки пред/след для объявлений', () => {
    const offer1 = {
        'autoru-id': '123',
        'autoru-hash-code': 'abc',
        mark: { id: 'audi' },
        model: { id: 'a4' },
        state: 'used',
    } as TListingContextOffer;

    const offer2 = {
        'autoru-id': '456',
        'autoru-hash-code': 'def',
        mark: { id: 'audi' },
        model: { id: 'a4' },
        state: 'new',
    } as TListingContextOffer;

    const context = {
        navigation: {
            next: { offer: offer1, sp: 10 },
            prev: { offer: offer2, sp: 8 },
            sp: 9,
            pager: {
                from: 1,
                to: 30,
                total_offers_count: 100,
            },
        },
    } as unknown as TListingContext;

    const pr = Promise.resolve(context);
    getCardListingContextMock.mockImplementation(() => pr);
    const tree = shallow(
        <ComponentMock
            contextBlock={ ContextBlock.BLOCK_CARD }
            contextPage={ ContextPage.PAGE_CARD }
            offerID="098"
            searchID="42"
            pageParams={{}}
        />,
        { context: { store: mockStore({}), ...contextMock } },
    );

    return pr.then(() => {
        const instance = tree.instance() as ComponentMock;
        expect(instance.getLinkToOffer(offer1)).toEqual('link/card/?category=cars&section=used&mark=audi&model=a4&sale_id=123&sale_hash=abc');
        expect(instance.getLinkToOffer(offer2)).toEqual('link/card/?category=cars&section=new&mark=audi&model=a4&sale_id=456&sale_hash=def');
    });
});

it('должен отреднерить правильные ссылки пред/след для группы', () => {
    const offer1 = {
        category: 'cars',
        mark: { id: 'audi' },
        model: { id: 'a4' },
        'autoru-id': '123',
        'autoru-hash-code': 'abc',
        section: 'new',
        tech_param_id: '123456789',
        complectation_id: '0',
    } as TListingContextOffer;

    const offer2 = {
        category: 'cars',
        mark: { id: 'audi' },
        model: { id: 'a4' },
        'autoru-id': '123',
        'autoru-hash-code': 'abc',
        section: 'new',
        tech_param_id: '987654321',
        complectation_id: '12345',
    } as TListingContextOffer;

    const context = {
        navigation: {
            next: { offer: offer1, sp: 10 },
            prev: { offer: offer2, sp: 8 },
            sp: 9,
            pager: {
                from: 1,
                to: 30,
                total_offers_count: 100,
            },
        },
    } as unknown as TListingContext;

    const pr = Promise.resolve(context);
    getCardListingContextMock.mockImplementation(() => pr);

    const tree = shallow(
        <ComponentMock
            contextBlock={ ContextBlock.BLOCK_CARD }
            contextPage={ ContextPage.PAGE_CARD }
            offerID="098"
            searchID="42"
            pageParams={{}}
        />,
        { context: { store: mockStore({}), ...contextMock } },
    );

    return pr.then(() => {
        const instance = tree.instance() as ComponentMock;
        expect(instance.getLinkToOffer(offer2))
            .toEqual('link/card-group/?category=cars&section=new&mark=audi&model=a4&tech_param_id=987654321&complectation_id=12345');
        expect(instance.getLinkToOffer(offer1))
            .toEqual('link/card-group/?category=cars&section=new&mark=audi&model=a4&tech_param_id=123456789&complectation_id=0');
    });
});
