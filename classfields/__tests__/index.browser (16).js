import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import dayjs from '@realty-front/dayjs';

import VasServiceProgressBar from '../index';

const service = {
    end: Number(dayjs('2019-10-28'))
};

describe('VasServiceProgressBar', () => {
    it('Six days remaining', async() => {
        const now = Number(dayjs('2019-10-22'));

        await render(
            <VasServiceProgressBar service={service} duration={7} now={now} />,
            { viewport: { width: 300, height: 10 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('One day remaining', async() => {
        const now = Number(dayjs('2019-10-27'));

        await render(
            <VasServiceProgressBar service={service} duration={7} now={now} />,
            { viewport: { width: 300, height: 10 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Renewal enabled', async() => {
        const now = Number(dayjs('2019-10-27'));

        await render(
            <VasServiceProgressBar service={service} duration={7} now={now} isFull />,
            { viewport: { width: 300, height: 10 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
