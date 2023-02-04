import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import OfferCallComplaintModalContainer from '../container';

import mocks from './mocks';

const Component = ({ store }) => (
    <AppProvider initialState={store}>
        <OfferCallComplaintModalContainer />
    </AppProvider>
);

describe('OfferCallComplaintModal', () => {
    it('correct draw while fetching', async() => {
        await render(<Component store={mocks.fetching} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when error', async() => {
        await render(<Component store={mocks.error} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when call is too old', async() => {
        await render(<Component store={mocks.tooOld} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when call is not billed', async() => {
        await render(<Component store={mocks.notBilledCall} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when call complaint on moderation', async() => {
        await render(<Component store={mocks.onModeration} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when call without answer', async() => {
        await render(<Component store={mocks.withoutAnswer} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when complaint was cancelled', async() => {
        await render(<Component store={mocks.alreadyModeratedPass} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when complaint was approved', async() => {
        await render(<Component store={mocks.alreadyModeratedFail} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when complaint not filled', async() => {
        await render(<Component store={mocks.isAbleToComplainClear} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when complaint filled', async() => {
        await render(<Component store={mocks.isAbleToComplainFilled} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when complaint are reporting', async() => {
        await render(<Component store={mocks.isAbleToComplainLoading} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when complaint was success reported', async() => {
        await render(<Component store={mocks.isAbleToComplainSuccess} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when complaint was failed', async() => {
        await render(<Component store={mocks.isAbleToComplainFailed} />, { viewport: { width: 650, height: 450 } });
        await page.waitFor(300);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
