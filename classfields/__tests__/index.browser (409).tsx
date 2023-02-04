import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { DeveloperChatPromo } from '../';

import { item, longDeveloperItem } from './mocks';

describe('DeveloperChatPromo', () => {
    it('рендерится корректно', async () => {
        await render(
            <AppProvider disableSetTimeoutDelay>
                <DeveloperChatPromo item={item} page="newbuilding" />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится корректно с длинным названием застройщика', async () => {
        await render(
            <AppProvider disableSetTimeoutDelay>
                <DeveloperChatPromo item={longDeveloperItem} page="newbuilding" />
            </AppProvider>,
            { viewport: { width: 1000, height: 500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
