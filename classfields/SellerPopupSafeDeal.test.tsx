import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import SellerPopupSafeDeal from './SellerPopupSafeDeal';

it('должен отправить метрику по клику на подробнее', () => {
    const wrapper = shallow(
        <SellerPopupSafeDeal/>,
        { context: contextMock },
    );

    const link = wrapper.find('.SellerPopupSafeDeal__link').dive();
    link.simulate('click');

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'show-phone', 'safe-deal', 'more', 'click' ]);

});
