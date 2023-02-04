import React from 'react';
import { shallow } from 'enzyme';

import { withContext, routerMock } from 'view/lib/test-helpers';

import { OfferForm } from '../';
import FormGroupTypeLocation from '../components/group/location';

describe('OfferForm', () => {
    const actions = {
        setValue() {}
    };

    describe('Properties', () => {
        it('renders an offer form only if type and category were chosen', () => {
            const component = (
                <OfferForm
                    data={{
                        type: 'RENT',
                        category: 'APARTMENT'
                    }}
                    geo={{}}
                    actions={actions}
                    router={routerMock}
                    update={jest.fn()}
                />
            );

            const wrapper = withContext(shallow, component);

            expect(wrapper.find(FormGroupTypeLocation).length).toBe(1);
        });

        it('does not render location without category and type', () => {
            const component = (
                <OfferForm
                    data={{}}
                    geo={{}}
                    actions={actions}
                    router={routerMock}
                    update={jest.fn()}
                />
            );

            const wrapper = withContext(shallow, component);

            expect(wrapper.find(FormGroupTypeLocation).length).toBe(0);
        });
    });
});
