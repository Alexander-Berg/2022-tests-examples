import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import AuthorizedReport from '../';

import { mock } from './excerptMock';

const spoilerButtonSelector = 'button[class^="Spoiler__button"]';

describe('EGRNAuthorizedReport', () => {
    it('should match mobile EGRN report for authorized users screenshot', async() => {
        await render(
            <AuthorizedReport
                excerptReport={mock}
                offer={{ offerType: 'SELL' }}
            />,
            { viewport: { width: 350, height: 1200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should match extended mobile EGRN report for authorized users screenshot', async() => {
        await render(
            <AuthorizedReport
                excerptReport={mock}
                offer={{ offerType: 'SELL' }}
            />,
            { viewport: { width: 350, height: 2300 } }
        );

        await page.click(spoilerButtonSelector);
        await page.click(spoilerButtonSelector);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится для арендных отчётов со скрытыми предыдущими собственниками', async() => {
        await render(
            <AuthorizedReport
                excerptReport={mock}
                offer={{ offerType: 'RENT' }}
            />,
            { viewport: { width: 350, height: 1200 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('should render excerpt without owner rights correctly', async() => {
        const modifiedMock = {
            ...mock,
            flatReport: {
                ...mock.flatReport,
                currentRights: [],
                previousRights: []
            }
        };

        await render(
            <AuthorizedReport
                excerptReport={modifiedMock}
                offer={{ offerType: 'SELL' }}
            />,
            { viewport: { width: 350, height: 900 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
