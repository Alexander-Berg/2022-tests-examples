import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { CardLocation } from '../index';

import styles from '../styles.module.css';

import { SITE_CARD } from './mocks';

describe('CardLocation', () => {
    it('Базовая отрисовка', async () => {
        await render(
            <AppProvider>
                <CardLocation item={SITE_CARD} />
            </AppProvider>,
            {
                viewport: { width: 320, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Без метро', async () => {
        await render(
            <AppProvider>
                <CardLocation
                    item={{
                        ...SITE_CARD,
                        location: {
                            ...SITE_CARD.location,
                            metroList: [],
                        },
                    }}
                />
            </AppProvider>,
            {
                viewport: { width: 320, height: 150 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('С раскрытым метро', async () => {
        await render(
            <AppProvider>
                <CardLocation item={SITE_CARD} />
            </AppProvider>,
            {
                viewport: { width: 320, height: 250 },
            }
        );

        await page.click(`.${styles.expandMetroListControl}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
