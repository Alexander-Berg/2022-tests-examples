import React from 'react';
import { shallow } from 'enzyme';

import { ClientOnlyRender } from '../';

describe('ClientOnlyRender', () => {
    it('Не рендерит вложенные компоненты на сервере', () => {
        const component = shallow(
            <ClientOnlyRender>
                <div>Test</div>
            </ClientOnlyRender>,
            { disableLifecycleMethods: true }
        );

        expect(component.children()).toHaveLength(0);
    });

    it('Рендерит вложенные компоненты на клиенте', () => {
        const component = shallow(
            <ClientOnlyRender>
                <div>Test</div>
            </ClientOnlyRender>,
            { disableLifecycleMethods: false }
        );

        expect(component.children()).not.toHaveLength(0);
    });
});
