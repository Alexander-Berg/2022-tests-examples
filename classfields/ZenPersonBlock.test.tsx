import React from 'react';
import { shallow } from 'enzyme';

import imageMock from 'auto-core/react/dataDomain/mag/imageMock';

import ZenPersonBlock from './ZenPersonBlock';

const TITLE_MOCK = 'Далеко-далеко за словесными горами.';
const SUBTITLE_MOCK = '<p>Далеко-далеко за словесными горами в стране гласных и согласных живут рыбные тексты.<p>';

it('должен отрендерить вариант c заголовком и контентом', () => {
    const wrapper = shallow(
        <ZenPersonBlock
            data={{
                type: 'person',
                person: {
                    title: TITLE_MOCK,
                    subtitle: SUBTITLE_MOCK,
                },
            }}
        />,
    );

    expect(wrapper).toMatchSnapshot();
});

it('должен отрендерить вариант c заголовком, контентом и картинкой', () => {
    const wrapper = shallow(
        <ZenPersonBlock
            data={{
                type: 'person',
                person: {
                    title: TITLE_MOCK,
                    subtitle: SUBTITLE_MOCK,
                    image: imageMock.value(),
                },
            }}
        />,
    );

    expect(wrapper).toMatchSnapshot();
});
