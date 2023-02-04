import React, { useEffect, useState, useCallback } from 'react';

import { ConnectionOptions } from '@trust/utils/connections/connection-options';
import { MessageConnectionListener } from '@trust/utils/connections/message-connection-listener';
import * as Sdk from '@yandex-pay/sdk/src/typings';

import { isTest } from '../../helpers/env';

interface TestSettingsContext {
    data: {
        abt: any;
    };
    toggle: Sys.CallbackFn1<keyof TestSettingsContext['data']>;
}

const InitialContext: TestSettingsContext = {
    data: {
        abt: {},
    },
    toggle: () => undefined,
};

export const testSettingsContext = React.createContext<TestSettingsContext>(InitialContext);

export function TestSettingsProvider({ children }: { children: JSX.Element }): JSX.Element {
    const [data, setData] = useState(InitialContext.data);

    const toggle = useCallback((param: keyof TestSettingsContext['data']) => {
        setData((_data) => ({ ..._data, [param]: !_data[param] }));
    }, []);

    useEffect(() => {
        const options = new ConnectionOptions();
        const listener = new MessageConnectionListener(options);

        listener.on((event: any) => {
            if (event && event.type === Sdk.InnerEventType.SdkExp) {
                setData((_data) => ({ ..._data, abt: event.flags }));
            }
        });

        return (): void => listener.destroy();
    }, []);

    return React.createElement(
        testSettingsContext.Provider,
        {
            value: isTest()
                ? {
                      data,
                      toggle,
                  }
                : {
                      data: InitialContext.data,
                      toggle: InitialContext.toggle,
                  },
        },
        children,
    );
}
