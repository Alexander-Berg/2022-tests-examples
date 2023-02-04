jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: () => Promise.resolve(42),
}));

import { render, screen } from '@testing-library/react';
import _ from 'lodash';
import React from 'react';
import { Provider } from 'react-redux';

import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/auction/promo_campaign/api_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import AuctionUsedAutobidderTableRow from './AuctionUsedAutobidderTableRow';

const Context = createContextProvider(contextMock);
const state = {};
const store = mockStore(state);

const campaign = {
    max_offer_daily_calls: '20',
    isPristine: false,
    id: '51',
    name: 'мерседесы',
    bidding_algorithm: {
        max_position_for_price: {
            max_bid: '0',
        },
    },
    period: {},
    status: Status.ACTIVE,
    description: '',
};

it('должен написать текст про параметры, если пользователь забыл ввести Макс. стоимость звонка (max_bid)', () => {
    render(
        <Context>
            <Provider store={ store }>
                <table>
                    <AuctionUsedAutobidderTableRow
                        onResize={ () => {} }
                        shouldRenderListing={ true }
                        campaign={ campaign }
                        index={ 0 }
                        toggleRow={ _.noop }
                        isOpened={ true }
                    />
                </table>
            </Provider>
        </Context>,
    );
    expect(screen.getByText('Установите состояние и параметры объявлений и посмотрите как распределится интерес')).not.toBeNull();
});
