import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { EGRNPaidReportBlock, VIEW } from '../';

const TEST_CASES = [
    {
        testDescription: 'рендерит белый блок',
        props: {
            title: 'my title',
            subtitle: 'my subtitle',
            view: VIEW.WHITE,
            children: 'some children',
        },
    },
    {
        testDescription: 'рендерит серый блок',
        props: {
            title: 'my title',
            subtitle: 'my subtitle',
            view: VIEW.GRAY,
            children: 'some children',
        },
    },
    {
        testDescription: 'рендерит зелёный блок',
        props: {
            title: 'my title',
            subtitle: 'my subtitle',
            view: VIEW.GREEN,
            children: 'some children',
        },
    },
    {
        testDescription: 'рендерит красный блок',
        props: {
            title: 'my title',
            subtitle: 'my subtitle',
            view: VIEW.RED,
            children: 'some children',
        },
    },
    {
        testDescription: 'рендерит прозрачный блок',
        props: {
            title: 'my title',
            subtitle: 'my subtitle',
            view: VIEW.TRANSPARENT,
            children: 'some children',
        },
    },
    {
        testDescription: 'рендерит белый блок с корректными отступами когда нет описания',
        props: {
            title: 'my title',
            view: VIEW.WHITE,
            children: 'some children',
        },
    },
    {
        testDescription: 'рендерит белый блок с корректными отступами когда нет заголовка и описания',
        props: {
            view: VIEW.WHITE,
            children: 'some children',
        },
    },
];

describe('EGRNPaidReportBlock', () => {
    TEST_CASES.forEach(({ testDescription, props }) =>
        it(testDescription, async () => {
            await render(<EGRNPaidReportBlock {...props} />, { viewport: { width: 350, height: 200 } });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        })
    );
});
