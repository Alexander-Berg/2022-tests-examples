import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EmailConfirmationModal } from '../';

describe('EmailConfirmationModalMobile', () => {
    it('should render open modal with positive result', async() => {
        await render(
            <EmailConfirmationModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                emailConfirmationResult
            />,
            { viewport: { width: 350, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render open modal with rejected result', async() => {
        await render(
            <EmailConfirmationModal
                isOpen
                renderToOverlay
                onClose={() => {}}
                emailConfirmationResult={false}
            />,
            { viewport: { width: 350, height: 250 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
