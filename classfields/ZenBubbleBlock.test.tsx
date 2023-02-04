import React from 'react';
import { shallow } from 'enzyme';

import imageMock from 'auto-core/react/dataDomain/mag/imageMock';

import ZenBubbleBlock from './ZenBubbleBlock';

it('должен отрендерить вариант c заголовком, контентом и картинкой', () => {
    const TITLE_MOCK = 'Далеко-далеко за словесными горами.';
    const CONTENT_MOCK = '<p>Далеко-далеко за словесными горами в стране гласных и согласных живут рыбные тексты.<p>';
    const IMAGE_MOCK = imageMock.value();

    const wrapper = shallow(
        <ZenBubbleBlock
            data={{
                type: 'bubble',
                bubble: {
                    content: CONTENT_MOCK,
                    image: IMAGE_MOCK,
                    title: TITLE_MOCK,
                },
            }}
        />,
    );

    expect(wrapper).toMatchSnapshot();
});
