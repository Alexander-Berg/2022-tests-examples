import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import OffersSerpSuggestionPresets from '..';

import { initialState, suggestions } from './mocks';

describe('Рендер фастлинок', () => {
    const TEST_CASES = [
        {
            title: 'Короткий контент',
            suggestions: suggestions[0],
            initialState,
        },
        {
            title: 'Длинный контент',
            suggestions: suggestions[1],
            initialState,
        },
        {
            title: 'Очень длинный контент',
            suggestions: suggestions[2],
            initialState,
        },
    ];

    TEST_CASES.map((testCase) =>
        it(testCase.title, async () => {
            const { initialState, suggestions } = testCase;

            await render(
                // eslint-disable-next-line @typescript-eslint/no-empty-function
                <AppProvider initialState={initialState} context={{ observeIntersection: () => {} }}>
                    <OffersSerpSuggestionPresets suggestions={suggestions} />
                </AppProvider>
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        })
    );
});
