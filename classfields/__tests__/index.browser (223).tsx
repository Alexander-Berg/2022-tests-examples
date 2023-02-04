import { render } from 'jest-puppeteer-react';

import React from 'react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { JournalExports } from '../index';

import { initialState } from './mocks';

describe('JournalExports', () => {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider initialState={initialState}>
                <JournalExports />
            </AppProvider>,
            {
                viewport: { width: 600, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
