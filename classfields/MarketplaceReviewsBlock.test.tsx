jest.mock('react-redux', () => {
    return {
        useSelector: jest.fn(),
    };
});

import userEvent from '@testing-library/user-event';
import React from 'react';
import { render } from '@testing-library/react';
import { useSelector } from 'react-redux';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import reviewsMock from 'auto-core/react/dataDomain/reviews/mocks/reviews.mock';

import MarketplaceReviewsBlock from './MarketplaceReviewsBlock';

const ContextProvider = createContextProvider({ ...contextMock, pageParams: {} });

const useSelectorMock = useSelector as jest.MockedFunction<typeof useSelector>;

it('отправит метрику по клику на отзыв', () => {
    renderComponent();
    const review = document.querySelector('.MarketplaceReviewsBlock__review:nth-child(1)') as Element;
    userEvent.click(review);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-model', 'blocks', 'reviews', 'review', 'click' ]);
});

it('отправит метрику по клику на кнопку показать больше', () => {
    renderComponent();
    const review = document.querySelector('.MarketplaceReviewsBlock__button') as Element;
    userEvent.click(review);
    expect(contextMock.metrika.sendParams).toHaveBeenCalledWith([ 'marketplace-model', 'blocks', 'reviews', 'show-all', 'click' ]);
});

function renderComponent() {
    useSelectorMock.mockReturnValue(reviewsMock);
    return render(
        <ContextProvider>
            <MarketplaceReviewsBlock
                title="Kalina"
            />
        </ContextProvider>,
    );
}
