import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import { NavigationErrorModal } from '../index';

const mobileViewports = [
    { width: 345, height: 812 },
    { width: 360, height: 640 },
];

const desktopViewport = { width: 1024, height: 768 };

const store: DeepPartial<IUniversalStore> = {
    config: {
        isMobile: undefined,
    },
};
const mobileStore: DeepPartial<IUniversalStore> = {
    config: {
        isMobile: 'iPhone',
    },
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider rootReducer={userReducer} Gate={props.Gate} initialState={props.store}>
        <NavigationErrorModal isOpen={true} onRepeat={() => undefined} closeModal={() => undefined} />
    </AppProvider>
);

describe('NavigationErrorModal', () => {
    it('Базовое состояние', async () => {
        await render(<Component store={store} />, {
            viewport: desktopViewport,
        });

        expect(await takeScreenshot()).toMatchImageSnapshot({
            failureThreshold: 3,
        });
    });

    it('Мобильная версия', async () => {
        for (const viewport of mobileViewports) {
            await render(<Component store={mobileStore} />, { viewport });
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(
                await takeScreenshot({
                    fullPage: true,
                })
            ).toMatchImageSnapshot({
                failureThreshold: 3,
            });
        }
    });
});
