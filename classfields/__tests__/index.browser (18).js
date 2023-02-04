import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import review from 'realty-core/view/react/modules/reviews/redux/reducer.js';
import createRootReducer from 'realty-core/view/react/libs/create-page-root-reducer';

import SocialObjectPointItem from '../index';

import { initialState, item, Gate } from './mock';

const renderComponent = (viewport = { width: 300, height: 550 }) =>
    render(
        <AppProvider initialState={initialState} Gate={Gate} rootReducer={createRootReducer({ review })}>
            <SocialObjectPointItem item={item} />
        </AppProvider>,
        { viewport }
    );

describe('SocialObjectPointItem', () => {
    it('дефолтный рендер', async() => {
        await renderComponent();

        await page.waitFor(100);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        const element = await page.$('.SocialObjectPointItem');
        const boundingBox = await element.boundingBox();

        await page.mouse.move(
            boundingBox.x + boundingBox.width / 2,
            boundingBox.y + boundingBox.height / 2
        );

        await page.mouse.wheel({ deltaY: 1000 });
        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
