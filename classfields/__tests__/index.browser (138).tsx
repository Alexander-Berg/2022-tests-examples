import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { CONCIERGE_FORM_PLACEMENT } from 'realty-core/view/react/modules/concierge/common/ConciergeForm/types';

import { ConciergeModal } from '..';

import formStyles from '../ConciergeModalForm/styles.module.css';

import { IGate, GatePending, GateSuccess, GateError, userMock, conciergeExtraValues } from './mocks';

const renderComponent = ({
    Gate,
    withUserData,
    viewport = { width: 375, height: 812 },
    conciergeFormPlacement = CONCIERGE_FORM_PLACEMENT.concierge,
}: {
    Gate?: IGate;
    withUserData?: boolean;
    viewport?: { width: number; height: number };
    conciergeFormPlacement?: CONCIERGE_FORM_PLACEMENT;
}) =>
    render(
        <AppProvider
            Gate={Gate}
            fakeTimers={{
                now: new Date('2021-09-28T10:00:00.111Z'),
            }}
            initialState={{ user: withUserData ? userMock : undefined }}
        >
            <ConciergeModal
                isModalOpen={true}
                handleModalHide={noop}
                initialFormExtraValues={conciergeExtraValues}
                conciergeFormPlacement={conciergeFormPlacement}
            />
        </AppProvider>,
        { viewport }
    );

describe('ConciergeModal', () => {
    it('рендерится в дефолтном состоянии (360px)', async () => {
        await renderComponent({ viewport: { width: 360, height: 640 } });

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

    it('рендерится без селектов даты и времени (375px)', async () => {
        await renderComponent({
            viewport: { width: 375, height: 812 },
            conciergeFormPlacement: CONCIERGE_FORM_PLACEMENT.site_without_offers,
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('форма', () => {
        it('достаёт данные о пользователе', async () => {
            await renderComponent({ withUserData: true });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('корректно генерирует даты', async () => {
            await renderComponent({});

            await page.click(`.${formStyles.select}:first-child button`);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click('.Menu__item:last-child');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('корректно генерирует время', async () => {
            await renderComponent({});

            await page.click(`.${formStyles.select}:last-child button`);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click('.Menu__item:last-child');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('предупреждает о незаполненном номере', async () => {
            await renderComponent({ Gate: GateSuccess });

            await page.click('button[type="submit"]');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('предупреждает о невалидном номере', async () => {
            await renderComponent({ Gate: GateSuccess });

            await page.type('input[name="phone_input"]', '12345');

            await page.click('button[type="submit"]');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('телефон заполнен, статус pending', async () => {
            await renderComponent({ Gate: GatePending });

            await page.type('input[name="phone_input"]', '1234567890');

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

        it('сообщение успешно отправлено (без описания)', async () => {
            await renderComponent({
                Gate: GateSuccess,
                conciergeFormPlacement: CONCIERGE_FORM_PLACEMENT.site_without_offers,
            });

            await page.type('input[name="phone_input"]', '1234567890');

            await page.click('button[type="submit"]');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
