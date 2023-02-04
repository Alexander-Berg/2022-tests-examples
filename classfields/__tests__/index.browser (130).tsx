import React, { useState } from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import TagWithValue from '../index';
import styles from '../styles.module.css';

describe('TagWithValue', () => {
    it('Отрисовка - только текст', async () => {
        await render(
            <AppProvider>
                <TagWithValue label={'В Москве'} />
            </AppProvider>,
            { viewport: { width: 480, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - текст + значение', async () => {
        await render(
            <AppProvider>
                <TagWithValue label={'В Москве'} value={19} />
            </AppProvider>,
            { viewport: { width: 480, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - кнопка "Ещё"', async () => {
        await render(
            <AppProvider>
                <TagWithValue label="Свернуть" collapsedLabel="Ещё" type="collapse" collapsed />
            </AppProvider>,
            { viewport: { width: 480, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Отрисовка - кнопка "Свернуть"', async () => {
        await render(
            <AppProvider>
                <TagWithValue label="Свернуть" collapsedLabel="Ещё" type="collapse" />
            </AppProvider>,
            { viewport: { width: 480, height: 300 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Действия - клик по кнопке "Ещё"', async () => {
        const Container = () => {
            const [isCollapsed, setCollapsedState] = useState(true);

            const handleClick = () => {
                setCollapsedState(!isCollapsed);
            };

            return (
                <AppProvider>
                    <TagWithValue
                        label="Развёрнуто"
                        type="collapse"
                        collapsedLabel="Свёрнуто"
                        onClick={handleClick}
                        collapsed={isCollapsed}
                    />
                </AppProvider>
            );
        };

        await render(<Container />, { viewport: { width: 480, height: 300 } });

        await page.click(`.${styles.arrow}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();

        await page.click(`.${styles.arrow}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
