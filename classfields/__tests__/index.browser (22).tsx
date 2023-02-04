import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { InventoryItemBase, IInventoryItemBaseProps } from '../';

import { item, itemWithLongName } from './stub/store';

import 'view/styles/common.css';

const renderOptions = [
    { viewport: { width: 960, height: 300 } },
    { viewport: { width: 625, height: 300 } },
    { viewport: { width: 360, height: 300 } },
];

const Component: React.FunctionComponent<IInventoryItemBaseProps> = (props) => (
    <div style={{ padding: '20px' }}>
        <InventoryItemBase {...props} />
    </div>
);

describe('InventoryItemBase', () => {
    describe(`Базовый вид`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component {...item} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    describe(`Длинное название`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component {...itemWithLongName} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });
});
