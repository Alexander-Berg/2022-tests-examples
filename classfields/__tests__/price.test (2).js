import React from 'react';
import { shallow } from 'enzyme';

import { FormGroupTypePrice } from '..';

const setValue = jest.fn();

const props = {
    newFlat: false,
    category: 'APARTMENT',
    activeControls: {},
    errors: {},
    controlNames: []
};

describe('Price', () => {
    beforeEach(() => {
        setValue.mockClear();
    });

    it('clears dealType if newFlat changed', () => {
        const wrapper = shallow(<FormGroupTypePrice {...props} setValue={setValue} />);

        wrapper.setProps({ newFlat: true });
        expect(setValue).toHaveBeenCalled();
    });

    it("doesn't clear dealType if newFlat didn't change", () => {
        const wrapper = shallow(<FormGroupTypePrice {...props} setValue={setValue} />);

        wrapper.setProps({ newFlat: false });

        expect(setValue).not.toHaveBeenCalled();
    });
});
