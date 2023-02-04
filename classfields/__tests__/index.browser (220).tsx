import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { FavoriteSerpShare } from '../';
import styles from '../styles.module.css';

import { GateError, GateSuccess, GatePending, store } from './mocks';

interface IViewport {
    width: number;
    height: number;
}

const render = (children: React.ReactElement, { gate, viewport }: { gate?: AnyObject; viewport: IViewport }) =>
    _render(
        <AppProvider initialState={store} Gate={gate}>
            {children}
        </AppProvider>,
        { viewport }
    );

describe('FavoriteSerpShare', () => {
    it('рендерится корректно', async () => {
        await render(<FavoriteSerpShare />, { viewport: { width: 320, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ошибка формирования ссылки', async () => {
        await render(<FavoriteSerpShare />, {
            viewport: { width: 320, height: 300 },
            gate: GateError,
        });

        await page.click(`.${styles.btn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ссылка формируется', async () => {
        await render(<FavoriteSerpShare />, {
            viewport: { width: 320, height: 300 },
            gate: GatePending,
        });

        await page.click(`.${styles.btn}`);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ссылка сформирована', async () => {
        await render(<FavoriteSerpShare />, {
            viewport: { width: 320, height: 700 },
            gate: GateSuccess,
        });

        await page.click(`.${styles.btn}`);

        await page.waitFor(50);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
