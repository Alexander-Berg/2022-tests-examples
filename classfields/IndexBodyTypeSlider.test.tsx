import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import IndexBodyTypes from 'auto-core/react/components/common/IndexBodyTypes/IndexBodyTypes';

import IndexBodyTypeSlider from './IndexBodyTypeSlider';

it('должен брать параметры запроса из поля "value", если оно есть', () => {
    const context = { ...contextMock };
    const wrapper = shallow(
        <IndexBodyTypeSlider/>,
        { context },
    );

    wrapper.find('Link').forEach((node, idx) => {
        const item = IndexBodyTypes.BODY_TYPES[idx];

        const bodyTypeGroup = item.value || [ item.id ];

        expect(node.prop('url')).toBe(context.link('listing', { body_type_group: bodyTypeGroup }));
    });
});
