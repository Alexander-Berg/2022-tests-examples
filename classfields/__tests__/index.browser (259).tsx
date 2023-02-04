import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { OfferCardSliderSnippet } from '../';

import { mockSell, mockRent, initialState } from './mocks';

it('Сниппет продажа', async () => {
    await render(
        <AppProvider initialState={initialState}>
            <OfferCardSliderSnippet item={mockSell} />
        </AppProvider>,
        { viewport: { width: 256, height: 256 } }
    );

    expect(await takeScreenshot()).toMatchImageSnapshot();
});

it('Сниппет аренда', async () => {
    await render(
        <AppProvider initialState={initialState}>
            <OfferCardSliderSnippet item={mockRent} />
        </AppProvider>,
        { viewport: { width: 256, height: 256 } }
    );

    expect(await takeScreenshot()).toMatchImageSnapshot();
});
