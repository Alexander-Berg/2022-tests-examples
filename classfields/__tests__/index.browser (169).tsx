import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import { SmartPhotoPanel } from '../index';

const defaultState = {
    offerForm: {
        imageOrderChangeAllowed: false,
    },
};

const Component = ({ store = defaultState }) => (
    <AppProvider initialState={store}>
        <SmartPhotoPanel />
    </AppProvider>
);

describe('SmartPhotoPanel', () => {
    it('отображение по-умолчанию', async () => {
        await render(<Component />, { viewport: { width: 930, height: 260 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('на ширине 740', async () => {
        await render(<Component />, { viewport: { width: 740, height: 260 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('с отмеченной галочкой', async () => {
        await render(
            <Component
                store={{
                    offerForm: {
                        imageOrderChangeAllowed: true,
                    },
                }}
            />,
            { viewport: { width: 930, height: 260 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
