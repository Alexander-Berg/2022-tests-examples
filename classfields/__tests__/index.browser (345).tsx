import React from 'react';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { BackCall, IBackCallProps } from '..';

import {
    IGate,
    userPayload,
    unauthorizedUserPayload,
    pendingGate,
    errorGate,
    errorAlreadyCreatedGate,
    rootReducer,
} from './mocks';

interface IRenderProps {
    Gate?: IGate;
    authorized?: boolean;
    initialState?: Record<string, unknown>;
    width?: number;
    height?: number;
    props?: Partial<IBackCallProps>;
}

const render = ({ Gate, authorized = true, initialState, width, height, props }: IRenderProps) =>
    _render(
        <AppProvider
            Gate={Gate}
            rootReducer={rootReducer}
            initialState={{ ...initialState, user: authorized ? userPayload : unauthorizedUserPayload }}
        >
            <BackCall siteId="123" {...props} />
        </AppProvider>,
        { viewport: { width: width || 380, height: height || 120 } }
    );

describe('BackCall', () => {
    it('дефолтный рендер пользователь авторизован', async () => {
        await render({});

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('дефолтный рендер пользователь не авторизован', async () => {
        await render({ authorized: false });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('форма отправляется', async () => {
        await render({ Gate: pendingGate });

        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ошибка сети', async () => {
        await render({ Gate: errorGate });

        await page.setOfflineMode(true);
        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
        await page.setOfflineMode(false);
    });

    it('ошибка сервера', async () => {
        await render({ Gate: errorGate });

        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ошибка повторного создания заявки для одного и того же номера телефона', async () => {
        await render({ Gate: errorAlreadyCreatedGate });

        await page.click('button');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('layout=column', async () => {
        await render({ height: 200, props: { layout: 'column' } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('layout=row', async () => {
        await render({ width: 600, props: { layout: 'row' } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
