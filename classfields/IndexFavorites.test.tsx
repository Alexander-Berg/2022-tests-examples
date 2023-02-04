jest.mock('auto-core/react/dataDomain/callsStats/actions/getCallsStats');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import { InView } from 'react-intersection-observer';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import getCallsStats from 'auto-core/react/dataDomain/callsStats/actions/getCallsStats';
import favoritesMock from 'auto-core/react/dataDomain/favorites/mocks';
import cardStateMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import type { DesktopAppState } from 'www-desktop/react/DesktopAppState';

import IndexFavorites from './IndexFavorites';

const getCallsStatsMock = getCallsStats as jest.MockedFunction<typeof getCallsStats>;
getCallsStatsMock.mockImplementation(() => () => Promise.resolve());

let initialState: Pick<DesktopAppState, 'favorites' | 'callsStats'>;

beforeEach(() => {
    initialState = {
        favorites: favoritesMock.value(),
        callsStats: {},
    };
});

describe('запрос статистики звонков', () => {
    beforeEach(() => {
        initialState.favorites = favoritesMock.withOffers([
            cloneOfferWithHelpers(cardStateMock).withSaleId('111-aaa').value(),
            cloneOfferWithHelpers(cardStateMock).withSaleId('222-bbb').value(),
            cloneOfferWithHelpers(cardStateMock).withSaleId('333-ccc').withStatus(OfferStatus.INACTIVE).value(),
            cloneOfferWithHelpers(cardStateMock).withSaleId('444-ddd').value(),
            cloneOfferWithHelpers(cardStateMock).withSaleId('555-eee').value(),
            cloneOfferWithHelpers(cardStateMock).withSaleId('666-fff').value(),
            cloneOfferWithHelpers(cardStateMock).withSaleId('777-ggg').value(),
        ]).value();
    });

    it('вызовется для первых пяти активных офферов', () => {
        const page = shallowRenderComponent({ initialState });

        expect(getCallsStatsMock).toHaveBeenCalledTimes(0);

        const observer = page.find(InView);
        observer.simulate('change', true);

        expect(getCallsStatsMock).toHaveBeenCalledTimes(5);
    });
});

function shallowRenderComponent({ initialState }: { initialState: Pick<DesktopAppState, 'favorites' | 'callsStats'> }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <IndexFavorites/>
            </Provider>
        </ContextProvider>,
    );

    return page.dive().dive().dive();
}
