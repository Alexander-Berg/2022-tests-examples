import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { OfferEGRNEncumbranceTile } from '../';

const OPTIONS = { viewport: { width: 300, height: 100 } };

const DEFAULT_PROPS = { title: 'title', description: 'couple of words about this tile' };

const TEST_CASES = [
    { type: 'ok', view: 'round', platform: 'desktop', disableHint: true },
    { type: 'ok', view: 'round', platform: 'desktop', disableHint: false },
    { type: 'ok', view: 'round', platform: 'mobile', disableHint: true },
    { type: 'ok', view: 'round', platform: 'mobile', disableHint: false },
    { type: 'ok', view: 'square', platform: 'desktop', disableHint: true },
    { type: 'ok', view: 'square', platform: 'desktop', disableHint: false },
    { type: 'ok', view: 'square', platform: 'mobile', disableHint: true },
    { type: 'ok', view: 'square', platform: 'mobile', disableHint: false },
    { type: 'warning', view: 'round', platform: 'desktop' },
    { type: 'warning', view: 'round', platform: 'mobile' },
    { type: 'warning', view: 'square', platform: 'desktop' },
    { type: 'warning', view: 'square', platform: 'mobile' },
] as const;

describe('OfferEGRNEncumbranceTile', () => {
    TEST_CASES.forEach((props) => {
        it(`should render with props: ${JSON.stringify(props)}`, async () => {
            await render(<OfferEGRNEncumbranceTile {...DEFAULT_PROPS} {...props} />, OPTIONS);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    it('should render long titles correctly', async () => {
        await render(
            <OfferEGRNEncumbranceTile
                title="long title long title long title long title long title"
                description="description"
                type="warning"
                platform="mobile"
                view="round"
            />,
            OPTIONS
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render long titles with hint correctly', async () => {
        await render(
            <OfferEGRNEncumbranceTile
                title="long title long title long title long title long title"
                description="description"
                type="ok"
                platform="mobile"
                view="round"
            />,
            OPTIONS
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render tiles without description correctly', async () => {
        await render(<OfferEGRNEncumbranceTile title="title" type="ok" platform="mobile" view="round" />, OPTIONS);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
