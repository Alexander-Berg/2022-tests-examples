import React from 'react';
import { shallow } from 'enzyme';

import FormGroup from '../../../controls/ui/form-group';
import { FormGroupTypeProperties } from '../';

describe('Properties', () => {
    it('renders properties only if category was chosen', () => {
        const component = (
            <FormGroupTypeProperties
                activeControls={{}}
                controlNames={[]}
                errors={{}}
                category='APARTMENT'
            />
        );

        const wrapper = shallow(component);

        expect(wrapper.find(FormGroup).length).toBe(1);
    });

    it('does not render properties only if there is no category', () => {
        const component = (
            <FormGroupTypeProperties
                activeControls={{}}
                controlNames={[]}
                errors={{}}
            />
        );

        const wrapper = shallow(component);

        expect(wrapper.find(FormGroup).length).toBe(0);
    });

    it('does not render properties only if category is COMMERCIAL and there is no commercial type', () => {
        const component = (
            <FormGroupTypeProperties
                activeControls={{}}
                controlNames={[]}
                errors={{}}
                category='COMMERCIAL'
            />
        );

        const wrapper = shallow(component);

        expect(wrapper.find(FormGroup).length).toBe(0);
    });

    it('renders properties only if category is COMMERCIAL and commercial type was chosen', () => {
        const component = (
            <FormGroupTypeProperties
                activeControls={{}}
                controlNames={[]}
                errors={{}}
                category={'COMMERCIAL'}
                commercialType={'OFFICE'}
            />
        );

        const wrapper = shallow(component);

        expect(wrapper.find(FormGroup).length).toBe(1);
    });
});
