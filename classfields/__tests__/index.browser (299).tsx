import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SearchSubscription } from '../';
import mainStyles from '../styles.module.css';
import styles from '../SearchSubscriptionForm/styles.module.css';

import { defaultStore, storeWithDefaultEmail } from './mocks';

describe('SearchSubscription', () => {
    it(`Дефолтная форма`, async () => {
        await render(
            <AppProvider initialState={storeWithDefaultEmail}>
                <SearchSubscription />
            </AppProvider>,
            {
                viewport: { width: 420, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${mainStyles.button}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`Заполнение формы и ошибка`, async () => {
        await render(
            <AppProvider initialState={defaultStore}>
                <SearchSubscription />
            </AppProvider>,
            {
                viewport: { width: 340, height: 500 },
            }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${mainStyles.button}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type(`.${styles.textInput}:nth-of-type(1) input`, 'Мой поиск');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type(`.${styles.textInput}:nth-of-type(2) input`, 'mail@yandex.ru');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.button}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
