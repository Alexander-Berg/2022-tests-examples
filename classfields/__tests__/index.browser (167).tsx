/* eslint-disable max-len */
import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { FeedBanReason } from '../container';
import styles from '../styles.module.css';

import { getStore, feed } from './stubs/store';

const viewport = { width: 1400, height: 900 } as const;

const selectors = {
    learnMore: (n: number) => `.${styles.reason}:nth-of-type(${n}) .Button`,
};

const Component: React.FunctionComponent = () => (
    <AppProvider initialState={getStore()} context={{}}>
        <FeedBanReason feed={feed} />
    </AppProvider>
);

describe('FeedBanReason', () => {
    it('открывается модалка при клике на "Подробнее"', async () => {
        const component = <Component />;

        await render(component, { viewport });
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(selectors.learnMore(1));
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
