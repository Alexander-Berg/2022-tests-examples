import React from 'react';
import { mount } from 'enzyme';

import TableCell from '../index';

describe('EGRNTableCell', () => {
    it('should render title when title is passed', () => {
        const wrapper = mount(
            <TableCell title='Title' description='Description' />
        );

        expect(wrapper.find('.container > span').at(0).text()).toBe('Title');
        expect(wrapper.find('.container > span').at(1).text()).toBe('Description');
    });

    it('should render nothing when no text props are passed', () => {
        const wrapper = mount(
            <TableCell />
        );

        expect(wrapper.isEmptyRender()).toEqual(true);
    });

    it('should render nothing when only description is passed', () => {
        const wrapper = mount(
            <TableCell description='Description' />
        );

        expect(wrapper.isEmptyRender()).toEqual(true);
    });

    it('should match snapshot when title and description are passed', () => {
        const wrapper = mount(
            <TableCell
                title='Title'
                description='Description'
            />
        );

        expect(wrapper).toMatchSnapshot();
    });
});
