import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { SearchSubscriptionFormControl } from '../';
import styles from '../SearchSubscriptionFormControlForm/styles.module.css';

import { defaultStore, storeWithDefaultEmail } from './mocks';

const options = {
    viewport: { width: 700, height: 500 },
};

describe('SearchSubscriptionFormControl', () => {
    it(`Дефолтная форма`, async () => {
        await render(
            <AppProvider initialState={storeWithDefaultEmail}>
                <SearchSubscriptionFormControl size="l" view="transparent-blue" pageParams={{}} />
            </AppProvider>,
            options
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('.Button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it(`Заполнение формы и ошибка`, async () => {
        await render(
            <AppProvider initialState={defaultStore}>
                <SearchSubscriptionFormControl size="m" view="soft-blue" pageParams={{}} />
            </AppProvider>,
            options
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click('.Button');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type(`.${styles.textInput}:nth-of-type(1) input`, 'Мой поиск');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.type(`.${styles.textInput}:nth-of-type(2) input`, 'mail@yandex.ru');
        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.button}`);
        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
