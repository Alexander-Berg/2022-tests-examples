import React from 'react';
import { shallow } from 'enzyme';

import BodyTypeFilter from './BodyTypeFilter';

it('правильно обрабатывает отмену изменений', () => {
    const onChange = jest.fn();
    const tree = shallow(
        <BodyTypeFilter
            category="moto"
            motoCategory="MOTORCYCLE"
            onChange={ onChange }
            offersCount={ 100 }
            value={ [ 'item0' ] }
        />,
    );
    tree.setState({ opened: true });
    tree.setProps({ value: [ 'item0', 'item1' ] });
    tree.find('FiltersPopup').simulate('close');
    expect(onChange).toHaveBeenCalledWith([ 'item0' ], { name: 'moto_type' });
});
