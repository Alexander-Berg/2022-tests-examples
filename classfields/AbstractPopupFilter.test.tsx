import React from 'react';
import { shallow } from 'enzyme';
import 'jest-enzyme';

import type { Props, State } from './AbstractPopupFilter';
import AbstractPopupFilter from './AbstractPopupFilter';

class TestFilter extends AbstractPopupFilter<Props, State> {
    filterName='filter';
    filterTitle='ФИЛЬТР';
    popupTitle='Привет';
    onCancel=() => {
        return;
    };
    renderContent() {
        return <div>ПРИВЕТ</div>;
    }
}

it('Должен запомнить исходное значение фильтра при его открытии', () => {
    const wrapper = shallow(
        <TestFilter
            category="cars"
            onChange={ () => {} }
            offersCount={ 100 }
            value="some value"
            searchParameters={{ category: 'cars' }}
        />,
    );
    expect((wrapper.state() as State).initialValue).toBeNull();
    wrapper.setState({ opened: true });
    wrapper.setProps({ value: 'other value' });
    expect((wrapper.state() as State).initialValue).toBe('some value');
});

it('Должен сбросить исходное значение фильтра при его закрытии', () => {
    const wrapper = shallow(
        <TestFilter
            category="cars"
            onChange={ () => {} }
            offersCount={ 100 }
            value="some value"
            searchParameters={{ category: 'cars' }}
        />,
    );
    wrapper.setState({ opened: true });
    wrapper.setState({ opened: false });
    expect((wrapper.state() as State).initialValue).toBeNull();
});
