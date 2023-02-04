import React from 'react';
import { render } from 'jest-puppeteer-react';

// eslint-disable-next-line no-restricted-imports
import { rejectPromise, infinitePromise } from 'realty-www/view/react/libs/test-helpers';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { SubscriptionDeleteConfirmationScreen } from '../';

const ComponentInContainer = props => (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
        <SubscriptionDeleteConfirmationScreen {...props} />
    </div>
);

describe('SubscriptionDeleteConfirmationScreenM', () => {
    it('open modal in default state', async() => {
        await render(
            <ComponentInContainer />,
            { viewport: { width: 350, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in loading state after submit button click', async() => {
        await render(
            <ComponentInContainer
                onConfirm={infinitePromise()}
            />,
            { viewport: { width: 350, height: 300 } }
        );

        await page.click('[data-test=subscription-delete-submit-button]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('open modal in failed state after rejected submitting', async() => {
        await render(
            <ComponentInContainer
                onConfirm={rejectPromise()}
            />,
            { viewport: { width: 350, height: 300 } }
        );

        await page.click('[data-test=subscription-delete-submit-button]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
