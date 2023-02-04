import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionOptionsModal } from '../';

const mockSubscription = {
    id: '12345',
    email: 'someone@mail.ru',
    title: 'Подписка на что-то',
    description: 'По такому-то адресу',
    frequency: 60
};

describe('SubscriptionOptionsModalM', () => {
    it('option selector when not given "screen" prop', async() => {
        await render(
            <SubscriptionOptionsModal
                isOpen
                renderToOverlay
            />,
            { viewport: { width: 350, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('delete confirmation screen when given screen="delete" prop', async() => {
        await render(
            <SubscriptionOptionsModal
                isOpen
                renderToOverlay
                screen='delete'
                subscription={mockSubscription}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('delete confirmation screen when given screen="settings" prop', async() => {
        await render(
            <SubscriptionOptionsModal
                isOpen
                renderToOverlay
                screen='settings'
                subscription={mockSubscription}
            />,
            { viewport: { width: 350, height: 400 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
