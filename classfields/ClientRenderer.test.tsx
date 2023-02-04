import React from 'react';
import { shallow } from 'enzyme';

import ClientRenderer from './ClientRenderer';

it('ClientRenderer не рендерит вложенные компоненты на сервере', () => {
    const component = shallow(
        <ClientRenderer>
            <div>Test</div>
        </ClientRenderer>,
        { disableLifecycleMethods: true },
    );

    expect(component.children()).toHaveLength(0);
});

it('ClientRenderer рендерит вложенные компоненты на клиенте', () => {
    const component = shallow(
        <ClientRenderer>
            <div>Test</div>
        </ClientRenderer>,
        { disableLifecycleMethods: false },
    );

    expect(component.children()).not.toHaveLength(0);
});
