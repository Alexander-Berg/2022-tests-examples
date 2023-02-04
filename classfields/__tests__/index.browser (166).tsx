/* eslint-disable jest/expect-expect */
import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/lib/test-helpers';
import { rootReducer } from 'view/common/reducers';

import feedBanReasonsStyle from '../components/feed-preview/FeedBanReason/styles.module.css';

import Feeds from '../index';

import { getStore } from './stubs/store';
import { onModerationFeed } from './stubs/feeds';
import { history } from './stubs/history';

import { stats } from './stubs/stats';

const desktopViewports = [
    { width: 1400, height: 2000 },
    { width: 1000, height: 2000 },
] as const;

const selectors = {
    deleteFeedButton: (feedIndex: number) =>
        `.feed-preview:nth-of-type(${feedIndex}) .feed-preview__actions .feed-preview__link-button:last-of-type`,
    feedHistoryButton: (feedIndex: number) =>
        `.feed-preview:nth-of-type(${feedIndex}) .feed-preview__actions .feed-preview__link-button:first-of-type`,
    feedErrorDetailsButton: (feedIndex: number) =>
        `.feed-preview:nth-of-type(${feedIndex}) .feed-preview__error-list-link`,
    sendToRecheckButton: (feedIndex: number) =>
        `.feed-preview:nth-of-type(${feedIndex}) .${feedBanReasonsStyle.bottomWrapper} .Button`,
    feedStatsButton: (feedIndex: number) =>
        `.feed-preview:nth-of-type(${feedIndex}) .feed-history .Radio_type_button:last-of-type`,
    feedBanReasonsButton: (feedIndex: number, banIndex: number) =>
        `.feed-preview:nth-of-type(${feedIndex}) .${feedBanReasonsStyle.reason}:nth-of-type(${banIndex}) .Button`,
};

interface ITestComponentProps {
    state: AnyObject;
    Gate?: AnyObject;
}

const render = async (component: React.ClassicElement<ITestComponentProps>) => {
    for (const viewport of desktopViewports) {
        await _render(component, { viewport });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

const Component: React.FunctionComponent<ITestComponentProps> = ({ state, Gate }) => {
    return (
        <AppProvider rootReducer={rootReducer} initialState={state} Gate={Gate}>
            <Feeds />
        </AppProvider>
    );
};

describe('Feeds', () => {
    it('Список фидов', async () => {
        await render(<Component state={getStore()} />);
    });

    it('Удаление фида', async () => {
        const Gate = {
            create: () => Promise.resolve(),
        };

        await _render(<Component state={getStore()} Gate={Gate} />, { viewport: desktopViewports[0] });

        await page.click(selectors.deleteFeedButton(1));
        await page.waitForSelector('.confirm-modal__controls');

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();

        await page.click('.confirm-modal__control_type_continue');

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('Удаление фида c причинами отклонения', async () => {
        const Gate = {
            create: () => Promise.resolve(),
        };

        await _render(<Component state={getStore()} Gate={Gate} />, { viewport: desktopViewports[0] });

        await page.click(selectors.deleteFeedButton(3));
        await page.waitFor('.confirm-modal__controls');

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();

        await page.click('.confirm-modal__control_type_continue');

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('История загрузок', async () => {
        const Gate = {
            create: (path: string) => {
                switch (path) {
                    case 'feeds.get-feed-downloads': {
                        return Promise.resolve(history);
                    }
                    case 'feeds.get-feed-statistics': {
                        return Promise.resolve(stats);
                    }
                }
            },
        };

        await _render(<Component state={getStore()} Gate={Gate} />, { viewport: desktopViewports[0] });

        await page.click(selectors.feedHistoryButton(1));

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();

        await page.click(selectors.feedStatsButton(1));

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('Отправка на перепроверку', async () => {
        const Gate = {
            create: () => Promise.resolve(),
            get: () => Promise.resolve(onModerationFeed),
        };

        await _render(<Component state={getStore()} Gate={Gate} />, { viewport: desktopViewports[0] });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();

        await page.click(selectors.sendToRecheckButton(2));

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('Попап подробнее с текстом в фиде c причинами отклонения', async () => {
        await _render(<Component state={getStore()} />, { viewport: desktopViewports[0] });

        await page.click(selectors.feedBanReasonsButton(3, 2));

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('Попап подробнее с html в фиде c причинами отклонения', async () => {
        await _render(<Component state={getStore()} />, { viewport: desktopViewports[0] });

        await page.click(selectors.feedBanReasonsButton(3, 5));

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('Ошибка загрузки', async () => {
        await _render(<Component state={getStore({ status: 'errored' })} />, {
            viewport: { width: 1000, height: 400 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
