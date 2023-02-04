import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { InventoryItem, IInventoryItemProps } from '../';

import { item, itemWithCount, itemWithDefect, itemWithLongName } from './stub/store';

import 'view/styles/common.css';

const renderOptions = [
    { viewport: { width: 960, height: 300 } },
    { viewport: { width: 625, height: 300 } },
    { viewport: { width: 360, height: 300 } },
];

const Component: React.FunctionComponent<IInventoryItemProps> = (props) => (
    <div style={{ padding: '20px' }}>
        <InventoryItem {...props} />
    </div>
);

describe('InventoryItem', () => {
    describe(`Базовый вид`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component item={item} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Несколько предметов`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component item={itemWithCount} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`С дефектом`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component item={itemWithDefect} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Длинное название`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component item={itemWithLongName} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
