import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { getCases } from 'realty-core/view/react/modules/filters/common/__tests__/test-cases';

import ContentWrapper from 'realty-core/view/react/common/components/wrappers/ContentWrapper';

import { AppProvider } from 'view/react/libs/test-helpers';

import OffersMainSearchFilters from '..';

import { getInitialState } from './mocks';

// eslint-disable-next-line no-undef
global.BUNDLE_LANG = 'ru';

describe('OffersMainSearchFilters', () => {
    getCases('main').forEach(([ name, state, action ]) => it(name, async() => {
        await render(
            <AppProvider initialState={getInitialState(state)}>
                <ContentWrapper style={{ backgroundColor: 'grey', paddingTop: '44px', height: '400px' }}>
                    <OffersMainSearchFilters />
                </ContentWrapper>
            </AppProvider>,
            { viewport: { width: 1000, height: 420 } }
        );

        if (action) {
            await action(page);
        }

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }));
});
