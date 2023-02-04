import React from 'react';
import { shallow } from 'enzyme';

import type { Props } from '../AbstractTagsFilter/AbstractTagsFilter';
import AbstractTagsFilter from '../AbstractTagsFilter/AbstractTagsFilter';

class TestFilter extends AbstractTagsFilter<Props> {
    filterName = 'fuel_rate_to';
    filterTitle = 'Расход до, л';

    renderContent() {
        return <div>ПРИВЕТ</div>;
    }
}

it('правильно вычисляет описание параметра', () => {
    const tree = shallow(
        <TestFilter category="cars" onChange={ () => {} } value="8"/>,
    );
    expect(tree.props().description.replace(/\s/g, ' ')).toBe('до 8 л');
});
