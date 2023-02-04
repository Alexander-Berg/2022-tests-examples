import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'view/libs/test-helpers';

import { HouseServiceCreateCounterModal, IHouseServiceCreateCounterModalProps } from '../index';
import styles from '../styles.module.css';

const renderOptions = [{ viewport: { width: 1000, height: 600 } }, { viewport: { width: 580, height: 600 } }];

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const Component: React.FunctionComponent<IHouseServiceCreateCounterModalProps> = ({ children, ...props }) => {
    return (
        <AppProvider>
            <HouseServiceCreateCounterModal {...props} />
        </AppProvider>
    );
};

describe('HouseServiceCreateCounterModal', () => {
    describe(`Счетчики`, () => {
        renderOptions.forEach((renderOption) => {
            it(`${renderOption.viewport.width}px`, async () => {
                await render(<Component isOpen={true} closeModal={noop} onItemClick={noop} />, renderOption);

                expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
            });
        });
    });

    it('Ховер в десктопе', async () => {
        await render(<Component isOpen={true} closeModal={noop} onItemClick={noop} />, renderOptions[0]);

        await page.hover(`.${styles.items} div:first-of-type`);

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });
});
