import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';

import type { Props, State } from './ListingMmmPopup';
import ListingMmmPopup from './ListingMmmPopup';

let wrapper: ShallowWrapper<Props, State>;

it('должен открыть поп-ап моделей после выбора марки', () => {
    const onChange = jest.fn();
    wrapper = shallow(
        <ListingMmmPopup
            index={ 0 }
            mmmInfo={ [] }
            popupState="MARK"
            onChange={ onChange }
            onRequestHide={ () => {} }
            onSubmit={ () => {} }
            renderExcludeControl
            submitText="Давай"
        />,
    );
    wrapper.find('Connect(ListingFiltersPopupMarks)').simulate('markClick', 'MARK');
    expect(wrapper.state().popupState).toBe('MODEL');
});

it('должен правильно вызвать onChange после выбора марки', () => {
    const onChange = jest.fn();
    const onSubmit = jest.fn();
    wrapper = shallow(
        <ListingMmmPopup
            index={ 0 }
            mmmInfo={ [] }
            popupState="MARK"
            onChange={ onChange }
            onRequestHide={ () => {} }
            onSubmit={ onSubmit }
            renderExcludeControl
            submitText="Давай"
        />,
    );
    wrapper.find('Connect(ListingFiltersPopupMarks)').simulate('markClick', 'MARK');
    expect(onChange).toHaveBeenCalledWith([ { mark: 'MARK', models: [] } ], { fetchBreadcrumbs: true, source: 'change' });
    expect(onSubmit).not.toHaveBeenCalled();
});

it('не должен вызвать onChange после выбора марки, которая уже была выбрана', () => {
    const onChange = jest.fn();
    const onSubmit = jest.fn();
    wrapper = shallow(
        <ListingMmmPopup
            index={ 0 }
            mmmInfo={ [ { mark: 'MARK', models: [ { id: 'MODEL', nameplates: [], generations: [] } ] } ] }
            popupState="MARK"
            onChange={ onChange }
            onRequestHide={ () => {} }
            onSubmit={ onSubmit }
            renderExcludeControl
            submitText="Давай"
        />,
    );
    wrapper.find('Connect(ListingFiltersPopupMarks)').simulate('markClick', 'MARK');
    expect(onChange).not.toHaveBeenCalled();
    expect(onSubmit).not.toHaveBeenCalled();
});

it('не должен открыть поп-ап моделей после выбора вендора, а должен засабмитить выбор', () => {
    const onChange = jest.fn();
    const onSubmit = jest.fn();
    wrapper = shallow(
        <ListingMmmPopup
            index={ 0 }
            mmmInfo={ [] }
            popupState="MARK"
            onChange={ onChange }
            onRequestHide={ () => {} }
            onSubmit={ onSubmit }
            renderExcludeControl
            submitText="Давай"
        />,
    );
    wrapper.find('Connect(ListingFiltersPopupMarks)').simulate('vendorClick', 'RU');
    expect(wrapper.state().popupState).toBe('MARK');
    expect(onChange).toHaveBeenCalledWith([ { mark: 'RU', models: [] } ], { fetchBreadcrumbs: true, source: 'change' });
    expect(onSubmit).toHaveBeenCalledWith();
});
