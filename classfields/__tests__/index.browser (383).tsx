import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { AnyObject } from 'realty-core/types/utils';

import { OfferCardGallery } from '../index';

import {
    baseOffer,
    editableOfferCardWithoutImages,
    offerCardWithAllFeatures,
    readonlyOfferCardWithoutImages,
    state,
} from './mocks';
import styles from './styles.module.css';

const renderComponent = (props: AnyObject) =>
    render(
        <AppProvider initialState={state}>
            {/* eslint-disable-next-line @typescript-eslint/ban-ts-comment */}
            {/* @ts-ignore */}
            <OfferCardGallery {...props} className={styles.gallery} />
        </AppProvider>,
        {
            viewport: { width: 700, height: 500 },
        }
    );

describe('OfferCardGallery', () => {
    it('Базовая отрисовка', async () => {
        await renderComponent({ offer: baseOffer, isOwner: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Нет изображений, пользователь - владелец, и карточка редактируемая', async () => {
        await renderComponent({ offer: editableOfferCardWithoutImages, isOwner: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Нет изображений, пользователь - владелец, и карточка не редактируемая', async () => {
        await renderComponent({ offer: readonlyOfferCardWithoutImages, isOwner: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Нет изображений, пользователь - не владелец', async () => {
        await renderComponent({ offer: editableOfferCardWithoutImages, isOwner: false });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Новостроечный оффер с планировкой и туром', async () => {
        await renderComponent({ offer: offerCardWithAllFeatures, isOwner: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
