import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';

import { FlatValidationErrorModal } from '../index';

import { store } from './stubs/store';

const renderOptions = [
    {
        viewport: {
            width: 1200,
            height: 1000,
        },
    },
    {
        viewport: {
            width: 420,
            height: 800,
        },
    },
];

const Component: React.FunctionComponent<React.ComponentProps<typeof FlatValidationErrorModal>> = (props) => (
    <AppProvider initialState={store}>
        <FlatValidationErrorModal {...props} />
    </AppProvider>
);

describe('FlatValidationErrorModal', () => {
    describe('Базовый рендеринг', () => {
        renderOptions.forEach((option) => {
            it(`${option.viewport.width}px${option.viewport.height}px`, async () => {
                await render(<Component isOpen={true} closeModal={() => undefined} />, option);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    });
});
