import React from 'react';

import { DeepPartial } from 'utility-types';

import { AnyObject } from 'realty-core/types/utils';

import { AppProvider } from 'view/libs/test-helpers';
import { rootReducer } from 'view/entries/manager/reducer';
import { IUniversalStore } from 'view/modules/types';

import ModalDisplay from 'view/components/ModalDisplay';

import { ManagerUserChecksContainer } from '../container';

export const Component: React.FunctionComponent<{ store: DeepPartial<IUniversalStore>; Gate?: AnyObject }> = ({
    store,
    Gate,
}) => (
    <AppProvider rootReducer={rootReducer} initialState={store} Gate={Gate}>
        <ManagerUserChecksContainer />
        <ModalDisplay />
    </AppProvider>
);

export const renderOptions = [
    { viewport: { width: 320, height: 568 } },
    { viewport: { width: 768, height: 1024 } },
    { viewport: { width: 1440, height: 900 } },
];
