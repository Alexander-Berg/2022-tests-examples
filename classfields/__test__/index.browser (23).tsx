import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import * as fsGalleryActions from 'realty-core/view/react/modules/fs-gallery/redux/actions';

import { NewbuildingProgressContainer } from '../container';
import styles from '../styles.module.css';

import { cardWithOneQuarter, cardWithFourQuarters, baseInitialState } from './mocks';

describe('NewbuildingProgress', () => {
    it('корректно рендерит компонент', async () => {
        await render(
            <AppProvider
                initialState={{
                    ...baseInitialState,
                    newbuildingCardPage: {
                        card: cardWithFourQuarters,
                    },
                }}
            >
                <NewbuildingProgressContainer fsGalleryActions={fsGalleryActions} />
            </AppProvider>,
            { viewport: { width: 375, height: 600 } }
        );

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('корректно рендерит компонент без пагинации при малом количестве данных о ходе строительства', async () => {
        await render(
            <AppProvider
                initialState={{
                    ...baseInitialState,
                    newbuildingCardPage: {
                        card: cardWithOneQuarter,
                    },
                }}
            >
                <NewbuildingProgressContainer fsGalleryActions={fsGalleryActions} />
            </AppProvider>,
            { viewport: { width: 375, height: 600 } }
        );

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });

    it('отрабатывает клик по кнопке "Показать ещё"', async () => {
        await render(
            <AppProvider
                initialState={{
                    ...baseInitialState,
                    newbuildingCardPage: {
                        card: cardWithFourQuarters,
                    },
                }}
            >
                <NewbuildingProgressContainer fsGalleryActions={fsGalleryActions} />
            </AppProvider>,
            { viewport: { width: 375, height: 600 } }
        );

        await page.click(`.${styles.button}`);

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });
});
