import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SiteSerpSnippet } from '..';

import { getSiteSerpSnippet, getInitialState } from './mocks';

// eslint-disable-next-line no-undef
global.BUNDLE_LANG = 'ru';

describe('SiteSerpSnippet', () => {
    it('рисует сниппет', async() => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <SiteSerpSnippet item={getSiteSerpSnippet()} />,
            </AppProvider>,
            { viewport: { width: 1000, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
