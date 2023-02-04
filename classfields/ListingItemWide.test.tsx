jest.mock('auto-core/react/dataDomain/state/actions/sellerPopupOpen');

import React from 'react';
import { Provider } from 'react-redux';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mockAllIsIntersecting } from 'react-intersection-observer/test-utils';

import '@testing-library/jest-dom';

import { ContextBlock, ContextPage } from '@vertis/schema-registry/ts-types-snake/auto/api/stat_events';

import mockStore from 'autoru-frontend/mocks/mockStore';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { TSearchParameters } from 'auto-core/types/TSearchParameters';

import type { Props } from './ListingItemWide';
import ListingItemWide from './ListingItemWide';

it('рендерит ссылку на публичный профиль перекупа и отправляет метрику на клик, если пришел encrypted_user_id', () => {
    const props: Props = {
        contextBlock: ContextBlock.BLOCK_LISTING,
        contextPage: ContextPage.PAGE_LISTING,
        geo: [],
        geoRadius: 0,
        index: 0,
        offer: cloneOfferWithHelpers(offerMock).withEncryptedUserId('some_encrypted_id').value(),
        params: {},
        searchParams: {},
        sellerPopupOpen: jest.fn(),
        sendMarketingEventByListingOffer: jest.fn(),
    };

    render(getComponentForTestingLibrary(props));

    const link = screen.getByRole('link', { name: /дмитрий/i });

    expect(link.getAttribute('href')).toBe('link/reseller-public-page/?encrypted_user_id=some_encrypted_id');

    userEvent.click(link);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_public', 'link-click' ]);
});

it('не рендерит ссылку на публичный профиль перекупа, если пришел encrypted_user_id, но пользователь уже на листинге перекупа', () => {
    const props: Props = {
        contextBlock: ContextBlock.BLOCK_LISTING,
        contextPage: ContextPage.PAGE_LISTING,
        geo: [],
        geoRadius: 0,
        index: 0,
        offer: cloneOfferWithHelpers(offerMock).withEncryptedUserId('some_encrypted_id').value(),
        params: { encrypted_user_id: 'some_encrypted_id' } as TSearchParameters,
        searchParams: {},
        sellerPopupOpen: jest.fn(),
        sendMarketingEventByListingOffer: jest.fn(),
    };

    render(getComponentForTestingLibrary(props));

    const link = screen.queryByRole('link', { name: /дмитрий/i });

    expect(link).not.toBeInTheDocument();
});

it('не рендерит ссылку на публичный профиль перекупа, если нет encrypted_user_id', () => {
    const props: Props = {
        contextBlock: ContextBlock.BLOCK_LISTING,
        contextPage: ContextPage.PAGE_LISTING,
        geo: [],
        geoRadius: 0,
        index: 0,
        offer: offerMock,
        params: {} as TSearchParameters,
        searchParams: {},
        sellerPopupOpen: jest.fn(),
        sendMarketingEventByListingOffer: jest.fn(),
    };

    render(getComponentForTestingLibrary(props));

    const link = screen.queryByRole('link', { name: /дмитрий/i });

    expect(link).not.toBeInTheDocument();
});

it('отправляет метрику при попадании ссылки в зону видимости', () => {
    const offer = cloneOfferWithHelpers(offerMock).withEncryptedUserId('some_encrypted_id').value();

    const props: Props = {
        contextBlock: ContextBlock.BLOCK_LISTING,
        contextPage: ContextPage.PAGE_LISTING,
        geo: [],
        geoRadius: 0,
        index: 0,
        offer,
        params: {} as TSearchParameters,
        searchParams: {},
        sellerPopupOpen: jest.fn(),
        sendMarketingEventByListingOffer: jest.fn(),
    };

    render(getComponentForTestingLibrary(props));
    mockAllIsIntersecting(true);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'reseller_public', 'link-show' ]);
});

function getComponentForTestingLibrary(props: Props) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore({});

    return (
        <ContextProvider>
            <Provider store={ store }>
                <ListingItemWide { ...props }/>
            </Provider>
        </ContextProvider>
    );
}
