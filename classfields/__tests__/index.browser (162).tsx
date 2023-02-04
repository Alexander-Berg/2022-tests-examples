import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import { EgrnEmpty } from '../';

const defalutStore = {
    geo: {
        rgid: 0,
    },
};

const Component = ({ store = defalutStore }) => (
    <AppProvider initialState={store}>
        <EgrnEmpty />
    </AppProvider>
);

describe('EgrnEmpty', () => {
    it('пустая старница', async () => {
        await render(<Component />, { viewport: { width: 1000, height: 400 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
