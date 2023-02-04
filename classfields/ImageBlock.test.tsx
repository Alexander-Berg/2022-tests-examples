import React from 'react';
import { shallow } from 'enzyme';

import imageMock from 'auto-core/react/dataDomain/mag/imageMock';

import ImageBlock from './ImageBlock';

it('должен отрендерить с базовыми значениями (широкая картинка)', () => {
    const wrapper = shallow(
        <ImageBlock
            data={{
                type: 'imageWithDescription',
                imageWithDescription: {
                    image: imageMock.value(),
                },
            }}
        />,
    );

    expect(wrapper.find('.ImageBlock').prop('size')).toEqual('none');
    expect(wrapper.find('.ImageBlock__image').prop('layout')).toEqual('responsive');
});

it('должен отрендерить обычную картинку с описанием', () => {
    const wrapper = shallow(
        <ImageBlock
            data={{
                type: 'imageWithDescription',
                imageWithDescription: {
                    type: 'normal',
                    image: imageMock.value(),
                    description: 'Привет',
                },
            }}
        />,
    );

    expect(wrapper.find('.ImageBlock').prop('size')).toEqual('m');
    expect(wrapper.find('.ImageBlock__image').prop('layout')).toEqual('responsive');
});
