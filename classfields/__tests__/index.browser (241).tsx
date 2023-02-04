import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { MortgageSearchPresets } from '../';

const rgid = 741965; // rgid для СПб и ЛО;

const Component = () => (
    <AppProvider>
        <MortgageSearchPresets rgid={rgid} />
    </AppProvider>
);

describe('MortgageSearchPresets', () => {
    it('рендерится корректно', async () => {
        await render(<Component />, {
            viewport: {
                width: 320,
                height: 200,
            },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
