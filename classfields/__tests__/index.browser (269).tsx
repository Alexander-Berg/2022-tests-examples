/* eslint-disable jest/expect-expect */
import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';

import { IOfferSnippetGalleryProps } from '../types';
import { OfferSnippetGalleryContainer } from '../container';

import { baseOffer, offerWithoutImages } from './mocks';
import styles from './styles.module.css';

const rootReducer = createRootReducer({});

const Component = (props: IOfferSnippetGalleryProps) => (
    <AppProvider initialState={rootReducer}>
        <OfferSnippetGalleryContainer className={styles.gallery} {...props} />
    </AppProvider>
);

describe('OfferSnippetGallery', () => {
    it('Рендерит галерею оффера', async () => {
        await render(<Component item={baseOffer} />, { viewport: { width: 320, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерит галерею оффера без картинок', async () => {
        await render(<Component item={offerWithoutImages} />, { viewport: { width: 320, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерит галерею оффера c маленькими сниппетами', async () => {
        await render(<Component item={baseOffer} view="small" />, { viewport: { width: 320, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерит галерею оффера без картинок c маленькими сниппетами', async () => {
        await render(<Component item={offerWithoutImages} view="small" />, { viewport: { width: 320, height: 200 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
