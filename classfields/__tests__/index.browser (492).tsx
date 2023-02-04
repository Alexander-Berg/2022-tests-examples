import React from 'react';
import { DeepPartial } from 'utility-types';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import reducer, { ICommonPageStore } from 'view/react/deskpad/reducers/roots/common';

import { WalletEmailContainer } from '../container';

const Component: React.FunctionComponent<{ store?: DeepPartial<ICommonPageStore>; Gate?: Record<string, unknown> }> = (
    props
) => (
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    <AppProvider rootReducer={reducer as any} Gate={props.Gate} initialState={props.store} context={{}}>
        <WalletEmailContainer />
    </AppProvider>
);

const renderOptions = { viewport: { width: 1000, height: 150 } };

describe('WalletEmail', () => {
    it('невалидный ввод', async () => {
        await render(<Component />, renderOptions);

        await page.type('.TextInput__control', 'test@');

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
