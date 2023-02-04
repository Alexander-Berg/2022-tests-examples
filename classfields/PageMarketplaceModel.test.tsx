jest.mock('./MarketplaceModelTechBlock/MarketplaceModelTechBlock', () => () => null);
jest.mock('./MarketplaceSameClassBlock/MarketplaceSameClassBlock', () => () => null);
jest.mock('./MarketplaceOffersBlock/MarketplaceOffersBlock', () => () => null);
jest.mock('./MarketplaceReviewsBlock/MarketplaceReviewsBlock', () => () => null);
jest.mock('./MarketplaceReviewsFeaturesBlock/MarketplaceReviewsFeaturesBlock', () => () => null);
jest.mock('auto-core/react/components/common/CrossLinks/CrossLinks', () => () => null);
jest.mock('www-mobile/react/components/ListingAllConfigurationsMobile/ListingAllConfigurationsMobile', () => () => null);
jest.mock('auto-core/react/components/common/Ad/Ad', () => () => null);
jest.mock('www-mobile/react/components/MarketplaceJournal/MarketplaceJournal', () => () => null);
jest.mock('www-mobile/react/components/MarketplaceInfinityListing/MarketplaceInfinityListing', () => () => null);

jest.mock('react-redux', () => {
    return {
        useSelector: jest.fn().mockReturnValue([]),
    };
});

import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

const ContextProvider = createContextProvider(contextMock);

import PageMarketplaceModel from './PageMarketplaceModel';

it('отправит метрику по клику на тайтл', () => {
    render(
        <ContextProvider>
            <PageMarketplaceModel/>
        </ContextProvider>,
    );
    const title = document.querySelector('.BigImageGallery__overlayContent > .Link') as Element;
    userEvent.click(title);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-model', 'blocks', 'gallery', 'listing-link', 'click' ]);
});
