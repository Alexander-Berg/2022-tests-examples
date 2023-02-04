import React from 'react';
import { shallow, mount } from 'enzyme';

import { Text } from 'core/client/components/Text/Text';
import { Link } from 'core/client/components/Link/Link';
import { ATTRIBUTES_FOR_EXTERNAL_LINKS } from 'core/client/constants/externalUrls';

import { PartnershipBadge } from './PartnershipBadge';

describe('правильно выставляет атрибуты', () => {
    const wrapper = shallow(
        <PartnershipBadge
            partnershipTitle="ОАО «Константин»"
            partnershipLink="https://example.com"
            className="testclass"
        />
    );

    it('className для контейнера', () => {
        expect(wrapper.find(`.testclass`).exists()).toBe(true);
    });

    it('href, и атрибуты для внешних ссылок', () => {
        const component = wrapper.find(Text).find(Link);

        expect(component.prop('href')).toBe('https://example.com');
        expect(component.prop('attributes')).toEqual(ATTRIBUTES_FOR_EXTERNAL_LINKS);
    });
});

it('меняется заголовок бейджа', () => {
    const wrapper = mount(
        <PartnershipBadge
            partnershipBadgeName="Промо"
            partnershipTitle="ОАО «Константин»"
        />
    );

    expect(wrapper.text()).toBe('Промо ОАО «Константин»');
});
