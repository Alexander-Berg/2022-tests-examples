import React from 'react';
import { DeepPartial } from 'utility-types';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';
import { ICoreStore } from 'realty-core/view/react/common/reducers/types';
import { AnyObject } from 'realty-core/types/utils';

import { MainMenuContainer } from '../container';

export const Component: React.FunctionComponent<
    React.ComponentProps<typeof MainMenuContainer> & { store: DeepPartial<ICoreStore> }
> = ({ store, ...otherProps }) => (
    <AppProvider
        disableSetTimeoutDelay
        initialState={store}
        context={{ link: (...args: [string, AnyObject]) => JSON.stringify(args) }}
    >
        <MainMenuContainer {...otherProps} />
    </AppProvider>
);
