import React from 'react';
import { DragDropContextProvider } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';

import {
    AppProvider as OriginalAppProvider,
    IAppProviderProps as ICommonProps,
} from 'realty-core/view/react/libs/test-helpers';

enum PageColor {
    DEFAULT = '#FFFFFF',
    USER_LK = '#F5F5F8',
}

interface IAppProviderProps extends ICommonProps {
    bodyBackgroundColor?: PageColor;
}

import { ArendaAclRoleType } from 'app/acl/role-factory';

import { AclRoleContextProviderDevelopment } from 'view/enhancers/withAcl';

interface IArendaAppProviderProps extends IAppProviderProps {
    role?: ArendaAclRoleType;
}

interface IAppProviderComponent extends React.FC<IArendaAppProviderProps> {
    PageColor: typeof PageColor;
}

export const AppProvider: IAppProviderComponent = ({
    rootReducer = (state) => state,
    initialState = {},
    context,
    children,
    Gate,
    rootEpic,
    role,
    fakeTimers,
    bodyBackgroundColor,
} = {}) => {
    if (bodyBackgroundColor) {
        document.body.style.backgroundColor = bodyBackgroundColor;
    }

    const isMobile = document.body.clientWidth < 940;

    const stateWithMobile = {
        ...initialState,
        config: {
            ...initialState.config,
            isMobile: typeof initialState.config?.isMobile !== 'undefined' ? initialState.config.isMobile : isMobile,
        },
    };
    return (
        <DragDropContextProvider backend={HTML5Backend}>
            <AclRoleContextProviderDevelopment predefinedAclRole={role || ArendaAclRoleType.ADMIN_ROLE}>
                <OriginalAppProvider
                    rootReducer={rootReducer}
                    initialState={stateWithMobile}
                    context={context}
                    Gate={Gate}
                    rootEpic={rootEpic}
                    fakeTimers={fakeTimers}
                >
                    {children}
                </OriginalAppProvider>
            </AclRoleContextProviderDevelopment>
        </DragDropContextProvider>
    );
};

AppProvider.PageColor = PageColor;
