import React from 'react';
import { advanceTo } from 'jest-date-mock';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { CardContactsModal } from '../';

import { testCases } from './mocks';

advanceTo(new Date('2022-02-24T12:20:00.111Z'));

describe('CardContactsModal', () => {
    testCases.map(({ title, props, options }) => {
        it(title, async () => {
            await render(
                <AppProvider>
                    <CardContactsModal {...props} />
                </AppProvider>,
                options
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
