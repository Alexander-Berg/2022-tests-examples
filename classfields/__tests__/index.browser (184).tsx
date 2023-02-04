import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import modalStyles from 'realty-core/view/react/modules/concierge/touch-phone/ConciergeModal/styles.module.css';

import profitsStyles from '../ConciergeProfits/styles.module.css';
import workingFeaturesStyles from '../ConciergeWorkingFeatures/styles.module.css';

import { Concierge } from '..';

const userMock = {
    crc: 'u6aufj490g4c0c3fb9e34612340215c1',
    uid: '1234123412',
    yuid: '2089203918273403541',
    isVosUser: false,
    isAuth: true,
    isJuridical: false,
    paymentTypeSuffix: 'natural',
    promoSubscription: {},
    avatarId: '0/0-0',
    avatarHost: 'avatars.mdst.yandex.net',
    defaultEmail: 'user.test@yandex.ru',
    emailHash: 'dkl1231kdsdc2b6c3112c80ed404c',
    defaultPhone: '+79999999999',
    passHost: 'https://pass-test.yandex.ru',
    passportHos: 'https://passport-test.yandex.ru',
    passportApiHost: 'https://api.passport-test.yandex.ru',
    passportOrigin: 'realty_saint-petersburg',
    passportDefaultEmail: 'user.test@yandex.ru',
    favorites: [],
    favoritesMap: {},
    comparison: [],
    statistics: {},
    displayName: 'Тестовый юзер',
};

describe('Concierge', function () {
    it('Рендерится корректно', async () => {
        await render(
            <AppProvider>
                <div style={{ margin: '-20px' }}>
                    <Concierge />
                </div>
            </AppProvider>,
            { viewport: { width: 415, height: 800 } }
        );

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('открывает форму с кнопки в основном блоке', async () => {
        await render(
            <AppProvider
                initialState={{ user: userMock }}
                fakeTimers={{
                    now: new Date('2021-09-28T10:00:00.111Z'),
                }}
            >
                <div style={{ margin: '-20px' }}>
                    <Concierge />
                </div>
            </AppProvider>,
            { viewport: { width: 375, height: 812 } }
        );

        await page.click(`.${profitsStyles.button}`);
        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('открывает форму с кнопки в блоке "Как мы работаем"', async () => {
        await render(
            <AppProvider
                initialState={{ user: userMock }}
                fakeTimers={{
                    now: new Date('2021-09-28T10:00:00.111Z'),
                }}
            >
                <div style={{ margin: '-20px' }}>
                    <Concierge />
                </div>
            </AppProvider>,
            { viewport: { width: 375, height: 812 } }
        );

        await page.click(`.${workingFeaturesStyles.button}`);
        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('закрывает форму с кнопки', async () => {
        await render(
            <AppProvider
                initialState={{ user: userMock }}
                fakeTimers={{
                    now: new Date('2021-09-28T10:00:00.111Z'),
                }}
            >
                <div style={{ margin: '-20px' }}>
                    <Concierge />
                </div>
            </AppProvider>,
            { viewport: { width: 375, height: 812 } }
        );

        await page.click(`.${profitsStyles.button}`);
        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();

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
                    <div style={{ margin: '-20px' }}>
                        <Concierge />
                    </div>
                </AppProvider>,
                { viewport: { width: 375, height: 812 } }
            );

            await page.click(`.${workingFeaturesStyles.button}`);
            await page.click('[data-test=ConciergeModalFormDateSelect]');

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click('[data-test=ConciergeModalFormDateSelect]');
            await page.click('[data-test=ConciergeModalFormTimeSelect]');

            expect(await takeScreenshot()).toMatchImageSnapshot();
        }
    );

    it('открывает форму и выбирает любой день, кроме первого', async () => {
        await render(
            <AppProvider initialState={{ user: userMock }} fakeTimers={{ now: new Date('2021-01-01T15:00:00.111Z') }}>
                <div style={{ margin: '-20px' }}>
                    <Concierge />
                </div>
            </AppProvider>,
            { viewport: { width: 375, height: 812 } }
        );

        await page.click(`.${workingFeaturesStyles.button}`);
        await page.click('[data-test=ConciergeModalFormDateSelect]');
        await page.click('.Menu__item:nth-child(3)');
        await page.click('[data-test=ConciergeModalFormTimeSelect]');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
