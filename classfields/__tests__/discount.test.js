import React from 'react';
import { shallow } from 'enzyme';
import Discount from '../index';

describe('Discount', () => {
    it('Применяется переданный css-класс', () => {
        const discount = 45;

        const wrapper = shallow(<Discount className='myclass' size='m' text='123' discount={discount} />);

        expect(wrapper.find('.myclass')).toHaveLength(1);
    });

    it('Если НЕ передан text - НЕ выводим его', () => {
        const discount = 45;

        const wrapper = shallow(<Discount discount={discount} />);

        const text = wrapper.find('.Discount__text');

        expect(text).toHaveLength(0);
    });

    it('Если передан text - выводим его', () => {
        const discount = 45;

        const wrapper = shallow(<Discount text='123' discount={discount} />);

        const text = wrapper.find('.Discount__text');

        expect(text).toHaveLength(1);
        expect(text.text()).toBe('123');
    });

    it('Если передан minus - выводим "-" перед скидкой', () => {
        const discount = 45;

        const wrapper = shallow(<Discount minus text='123' discount={discount} />);

        expect(wrapper.find('.Discount__dash')).toHaveLength(1);
    });

    it('По умолчанию "-" не выводится', () => {
        const discount = 45;

        const wrapper = shallow(<Discount text='123' discount={discount} />);

        expect(wrapper.find('.Discount__dash')).toHaveLength(0);
    });

    it('Задается произвольный символ "-"', () => {
        const discount = 45;

        const wrapper = shallow(<Discount minus minusType='--' text='123' discount={discount} />);

        expect(wrapper.find('.Discount__dash').text()).toBe('--');
    });

    it('Если у скидки несуществующий discountType ничего не выводится', () => {
        const discount = 45;

        const wrapper = shallow(<Discount text='123' discount={discount} discountType='non-percent' />);

        expect(wrapper.find('.Discount__postfix').text()).toBe('');
    });

    it('Если discountType "percent" выводм "%"', () => {
        const discount = 45;

        const wrapper = shallow(<Discount text='123' discount={discount} />);

        expect(wrapper.find('.Discount__postfix').text()).toBe('%');
    });

    it('Выводится значение скидки', () => {
        const discount = 45;

        const wrapper = shallow(<Discount text='123' discount={discount} />);

        expect(wrapper.find('.Discount__value').text()).toBe('45');
    });

    it('Применяется стиль размера шилда', () => {
        const discount = 45;

        const wrapper = shallow(<Discount size='m' text='123' discount={discount} />);

        expect(wrapper.find('.Discount_size_m')).toHaveLength(1);
    });

    it('Применяется стиль типа шилда', () => {
        const discount = 45;

        const wrapper = shallow(<Discount type='corner' size='m' text='123' discount={discount} />);

        expect(wrapper.find('.Discount_type_corner')).toHaveLength(1);
    });
});
