import React from 'react';
import { shallow } from 'enzyme';


import SafeDealCreateModal from './SafeDealCreateModal';


it('должен вызвать метод отправки запроса с новым ценовым предложением', () => {
    const modalConfirm = jest.fn();

    const default_props = {
        handle_modal_close: jest.fn(),
        handle_modal_confirm: modalConfirm,
        is_mobile: false,
        is_request_pending: false,
        is_visible: true,
        price: '10000000',
    };

    const NEW_PRICE = '555555555';

    const tree = shallow(
        <SafeDealCreateModal { ...default_props }/>,
    ).dive();

    tree.find('.SafeDealCreateModal__input').simulate('change', NEW_PRICE);
    tree.find('.SafeDealCreateModal__button').simulate('click');

    expect(modalConfirm).toHaveBeenCalledWith(NEW_PRICE);
});
