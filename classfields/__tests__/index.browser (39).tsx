import React from 'react';
import { render } from 'jest-puppeteer-react';
import { DeepPartial } from 'utility-types';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { IUniversalStore } from 'view/modules/types';
import { userReducer } from 'view/entries/user/reducer';

import { HouseServicesPeriodPaymentConfirmationNotSentModal } from '../index';

const mobileViewports = [
    { width: 320, height: 812 },
    { width: 375, height: 812 },
    { width: 460, height: 812 },
    { width: 768, height: 1024 },
    { width: 1024, height: 1366 },
];

const mobileStore: DeepPartial<IUniversalStore> = {
    config: {
        isMobile: 'iPhone',
    },
};

const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = (props) => (
    <AppProvider rootReducer={userReducer} Gate={props.Gate} initialState={props.store}>
        <HouseServicesPeriodPaymentConfirmationNotSentModal isOpen={true} closeModal={() => undefined} />
    </AppProvider>
);

describe('HouseServicesPeriodPaymentConfirmationNotSentModal', () => {
    it('Базовое состояние', async () => {
        for (const viewport of mobileViewports) {
            await render(<Component store={mobileStore} />, { viewport });
            await page.addStyleTag({ content: 'body{padding: 0}' });

            expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
        }
    });
});
