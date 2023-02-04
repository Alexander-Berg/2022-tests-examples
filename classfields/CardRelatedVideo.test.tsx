import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import CardRelatedVideo from './CardRelatedVideo';

it('правильно формирует параметры запрашиваемого ресурса', () => {
    const tree = shallow(
        <CardRelatedVideo offer={ offerMock }/>,
        { context: contextMock },
    );

    expect(tree.prop('resourceParams')).toEqual({
        category: 'cars',
        mark: 'FORD',
        model: 'ECOSPORT',
        page_size: 6,
        super_gen: '20104320',
    });
});
