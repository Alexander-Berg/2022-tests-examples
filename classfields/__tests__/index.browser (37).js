import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { daysByInterval } from 'view/lib/weekdays';

import { ProductFeedSchedulesModal } from '../index';

const props = {
    isOpened: true,
    schedules: [
        {
            daysOfWeek: daysByInterval.everyday,
            startTime: '2020-05-22T07:00:00Z'
        },
        {
            daysOfWeek: daysByInterval.weekday,
            startTime: '2020-05-22T17:00:00Z'
        },
        {
            daysOfWeek: daysByInterval.weekends,
            startTime: '2020-05-22T21:00:00Z'
        }
    ],
    onClose: noop
};

describe('ProductFeedSchedulesModal', () => {
    it('render by default', async() => {
        await render(
            <ProductFeedSchedulesModal {...props} />,
            { viewport: { width: 1100, height: 650 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
