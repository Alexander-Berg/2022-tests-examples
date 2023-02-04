import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { getCases } from 'realty-core/view/react/modules/filters/common/__tests__/test-cases';

import { AppProvider } from 'view/react/libs/test-helpers';

import OffersSearchFilters from '..';

import { getInitialState } from './mocks';

describe('OffersSearchFilters', () => {
    getCases('search').forEach(([ name, state ]) => it(name, async() => {
        await render(
            <AppProvider initialState={getInitialState(state)}>
                <OffersSearchFilters withSearchHistory={false} />
            </AppProvider>,
            { viewport: { width: 1200, height: 1200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }));
});
