/* eslint-disable jest/expect-expect */
import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider, createRootReducer } from 'realty-core/view/react/libs/test-helpers';

import { AnyObject } from 'realty-core/types/utils';

import OffersMapSerpModal from '../container';

import { initialState, props as baseProps } from './mocks';

const mobileViewports = [
    { width: 320, height: 700 },
    { width: 375, height: 700 },
] as const;

const renderMultiple = async (component: React.ReactElement) => {
    for (const viewport of mobileViewports) {
        await render(component, { viewport });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    }
};

const Component: React.FunctionComponent<AnyObject> = (props) => {
    return (
        <AppProvider rootReducer={createRootReducer({})} initialState={initialState}>
            {/* eslint-disable-next-line @typescript-eslint/ban-ts-comment */}
            {/* @ts-ignore */}
            <OffersMapSerpModal {...props} />
        </AppProvider>
    );
};

describe('OfferSerpSnippet', () => {
    it('Базовый сценарий', async () => {
        await renderMultiple(<Component {...baseProps} />);
    });

    // TODO нужно придумать как сделать написать тест с проверкой работы скролла через тач события
    it.skip('Скролл работает', async () => {
        await render(<Component {...baseProps} />, { viewport: mobileViewports[1] });

        await page.evaluate(() => {
            const touchstart = new Touch({
                identifier: Date.now(),
                target: window.document.body,
                pageX: 100,
                pageY: 100,
                screenX: 100,
                screenY: 100,
                clientX: 100,
                clientY: 100,
                force: 1,
            });

            const touchmove = new Touch({
                identifier: Date.now(),
                target: window.document.body,
                pageX: 100,
                pageY: 300,
                screenX: 100,
                screenY: 300,
                clientX: 100,
                clientY: 300,
                force: 1,
            });

            const touchMoveEvent = new TouchEvent('touchmove', {
                bubbles: true,
                touches: [touchstart, touchmove],
                changedTouches: [touchstart, touchmove],
            });

            window.document.body.dispatchEvent(touchMoveEvent);
        });

        expect(
            await takeScreenshot({
                fullPage: true,
            })
        ).toMatchImageSnapshot();
    });
});
