import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { NewbuildingPlans } from '../';

import { getInitialState, getProps } from './mocks';

describe('NewbuildingPlans', () => {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider initialState={getInitialState()}>
                <NewbuildingPlans {...getProps()} />
            </AppProvider>,
            { viewport: { width: 1300, height: 2500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
