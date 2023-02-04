import React from 'react';
import { shallow } from 'enzyme';

import RobotsMeta from './RobotsMeta';

const pageTypes = [ 'mag-article-amp', 'mag-article', 'mag-article-amp-old' ];

pageTypes.forEach(pageType => {
    it(`Рендерит метатег max-image-preview:large для страницы ${ pageType }`, () => {
        const Component = () => {
            return RobotsMeta(pageType);
        };

        const wrapper = shallow(<Component/>);

        expect(wrapper).toMatchSnapshot();
    });
});

it('Не рендерит метатег max-image-preview:large для страницы index', () => {
    const Component = () => {
        return RobotsMeta('index');
    };

    const wrapper = shallow(<Component/>);

    expect(wrapper).toMatchSnapshot();
});
