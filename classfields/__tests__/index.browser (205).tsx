import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import favoriteFilledIcon from '@realty-front/icons/common/favorite-filled-24.svg';

import IconSvg from 'vertis-react/components/IconSvg';

import { EGRNPaidReportCell, SIZE } from '../';

const OPTIONS = { viewport: { width: 200, height: 100 } };

const TEST_CASES = [
    {
        testDescription: 'рендерит ячейку',
        props: {
            value: '123',
            description: 'price',
        },
    },
    {
        testDescription: 'рендерит ячейку c подсказкой',
        props: {
            value: '123',
            description: 'price',
            hint: 'my hint',
        },
    },
    {
        testDescription: 'рендерит ячейку с картинкой',
        props: {
            value: '123',
            description: 'price',
            imageElement: <IconSvg id={favoriteFilledIcon} size={IconSvg.SIZES.SIZE_24} />,
        },
    },
    {
        testDescription: 'рендерит ячейку c подсказкой и картинкой',
        props: {
            value: '123',
            description: 'price',
            hint: 'my hint',
            imageElement: <IconSvg id={favoriteFilledIcon} size={IconSvg.SIZES.SIZE_24} />,
        },
    },
];

describe('EGRNPaidReportCell', () => {
    TEST_CASES.forEach(({ testDescription, props }) => {
        it(`${testDescription} маленького размера`, async () => {
            await render(<EGRNPaidReportCell {...props} size={SIZE.S} />, OPTIONS);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${testDescription} большого размера`, async () => {
            await render(<EGRNPaidReportCell {...props} size={SIZE.L} />, OPTIONS);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${testDescription} маленького размера с лейблом сверху`, async () => {
            await render(<EGRNPaidReportCell {...props} size={SIZE.S} textReverse />, OPTIONS);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it(`${testDescription} большого размера с лейблом сверху`, async () => {
            await render(<EGRNPaidReportCell {...props} size={SIZE.L} textReverse />, OPTIONS);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
