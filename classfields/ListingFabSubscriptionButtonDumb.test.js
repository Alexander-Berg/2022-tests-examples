import React from 'react';
import _ from 'lodash';
import { render, fireEvent } from '@testing-library/react';
import ListingFabSubscriptionButtonDumb from './ListingFabSubscriptionButtonDumb';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import flushPromises from 'autoru-frontend/jest/unit/flushPromises';

const ContextProvider = createContextProvider(contextMock);

describe('ListingFabSubscriptionButtonDumb', () => {
    it('отправляет цель при успешном сохранении поиска', async() => {
        const onCreate = jest.fn(() => Promise.resolve());
        render(
            <ContextProvider>
                <ListingFabSubscriptionButtonDumb
                    className="ListingFabSubscriptionButtonDumb"
                    onCreate={ onCreate }
                    onDelete={ _.noop }
                    onEmailSubmit={ _.noop }
                    userEmail="example@exmaple.com"
                />
            </ContextProvider>,
        );

        const element = document.querySelector('.ListingFabSubscriptionButtonDumb');
        if (element) {
            fireEvent.click(element);
        }

        await flushPromises();

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CLICK_SAVE_SEARCH');
    });
});
