import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { HeaderShadowUserComponent } from '../';

const renderOptions = { viewport: { width: 250, height: 100 } };

describe('HeaderShadowUser', () => {
    it('Рендерится', async () => {
        const WrappedComponent = (
            <HeaderShadowUserComponent
                partnerUrl="/"
                link={() => '/'}
                hasShadowUser
                shadowName="vasek"
                shadowUid="123"
                sharedCookiesDomain=".ya.ru"
            />
        );

        await render(WrappedComponent, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится со ссылкой на ЛК', async () => {
        const WrappedComponent = (
            <HeaderShadowUserComponent
                partnerUrl="/"
                link={() => '/'}
                hasShadowUser
                shadowName="vasek"
                shadowUid="123"
                sharedCookiesDomain=".ya.ru"
                isNewLkAvailable
            />
        );

        await render(WrappedComponent, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('Рендерится без имени', async () => {
        const WrappedComponent = (
            <HeaderShadowUserComponent
                partnerUrl="/"
                link={() => '/'}
                hasShadowUser
                shadowUid="123"
                sharedCookiesDomain=".ya.ru"
            />
        );

        await render(WrappedComponent, renderOptions);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
