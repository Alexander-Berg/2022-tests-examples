import React from 'react';
import { render } from '@vertis/jest-puppeteer-react';

import Button from '../Button';

describe('Button2', () => {
    jest.setTimeout(60000);
    it('should render correctly', async() => {
        await render(<Button label="Button2"/>, {
            viewport: { width: 200, height: 100, deviceScaleFactor: 1 },
        });

        const screenshot = await page.screenshot();
        expect(screenshot).toMatchImageSnapshot();
    });

    it.each([ 'test 1', 'test 2' ])('should render correctly %s', async(t) => {
        await render(<Button label={ `Button2 in ${ t }` }/>, {
            viewport: { width: 100, height: 100, deviceScaleFactor: 1 },
        });

        const screenshot = await page.screenshot();
        expect(screenshot).toMatchImageSnapshot();
    });
    describe('with describe', () => {
        it('should render correctly', async() => {
            await render(<Button label="Button2 with describe"/>, {
                viewport: { width: 100, height: 100, deviceScaleFactor: 1 },
            });

            const screenshot = await page.screenshot();
            expect(screenshot).toMatchImageSnapshot();
        });
    });

    describe.each([ 'describe 1', 'describe 2' ])('with %s', (d) => {
        it('should render correctly 2', async() => {
            await render(<Button label={ `Button2 in ${ d }` }/>, {
                viewport: { width: 100, height: 100, deviceScaleFactor: 1 },
            });

            const screenshot = await page.screenshot();
            expect(screenshot).toMatchImageSnapshot();
        });
    });

    describe.each([ 'describe 1', 'describe 2' ])('with %s', (d) => {
        it.each([ 'test 1', 'test 2' ])('should render correctly %s', async(t) => {
            await render(<Button label={ `Button2 in ${ d } and ${ t }` }/>, {
                viewport: { width: 100, height: 100, deviceScaleFactor: 1 },
            });

            const screenshot = await page.screenshot();
            expect(screenshot).toMatchImageSnapshot();
        });
    });
});
