import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { getCases } from 'realty-core/view/react/modules/filters/common/__tests__/test-cases';

import OffersSearchFilters from '..';

import { getInitialState } from './mocks';

import '../../Page/styles.css';
import '../../pages/FiltersPage/styles.css';

describe('OffersSearchFiltersTouch', () => {
    getCases('search').forEach(([ name, state ]) => it(name, async() => {
        const initState = getInitialState(state);

        await render(
            <AppProvider initialState={initState}>
                <div className='Page Page_type_filters'>
                    <div className='PageLayout _withFilters'>
                        <OffersSearchFilters {...initState.filters.offers} />
                    </div>
                </div>
            </AppProvider>,
            { viewport: { width: 400, height: 3200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }));
});
