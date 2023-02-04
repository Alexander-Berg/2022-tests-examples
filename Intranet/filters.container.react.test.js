import React from 'react';
import FiltersContainer from 'b:filters.container';
import {mount} from 'enzyme';

describe('FiltersContainer component', () => {
    const MORE_FILTERS_SHOW_BUTTON_SELECTOR = '.filters__toggle-additional';
    const CLEAR_FILTERS_SELECTOR = '.filters__clear';
    const APPLY_FILTERS_SELECTOR = '.filters__apply';
    const INPUT_COMMON_NAME_SELECTOR = '.filters__main .filters__filter_type_textinput .textinput__control';

    let data;
    let filters;

    let updateQueryStr;
    let filterQueryStr;

    let query;

    beforeEach(() => {

        const additionalFiltersKeys = [
            'ca_name',
            'requester',
            'host',
            'serial_number',
            'abc_service'
        ];

        const mainFiltersKeys = [
            'type',
            'status',
            'user',
            'common_name'
        ];

        const filtersKeys = [
            ...mainFiltersKeys,
            ...additionalFiltersKeys
        ];

        filters = filtersKeys.reduce((acc, key) => {
            acc[key] = key;

            return acc;
        }, {});

        const getFilters = jest.fn();

        getFilters.mockReturnValue(filters);

        // Возвращает результаты, укутанные в массив
        query = filtersKeys.reduce((acc, key) => {
            acc[key] = [key];

            return acc;
        }, {});

        query.randomParam = ['randomParam'];

        updateQueryStr = jest.fn();
        filterQueryStr = jest.fn();

        data = {
            getFilters,
            filtersKeys,
            additionalFiltersKeys,
            onApplyClick: jest.fn(),
            onClearClick: jest.fn(),
            onFilterChange: jest.fn(),
            disableScroll: jest.fn(),
            enableScroll: jest.fn(),
            onToggleAdditionalFiltersClick: jest.fn(),
            updateQueryStr,
            filterQueryStr
        };
    });

    describe('renders correctly', () => {
        it('when full data is provided', () => {
            const wrapper = mount(
                <FiltersContainer {...data} />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });

        it('when partial data is provided', async () => {
            filters.serial_number = '';
            filters.host = '';

            const wrapper = mount(
                <FiltersContainer {...data} />
            );

            expect(wrapper).toMatchSnapshot();
            await wrapper.unmount();
        });

        it('when partial applied filters num is 0', async () => {
            data.additionalFiltersAppliedNum = 0;

            const wrapper = mount(
                <FiltersContainer {...data} />
            );

            expect(wrapper).toMatchSnapshot();
            await wrapper.unmount();
        });
    });

    it('shows and hides additional menu', () => {
        const wrapper = mount(
            <FiltersContainer {...data} />
        );

        expect(data.disableScroll.mock.calls.length).toBe(0);

        expect(data.enableScroll.mock.calls.length).toBe(0);

        wrapper.find(MORE_FILTERS_SHOW_BUTTON_SELECTOR).simulate('click');

        expect(wrapper).toMatchSnapshot();

        expect(data.disableScroll.mock.calls.length).toBe(1);

        expect(data.enableScroll.mock.calls.length).toBe(0);

        wrapper.find(MORE_FILTERS_SHOW_BUTTON_SELECTOR).simulate('click');

        expect(wrapper).toMatchSnapshot();

        expect(data.disableScroll.mock.calls.length).toBe(1);

        expect(data.enableScroll.mock.calls.length).toBe(1);

        wrapper.unmount();
    });

    it('updates url on plain text input filter changes', () => {
        const wrapper = mount(
            <FiltersContainer {...data} />
        );

        wrapper.find(INPUT_COMMON_NAME_SELECTOR).simulate('change', {target: {value: 'brand new common name'}});

        expect(wrapper).toMatchSnapshot();

        expect(updateQueryStr.mock.calls.length).toBe(1);

        expect(updateQueryStr.mock.calls[0]).toEqual([{common_name: 'brand new common name'}]);

        wrapper.unmount();
    });

    it('does not update url when addition filters are show', () => {
        const wrapper = mount(
            <FiltersContainer {...data} />
        );

        wrapper.find(MORE_FILTERS_SHOW_BUTTON_SELECTOR).simulate('click');

        expect(wrapper).toMatchSnapshot();

        wrapper.find(INPUT_COMMON_NAME_SELECTOR).simulate('change', {target: {value: 'brand new common name'}});

        expect(updateQueryStr.mock.calls.length).toBe(0);

        wrapper.unmount();
    });

    it('discards all the filters on revert changes button click', () => {
        const wrapper = mount(
            <FiltersContainer {...data} />
        );

        wrapper.find(MORE_FILTERS_SHOW_BUTTON_SELECTOR).simulate('click');

        wrapper.find(CLEAR_FILTERS_SELECTOR).simulate('click');

        expect(wrapper).toMatchSnapshot();

        wrapper.unmount();
    });

    it('applies all the filters on revert changes button click', () => {
        const wrapper = mount(
            <FiltersContainer {...data} />
        );

        wrapper.find(MORE_FILTERS_SHOW_BUTTON_SELECTOR).simulate('click');

        wrapper.find(INPUT_COMMON_NAME_SELECTOR).simulate('change', {target: {value: 'brand new common name'}});

        wrapper.find(APPLY_FILTERS_SELECTOR).simulate('click');

        expect(wrapper).toMatchSnapshot();

        expect(updateQueryStr.mock.calls.length).toEqual(1);

        expect(updateQueryStr.mock.calls[0]).toEqual([{common_name: 'brand new common name'}]);

        wrapper.unmount();
    });
});
