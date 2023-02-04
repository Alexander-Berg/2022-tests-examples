import React from 'react';
import { DeepPartial } from 'utility-types';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import reducer, { ICommonPageStore } from 'view/react/deskpad/reducers/roots/common';

import { WalletBalanceContainer } from '../container';

const Component: React.FunctionComponent<{ store?: DeepPartial<ICommonPageStore>; Gate?: Record<string, unknown> }> = (
    props
) => (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    <AppProvider rootReducer={reducer as any} Gate={props.Gate} initialState={props.store} context={{}}>
        <WalletBalanceContainer />
    </AppProvider>
);

const renderOptions = { viewport: { width: 1000, height: 200 } };

describe('WalletBalance', () => {
    it('невалидный ввод', async () => {
        await render(<Component />, renderOptions);

        await page.click('.TextInput__clear');
        await page.type('.TextInput__control', '0');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
