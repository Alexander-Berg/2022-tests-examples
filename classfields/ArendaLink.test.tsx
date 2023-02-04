import React from 'react';
import { shallow } from 'enzyme';

import { ArendaLink } from './ArendaLink';

it('отрендерилась компонента с href', () => {
    const link = shallow(<ArendaLink/>);

    expect(link.prop('href')).toBe('https://arenda.yandex.ru/?utm_source=journal_main_menu');
});
