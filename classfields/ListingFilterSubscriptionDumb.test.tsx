import React from 'react';
import { render, fireEvent } from '@testing-library/react';

import flushPromises from 'autoru-frontend/jest/unit/flushPromises';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import ListingFilterSubscriptionDumb from './ListingFilterSubscriptionDumb';

const ContextProvider = createContextProvider(contextMock);

describe('ListingFilterSubscriptionDumb', () => {
    it('отправляет цель при клике на кнопку "Сохранить поиск"', async() => {
        const onCreateSubscription = jest.fn(() => Promise.resolve());
        const { findByText } = render(
            <ContextProvider>
                <ListingFilterSubscriptionDumb
                    onCreateSubscription={ onCreateSubscription }
                />
            </ContextProvider>,
        );

        const element = await findByText('Сохранить поиск');
        fireEvent.click(element);
        await flushPromises();
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CLICK_SAVE_SEARCH');
    });
});
