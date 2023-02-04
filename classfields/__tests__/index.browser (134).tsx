import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { BackcallAgreementModal } from '..';

import { IGate, GateSuccess, GateError, getInitialState } from './mocks';

interface IComponentRenderParams {
    Gate?: IGate;
    withUserData?: boolean;
    viewport?: { width: number; height: number };
}

const renderComponent = async ({
    Gate,
    withUserData,
    viewport = { width: 375, height: 812 },
}: IComponentRenderParams) => {
    await render(
        <AppProvider
            Gate={Gate}
            initialState={getInitialState(withUserData)}
            fakeTimers={{
                now: new Date().getTime(),
            }}
        >
            <BackcallAgreementModal />
        </AppProvider>,
        { viewport }
    );

    await customPage.tick(40000);
};

describe('BackcallAgreementModal', () => {
    it('рендерится в дефолтном состоянии (320px)', async () => {
        await renderComponent({ viewport: { width: 320, height: 640 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии (375px)', async () => {
        await renderComponent({ viewport: { width: 375, height: 812 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии (411px)', async () => {
        await renderComponent({ viewport: { width: 411, height: 823 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в дефолтном состоянии (640px)', async () => {
        await renderComponent({ viewport: { width: 640, height: 800 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('достаёт данные о пользователе', async () => {
        await renderComponent({ withUserData: true });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('предупреждает о невалидном номере', async () => {
        await renderComponent({ Gate: GateSuccess });

        await page.type('input[name="phone_input"]', '12345');

        await page.click('button[type="submit"]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('ошибка сервера', async () => {
        await renderComponent({ Gate: GateError });

        await page.type('input[name="phone_input"]', '1234567890');

        await page.click('button[type="submit"]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('сообщение успешно отправлено', async () => {
        await renderComponent({ Gate: GateSuccess });

        await page.type('input[name="phone_input"]', '1234567890');

        await page.click('button[type="submit"]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
