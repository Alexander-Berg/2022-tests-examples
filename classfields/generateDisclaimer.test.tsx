import React from 'react';
import { shallow } from 'enzyme';

import Link from 'auto-core/react/components/islands/Link/Link';

import generateDisclaimer from './generateDisclaimer';

describe('generateDisclaimer', () => {
    it('вернет массив строк и Link, tag ссылки заменяется на компонент ссылки, в шаблоне один tag', () => {
        const result = generateDisclaimer(
            'Акция для пользователей Авто.ру, добавивших информацию об автомобиле в раздел «Гараж». ${link_1}',
            ({ index, linkIndex }) => <Link key={ index }>{ `ссылка ${ linkIndex }` }</Link>,
        );

        const wrapper = shallow(
            <div>{ result }</div>,
        );

        expect(wrapper.find(Link)).toHaveLength(1);
        expect(wrapper.find(Link).at(0).children().text()).toBe('ссылка 0');
        expect(result[0]).toBe('Акция для пользователей Авто.ру, добавивших информацию об автомобиле в раздел «Гараж». ');
    });

    it('вернет массив строк и Link, tag ссылки заменяется на компонент ссылки, в шаблоне несколько tag', () => {
        const result = generateDisclaimer(
            'Текст ${link_1} еще текст ${link_1}, а затем еще один ${link_2}.',
            ({ index, linkIndex }) => <Link key={ index }>{ `ссылка ${ linkIndex }` }</Link>,
        );

        const wrapper = shallow(
            <div>{ result }</div>,
        );

        expect(wrapper.find(Link)).toHaveLength(3);
        expect(wrapper.find(Link).at(0).children().text()).toBe('ссылка 0');
        expect(wrapper.find(Link).at(1).children().text()).toBe('ссылка 0');
        expect(wrapper.find(Link).at(2).children().text()).toBe('ссылка 1');
        expect(result[0]).toBe('Текст ');
        expect(result[2]).toBe(' еще текст ');
        expect(result[4]).toBe(', а затем еще один ');
        expect(result[6]).toBe('.');
    });

    it('вернет массив строк, tag в шаблоне нет', () => {
        const result = generateDisclaimer(
            'Какой-то текст',
            ({ index, linkIndex }) => <Link key={ index }>{ `ссылка ${ linkIndex }` }</Link>,
        );

        const wrapper = shallow(
            <div>{ result }</div>,
        );

        expect(wrapper.find(Link)).toHaveLength(0);
        expect(result[0]).toBe('Какой-то текст');
    });
});
