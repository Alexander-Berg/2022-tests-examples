import React from 'react';
import { shallow } from 'enzyme';

import Html from '../';

jest.mock('../HtmlHead/icons');

test('renders component', () => {
    const wrapper = shallow(
        <Html
            fontsLoaded
            rootUrl='https://realty.yandex.ru'
            className="TEST_CLASS"
            content="TEST_CONTENT"
            crossorigin="TEST_CROSSORIGIN"
            initialState={{ config: { cspNonce: 'TEST_NONCE' } }}
            lang="TEST_LANG"
            scripts={[ { src: '/build/assets/test.js' } ]}
            stylesheets={[ '/build/assets/test.css' ]}
            staticPath="TEST_STATIC_PATH"
            view="TEST_VIEW"
        />
    );

    expect(wrapper).toMatchSnapshot();
});
