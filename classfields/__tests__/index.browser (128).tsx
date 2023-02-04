import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SharedFavoritesSocialNetworks } from '../';

describe('SharedFavoritesSocialNetworks', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider>
                <SharedFavoritesSocialNetworks url="" />
            </AppProvider>,
            { viewport: { width: 345, height: 100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
