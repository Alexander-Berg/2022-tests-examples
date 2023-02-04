import React from 'react';

import { DeepPartial } from 'utility-types';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/manager/reducer';
import { IUniversalStore } from 'view/modules/types';

import { ManagerFlatPaymentContainer } from '../ManagerFlatPayment/container';
import { ManagerFlatPaymentTab } from '../ManagerFlatPayment/types';

export const viewports = [
    { viewport: { width: 1280, height: 1000 } },
    { viewport: { width: 375, height: 1000 } },
    { viewport: { width: 320, height: 1000 } },
];

export const FlatPayment: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <ManagerFlatPaymentContainer tab={ManagerFlatPaymentTab.PAYMENT_INFO} />
    </AppProvider>
);
