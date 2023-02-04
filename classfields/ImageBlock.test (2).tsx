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
    expect(wrapper.find('Memo(Image)').prop('imageAttributes')).toEqual(expect.objectContaining({ sizes: '100vw' }));
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

    expect(wrapper.find('Memo(Image)').prop('imageAttributes'))
        .toEqual(expect.objectContaining({
            decoding: 'async',
            sizes: '(orientation: portrait) and (max-width: 767px) calc(100vw - 24px), 736px',
        }));

});
