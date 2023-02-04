import React from 'react';
import { render, fireEvent } from '@testing-library/react';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import SubscriptionModalEmail from './SubscriptionModalEmail';

const ContextProvider = createContextProvider(contextMock);

describe('SubscriptionModalEmail', () => {
    it('отправляет метрику если введен имейл и нажата кнопка "Отправить"', async() => {
        const emptyFunc = () => {};
        const { findByText } = render(
            <ContextProvider>
                <SubscriptionModalEmail
                    onClose={ emptyFunc }
                    onEmailSubmit={ emptyFunc }
                    visible={ true }
                    subscription={{ id: '123' }}
                />
            </ContextProvider>,
        );
        const element = document.querySelector('input');
        if (element) {
            fireEvent.change(element, { target: { value: 'example@example.com' } });
        }

        const btn = await findByText('Отправить');

        fireEvent.click(btn);

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('CLICK_SAVE_SEARCH');
    });
});
