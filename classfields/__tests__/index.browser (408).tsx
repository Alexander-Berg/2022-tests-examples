import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import modalStyles from 'realty-core/view/react/modules/concierge/deskpad/ConciergeModal/styles.module.css';

import { Concierge } from '..';
import profitsStyles from '../ConciergeProfits/styles.module.css';
import workingFeaturesStyles from '../ConciergeWorkingFeatures/styles.module.css';

import { userMock } from './mocks';

describe('Concierge', function () {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider initialState={{ user: { isAuth: false } }}>
                <Concierge />
            </AppProvider>,
            { viewport: { width: 1440, height: 800 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('Отрабатывает ховер на иконку пользователя', async () => {
        await render(
            <AppProvider initialState={{ user: userMock }}>
                <Concierge />
            </AppProvider>,
            { viewport: { width: 1440, height: 800 } }
        );

        await page.hover('.UserPic');

        expect(await takeScreenshot({ fullPage: true, keepCursor: true })).toMatchImageSnapshot();
    });

    it('открывает форму с кнопки в основном блоке', async () => {
        await render(
            <AppProvider
                initialState={{ user: userMock }}
                fakeTimers={{
                    now: new Date('2021-09-28T10:00:00.111Z'),
                }}
            >
                <Concierge />
            </AppProvider>,
            { viewport: { width: 1440, height: 800 } }
        );

        await page.click(`.${profitsStyles.button}`);
        await page.waitFor(100);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('открывает форму с кнопки в блоке "Как мы работаем"', async () => {
        await render(
            <AppProvider
                initialState={{ user: userMock }}
                fakeTimers={{
                    now: new Date('2021-09-28T10:00:00.111Z'),
                }}
            >
                <Concierge />
            </AppProvider>,
            { viewport: { width: 1440, height: 800 } }
        );

        await page.click(`.${workingFeaturesStyles.button}`);
        await page.waitFor(100);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('закрывает форму с кнопки', async () => {
        await render(
            <AppProvider
                initialState={{ user: userMock }}
                fakeTimers={{
                    now: new Date('2021-09-28T10:00:00.111Z'),
                }}
            >
                <Concierge />
            </AppProvider>,
            { viewport: { width: 1440, height: 800 } }
        );

        await page.click(`.${profitsStyles.button}`);
        await page.waitFor(100);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();

        await page.click(`.${modalStyles.closeButton}`);
        await page.waitFor(100);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it.each([['07:00'], ['11:00'], ['13:00'], ['15:00'], ['17:00'], ['19:00'], ['21:00'], ['23:00']])(
        'открывает форму в %s',
        async (time) => {
            await render(
                <AppProvider
                    initialState={{ user: userMock }}
                    fakeTimers={{ now: new Date(`2021-01-01T${time}:00.111Z`) }}
                >
                    <Concierge />
                </AppProvider>,
                { viewport: { width: 1440, height: 800 } }
            );

            await page.click(`.${workingFeaturesStyles.button}`);
            await page.click('[data-test=ConciergeModalFormDateSelect]');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click('[data-test=ConciergeModalFormTimeSelect]');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    );

    it('открывает форму и выбирает любой день, кроме первого', async () => {
        await render(
            <AppProvider initialState={{ user: userMock }} fakeTimers={{ now: new Date('2021-01-01T15:00:00.111Z') }}>
                <Concierge />
            </AppProvider>,
            { viewport: { width: 1440, height: 800 } }
        );

        await page.click(`.${workingFeaturesStyles.button}`);
        await page.click('[data-test=ConciergeModalFormDateSelect]');
        await page.click('.Menu__item:nth-child(3)');
        await page.click('[data-test=ConciergeModalFormTimeSelect]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
