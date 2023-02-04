import React from 'react';
import { render } from 'jest-puppeteer-react';
import fill from 'lodash/fill';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { Table, ITableProps } from '../';

const getMockProps: ITableProps<
    { field1: string; field2: number; field3: string; field4: string; field5: string },
    undefined
> = {
    rowProps: undefined,
    columns: [
        {
            title: 'foooooooo',
            subtitle: 'barrrrrrrrrr',
            widthPercent: 20,
            minWidth: 300,
            renderCell: ({ field1 }) => <div>{field1}</div>,
        },
        {
            title: 'LOREM',
            subtitle: 'ipsum',
            widthPercent: 20,
            minWidth: 50,
            maxWidth: 100,
            renderCell: ({ field2 }) => <span>{field2}</span>,
        },
        {
            title: 'DOLOR SIT',
            widthPercent: 20,
            minWidth: 250,
            maxWidth: 250,

            renderCell: ({ field3 }) => <button>{field3}</button>,
        },
        {
            title: 'another title',
            subtitle: 'another subtitle',
            widthPercent: 20,
            minWidth: 100,
            maxWidth: 100,

            renderCell: ({ field4 }) => <a href="/">{field4}</a>,
        },
        {
            title: 'and another one',
            subtitle: 'yeah',
            widthPercent: 20,
            minWidth: 59,
            maxWidth: 100,

            renderCell: ({ field5 }) => <div>{field5}</div>,
        },
    ],
    rowData: fill(Array(10), {
        field1: 'hey are you doing fine',
        field2: 20,
        field3: 'some data and maybe some more',
        field4: 'advice: dont buy an iphone',
        field5: 'markov punk kid',
    }),
    extractRowKey: (_, index) => index,
};

describe('Table', () => {
    it('Рендерится', async () => {
        await render(<Table {...getMockProps} />, { viewport: { width: 1300, height: 800 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится в узком контейнере со скроллом', async () => {
        // скролла нет на скриншотах, но он там есть,
        // просто не рендерится в headless-хроме
        // issue: https://github.com/puppeteer/puppeteer/issues/4747
        await render(
            <div style={{ width: 600, overflowX: 'scroll' }}>
                <Table {...getMockProps} />
            </div>,
            { viewport: { width: 700, height: 1100 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
