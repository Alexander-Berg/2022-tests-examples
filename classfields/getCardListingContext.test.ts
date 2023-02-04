jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

jest.mock('auto-core/react/lib/listingContext', () => {
    return {
        get: jest.fn(),
        save: jest.fn(),
    };
});

jest.mock('auto-core/server/blocks/listingContext/buildListingContext', () => jest.fn(() => 'newContext'));
jest.mock('auto-core/server/blocks/listingContext/mergeListingContexts', () => jest.fn(() => 'mergedContext'));

import listingMock from 'autoru-frontend/mockData/state/listing';

import gateApi from 'auto-core/react/lib/gateApi';
import type { TListingContext, TListingContextOffer } from 'auto-core/react/lib/listingContext';
import { get, save } from 'auto-core/react/lib/listingContext';

import buildListingContext from 'auto-core/server/blocks/listingContext/buildListingContext';
import mergeContexts from 'auto-core/server/blocks/listingContext/mergeListingContexts';

import getCardListingContext from './getCardListingContext';

it('не должен запрашивать новый поиск, если предыдущее и следующее объявления не граничные', () => {
    const offer = {} as Partial<TListingContextOffer> as TListingContextOffer;
    const context = {
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
    (get as jest.Mock).mockImplementation(() => context);
    return getCardListingContext('123', undefined).then((result) => {
        expect(gateApi.getResource).not.toHaveBeenCalled();
        expect(save).not.toHaveBeenCalled();
        expect(result).toEqual(context);
    });
});

it('должен запрашивать новый поиск и сохранить, если объявление граничное', () => {
    const offer = {} as Partial<TListingContextOffer> as TListingContextOffer;
    const context = {
        navigation: {
            next: { offer, sp: 31 },
            prev: { offer, sp: 29 },
            sp: 30,
            pager: {
                from: 1,
                to: 30,
                total_offers_count: 100,
                page_size: 30,
            },
        },
        key: 1234567890,
        expires: 2234567890,
        data: { query: { section: 'used', category: 'cars', currency: 'RUR' }, ids: [], ts: 1, expires: 1 },
    } as Partial<TListingContext> as TListingContext;
    (get as jest.Mock).mockImplementation(() => context);
    (gateApi.getResource as jest.Mock).mockImplementation(() => {
        return Promise.resolve(listingMock);
    });
    return getCardListingContext('123', '456').then(() => {
        expect(gateApi.getResource).toHaveBeenCalledWith('search', { category: 'cars', currency: 'RUR', page: 2, section: 'used' });
        expect(save).toHaveBeenCalledWith({
            data: 'mergedContext',
            key: 1234567890,
            expires: 2234567890,
            position: 30,
            group_id: '456',
            sale_id: '123',
        });
    });
});

it('должен построить контекст по новому поиску и сморжить контексты', () => {
    const offer = {} as Partial<TListingContextOffer> as TListingContextOffer;
    const context = {
        navigation: {
            next: { offer, sp: 31 },
            prev: { offer, sp: 29 },
            sp: 30,
            pager: {
                from: 1,
                to: 30,
                total_offers_count: 100,
                page_size: 30,
            },
        },
        data: { query: { section: 'used', category: 'cars', currency: 'RUR' }, ids: [], ts: 1, expires: 1 },
    } as Partial<TListingContext> as TListingContext;
    (get as jest.Mock).mockImplementation(() => context);
    const pr = Promise.resolve(listingMock);
    (gateApi.getResource as jest.Mock).mockImplementation(() => pr);
    return getCardListingContext('123', undefined).then(() => {
        return pr.then(() => {
            expect(buildListingContext).toHaveBeenCalledWith(listingMock);
            expect(mergeContexts).toHaveBeenCalledWith(
                30,
                { expires: 1, ids: [], query: { category: 'cars', currency: 'RUR', section: 'used' }, ts: 1 },
                'newContext',
            );
        });
    });
});
