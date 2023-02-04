import React from 'react';
import { shallow } from 'enzyme';

import HtmlHead from '../';

test('renders component', () => {
    const wrapper = shallow(
        <HtmlHead
            cspNonce="TEST_NONCE"
            scripts={[ { content: 'TEST_SCRIPT_CONTENT' }, { src: '//TEST_SCRIPT_SRC' } ]}
            view="TEST_VIEW"
        />
    );

    expect(wrapper).toMatchSnapshot();
});
