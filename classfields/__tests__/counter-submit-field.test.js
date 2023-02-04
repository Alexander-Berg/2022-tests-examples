import * as React from 'react';
import { shallow } from 'enzyme';
import I18N from 'realty-core/view/react/libs/i18n';

import CounterSubmitFiltersFormField from
    'realty-core/view/react/modules/filters/common/FiltersForm/fields/CounterSubmitFiltersFormField';

import i18n from '../i18n';

const defaultProps = {
    i18n,
    i18nKey: 'offers',
    name: 'test',
    buildValueLabel: () => {},
    buildLabel: () => {},
    data: { statuses: {} },
    decl: {}
};

describe('CounterSubmitFiltersFormField', () => {
    beforeEach(() => {
        I18N.setLang('ru');
    });

    it('renders submit button with count if it has matchedQuantity', () => {
        const wrapper = shallow(
            <CounterSubmitFiltersFormField
                withCounter
                matchedQuantity={50}
                {...defaultProps}
            />
        );

        expect(wrapper.html().includes('Показать 50 объявлений')).toBe(true);
    });

    it('renders submit button without count if it has not matchedQuantity', () => {
        const wrapper = shallow(
            <CounterSubmitFiltersFormField
                matchedQuantity={50}
                {...defaultProps}
            />
        );

        expect(wrapper.html().includes('Найти')).toBe(true);
    });

    it('renders submit button without count if matchedQuantity equals -1', () => {
        const wrapper = shallow(
            <CounterSubmitFiltersFormField
                withCounter
                matchedQuantity={-1}
                {...defaultProps}
            />
        );

        expect(wrapper.html().includes('Показать объявления')).toBe(true);
    });
});
