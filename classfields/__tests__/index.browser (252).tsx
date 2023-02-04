import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { NewbuildingPlans } from '../';

import { getInitialState, getProps } from './mocks';

describe('NewbuildingPlans', () => {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider
                initialState={getInitialState()}
                fakeTimers={{ now: new Date('2020-06-02T09:00:00.111Z').getTime() }}
            >
                <NewbuildingPlans {...getProps()} />
            </AppProvider>,
            { viewport: { width: 320, height: 3500 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
