import React from 'react';
import { shallow } from 'enzyme';

import imageMock from 'auto-core/react/dataDomain/mag/imageMock';

import Image from './Image';

const sizesMock = imageMock.value().sizes;

it('рендерит картинку с переданными src, attributes, imageAttributes, className', () => {
    const wrapper = shallow(
        <Image
            sizes={ sizesMock }
            attributes={{
                style: { opacity: 1 },
            }}
            className="Test"
            imageAttributes={{
                sizes: '100px',
                className: 'Test__image',
                decoding: 'sync',
            }}
        />,
    );

    expect(wrapper).toMatchSnapshot();
});

it('рендерит картинку с соотношением сторон 16/9', () => {
    const wrapper = shallow(
        <Image sizes={ sizesMock } aspectRatio={ 16 / 9 }/>,
    );

    expect(wrapper.find('Memo(AspectRatio)').prop('ratio')).toBe(1.7777777777777777);
});

describe('Пути', () => {
    const defaultSrc = 'https://mag.auto.ru/';

    it('рендерит картинку с переданным src', () => {
        const wrapper = shallow(<Image src={ defaultSrc }/>);

        expect(wrapper.find('img').prop('src')).toBe(defaultSrc);
    });

    it('устанавливает srcSet и src есть переданы размеры', () => {
        const wrapper = shallow(<Image sizes={ sizesMock }/>);

        expect(wrapper).toMatchSnapshot();
    });

    it('устанавливает src из пропсов, если передан src и sizes', () => {
        const wrapper = shallow(<Image src={ defaultSrc } sizes={ sizesMock }/>);

        expect(wrapper.find('img').prop('src')).toBe(defaultSrc);
    });
});
