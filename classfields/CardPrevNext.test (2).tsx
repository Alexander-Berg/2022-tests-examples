import 'jest-enzyme';
import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

jest.mock('auto-core/react/lib/getCardListingContext', () => jest.fn());

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import imageSmall from 'autoru-frontend/mockData/images/small';

import getCardListingContext from 'auto-core/react/lib/getCardListingContext';
import type { TListingContext, TListingContextOffer } from 'auto-core/react/lib/listingContext';
import configStateMock from 'auto-core/react/dataDomain/config/mock';

import type { MobileAppState } from 'www-mobile/react/MobileAppState';

import CardPrevNext from './CardPrevNext';

let context: TListingContext;
let offer: TListingContextOffer;
beforeEach(() => {
    offer = {
        'autoru-id': '456',
        'autoru-hash-code': 'def',
        mark: { id: 'audi' },
        model: { id: 'a4' },
        state: 'new',
        imageUrl: imageSmall,
    } as Partial<TListingContextOffer> as TListingContextOffer;

    context = {
        navigation: {
            next: { offer, sp: 10 },
            prev: { offer, sp: 8 },
            sp: 9,
            pager: {
                from: 1,
                to: 30,
                total_offers_count: 100,
            },
        },
    } as Partial<TListingContext> as TListingContext;
});

describe('если есть информация о контексте листинга', () => {
    let tree: ShallowWrapper;
    let store: MobileAppState;
    let pr: Promise<TListingContext>;
    beforeEach(() => {
        pr = Promise.resolve(context);
        (getCardListingContext as jest.MockedFunction<typeof getCardListingContext>).mockImplementation(() => pr);
        store = mockStore({
            cookies: {},
            config: configStateMock.value(),
        }) as Partial<MobileAppState> as MobileAppState;
        tree = shallow(
            <CardPrevNext offerId="123" groupingId=""/>,
            { context: { store, ...contextMock } },
        ).dive();
    });

    it('должен отрисовать блок со ссылками', () => {
        return pr.then(() => expect(shallowToJson(tree)).toMatchSnapshot());
    });

    it('не должен передать для iOS обрабатчик свайпа назад', () => {
        store = mockStore({
            cookies: {},
            config: {
                data: {
                    browser: { OSFamily: 'iOS' },
                },
            },
        }) as Partial<MobileAppState> as MobileAppState;
        tree = shallow(
            <CardPrevNext offerId="123" groupingId=""/>,
            { context: { store, ...contextMock } },
        ).dive();

        return pr.then(() => expect(tree.find('Swiper')).toHaveProp('onSwipeRight', undefined));
    });
});

it('не нарисует блок, если переход был из групповой карточки', () => {
    const pr = Promise.resolve({ ...context, group_id: 'group_id' });
    (getCardListingContext as jest.MockedFunction<typeof getCardListingContext>)
        .mockImplementation(() => pr);

    const store = mockStore({
        cookies: {},
        config: configStateMock.value(),
    }) as Partial<MobileAppState> as MobileAppState;

    const tree = shallow(
        <CardPrevNext offerId="123" groupingId="group_id"/>,
        { context: { store, ...contextMock } },
    ).dive();

    return pr.then(() => expect(tree.isEmptyRender()).toBe(true));
});

describe('если нет информации о контексте листинга', () => {
    let tree: ShallowWrapper;
    const store = mockStore({
        cookies: {},
        config: configStateMock.value(),
    }) as Partial<MobileAppState> as MobileAppState;

    beforeEach(() => {
        tree = shallow(
            <CardPrevNext offerId="123" groupingId=""/>,
            { context: { store, ...contextMock } },
        ).dive();
    });

    it('не должен отрисовать блок со ссылками', () => {
        expect(tree).toBeEmptyRender();
    });
});
