import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { HeaderNavigation } from '../';

const renderOptions = { viewport: { width: 450, height: 100 } };

const initialState = {
    page: {
        name: 'partner-devchats',
        params: {},
    },
    config: {
        partnerUrl: 'https://partner.realty.ru',
    },
    client: {
        id: 1,
    },
};

describe('HeaderNavigation', () => {
    it('Рендерится', async () => {
        const WrappedComponent = (
            <AppProvider initialState={initialState}>
                <HeaderNavigation />
            </AppProvider>
        );

        await render(WrappedComponent, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
