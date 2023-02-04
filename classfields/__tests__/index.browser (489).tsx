import React from 'react';
import { DeepPartial } from 'utility-types';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import reducer, { ICommonPageStore } from 'view/react/deskpad/reducers/roots/common';

import { WalletContainer } from '../container';

import { notAuthorizedStore, emptyStore, notEmptyStore } from './stub/store';

const Component: React.FunctionComponent<{ store?: DeepPartial<ICommonPageStore>; Gate?: Record<string, unknown> }> = (
    props
) => (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    <AppProvider rootReducer={reducer as any} Gate={props.Gate} initialState={props.store} context={{}}>
        <WalletContainer />
    </AppProvider>
);

advanceTo(new Date('2021-02-26'));

describe('Wallet', () => {
    it('нет авторизации', async () => {
        await render(<Component store={notAuthorizedStore} />, { viewport: { width: 1000, height: 600 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('пустой', async () => {
        await render(<Component store={emptyStore} />);

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });

    it('с данными', async () => {
        await render(<Component store={notEmptyStore} />, { viewport: { width: 1000, height: 1000 } });

        expect(await takeScreenshot({ fullPage: true })).toMatchImageSnapshot();
    });
});
