import React from 'react';
import { shallow } from 'enzyme';

import MarkupText from './MarkupText';

it('должен отрендерить тегом div', () => {
    const tree = shallow(
        <MarkupText>
            <p>Привет!</p>
        </MarkupText>,
    );

    expect(tree.find('div').exists()).toBe(true);
});

it('должен отрендерить тегом h1', () => {
    const tree = shallow(
        <MarkupText text="Привет!" tag="h1"/>,
    );

    expect(tree.find('h1').exists()).toBe(true);
});

it('должен установить класс из пропсов', () => {
    const tree = shallow(
        <MarkupText className="TheOffice"/>,
    );

    expect(tree.find('.TheOffice').exists()).toBe(true);
});
