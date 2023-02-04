/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';
import { Helmet } from 'react-helmet';

import { mockStore } from 'core/mocks/store.mock';
import { ServiceId } from 'core/services/ServiceId';
import { ROUTER_SERVICE_MOCK_1 } from 'core/services/router/mocks/routerService.mock';

import { Stack } from '../Stack/Stack';
import { Link } from '../Link/Link';

import { Breadcrumbs } from './Breadcrumbs';

const ITEMS_MOCK = [
    { href: 'https://realty.yandex.ru', isHostItem: true, text: 'Я.Недвижимость' },
    { href: '/journal/', text: 'Журнал' },
    { href: '/journal/category/uchebnik/', text: 'Учебник' },
    { text: 'Аренда' },
];

describe('в разметке есть', () => {
    mockStore({ [ServiceId.ROUTER]: ROUTER_SERVICE_MOCK_1 });

    const wrapper = shallow(<Breadcrumbs items={ ITEMS_MOCK }/>);

    it('правильные теги и атрибуты для доступности', () => {
        expect(wrapper.find(Stack).prop('as')).toBe('ul');
        expect(wrapper.find(Stack).prop('itemAs')).toBe('li');
        expect(wrapper.find(Stack).prop('attributes')).toEqual({
            'aria-label': 'Навигационная цепочка',
        });
    });

    it('микроразметка для BreadcrumbList', () => {
        const LDJson = JSON.parse(
            wrapper.find(Helmet).find('script[type="application/ld+json"]').text()
        );

        expect(LDJson['@type']).toBe('BreadcrumbList');
    });

    it('последний элемент не ссылка', () => {
        expect(wrapper.find(Link).at(3).prop('href')).toBeUndefined();
    });
});
