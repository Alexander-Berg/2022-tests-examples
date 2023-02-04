import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { BackcallAgreementModal } from '..';

import { IGate, GateSuccess, GateError, getInitialState } from './mocks';

const renderComponent = async ({ Gate, withUserData }: { Gate?: IGate; withUserData?: boolean }) => {
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
        { viewport: { width: 1440, height: 800 } }
    );

    await customPage.tick(40000);
};

describe('BackcallAgreementModal', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await renderComponent({});

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('достаёт телефон пользователя из стора', async () => {
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
