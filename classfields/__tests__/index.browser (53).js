import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/lib/test-helpers';

import ExcerptInfo from '../index';

const WIDTH = 400;
const HEIGHT = 300;
const WIDTH_MIN = 297;

const Component = ({ style, ...props }) => (
    <AppProvider>
        <div
            style={style || {
                marginTop: '70px',
                display: 'flex',
                justifyContent: 'flex-end'
            }}
        >
            <ExcerptInfo offerId='777' {...props} />
        </div>
    </AppProvider>
);

const STATUSES = [
    'missingDataForReport',
    'reportWillBeRequestedSoon',
    'reportWasRequested',
    'reportReceived',
    'incompleteReportReceived',
    'reportNotReceived'
];

describe('excerpt-info', () => {
    describe('reportIsAbsent', () => {
        it('Отрисовка по умолчанию', async() => {
            await render(<Component excerptStatus='reportIsAbsent' />, {
                viewport: { width: WIDTH, height: HEIGHT }
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    // eslint-disable-next-line no-unused-vars
    for (const status of STATUSES) {
        describe(status, () => {
            it('нормальная ширина, физик', async() => {
                await render(<Component excerptStatus={status} />, {
                    viewport: { width: WIDTH, height: HEIGHT }
                });

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('минимальная ширина, физик', async() => {
                await render(<Component excerptStatus={status} />, {
                    viewport: { width: WIDTH_MIN, height: HEIGHT }
                });

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('нормальная ширина, физик, ховер', async() => {
                await render(<Component excerptStatus={status} />, {
                    viewport: { width: WIDTH, height: HEIGHT }
                });

                await page.hover('[data-test=badge]');

                await page.waitFor(300);

                expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
            });

            it('нормальная ширина, юр. лицо, не из фида', async() => {
                await render(<Component excerptStatus={status} isJuridical />, {
                    viewport: { width: WIDTH, height: HEIGHT }
                });

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('минимальная ширина, юр. лицо, не из фида', async() => {
                await render(<Component excerptStatus={status} isJuridical />, {
                    viewport: { width: WIDTH_MIN, height: HEIGHT }
                });

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('нормальная ширина, юр. лицо, не из фида, hover', async() => {
                await render(<Component excerptStatus={status} isJuridical />, {
                    viewport: { width: WIDTH, height: HEIGHT }
                });

                await page.hover('[data-test=badge]');

                await page.waitFor(300);

                expect(await takeScreenshot({ keepCursor: true })).toMatchImageSnapshot();
            });

            it('нормальная ширина, юр. лицо, из фида', async() => {
                await render(<Component excerptStatus={status} isJuridical isFromFeed />, {
                    viewport: { width: WIDTH, height: HEIGHT }
                });

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('минимальная ширина, юр. лицо, из фида', async() => {
                await render(<Component excerptStatus={status} isJuridical isFromFeed />, {
                    viewport: { width: WIDTH_MIN, height: HEIGHT }
                });

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });
    }
});
