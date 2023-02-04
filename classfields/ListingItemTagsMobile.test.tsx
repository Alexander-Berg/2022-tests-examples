import React from 'react';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';

import mockStore from 'autoru-frontend/mocks/mockStore';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import ListingItemTagsMobile from './ListingItemTagsMobile';

const Context = createContextProvider({
    metrika: {
        reachGoal: () => {},
        params: () => {},
        sendPageEvent: () => {},
        sendPageAuthEvent: () => {},
    },
    hasExperiment: () => {},
});

describe('ListingItemTagsMobile', () => {
    const store = mockStore({});

    it('рисует бейджик не растаможен', async() => {
        const offer = {
            tags: [ 'custom_not_cleared_without_pts' ],
        } as Offer;

        const { findAllByText } = await render(
            <Context>
                <Provider store={ store }>
                    <ListingItemTagsMobile offer={ offer }/>
                </Provider>
            </Context>,
        );

        expect(await findAllByText('Не растаможен')).toHaveLength(1);
    });

    it('рисует бейджик Растаможен, нет ПТС', async() => {
        const offer = {
            tags: [ 'custom_cleared_without_pts' ],
        } as Offer;

        const { findAllByText } = await render(
            <Context>
                <Provider store={ store }>
                    <ListingItemTagsMobile offer={ offer }/>
                </Provider>
            </Context>,
        );

        expect(await findAllByText('Растаможен, нет ПТС')).toHaveLength(1);
    });
});
