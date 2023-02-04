import React from 'react';
import { shallow } from 'enzyme';

import IconSvg from 'auto-core/react/components/islands/IconSvg/IconSvg';
import Button from 'auto-core/react/components/islands/Button/Button';

import C2bAuctionExitNotification from './C2bAuctionExitNotification';

describe('C2bAuctionExitNotification - компонент для отрисовки контента модалки при уходе с вкладки аукциона', () => {
    it('отрисовывает иконку, тексты и две кнопки - Выйти и Остаться', () => {
        const onLeaveMock = jest.fn();
        const onStayMock = jest.fn();

        const wrapper = shallow(
            <C2bAuctionExitNotification onLeave={ onLeaveMock } onStay={ onStayMock }/>,
        );

        expect(wrapper.find(IconSvg).exists()).toBe(true);

        const title = wrapper.find('.C2bAuctionExitNotification__title');

        expect(title.exists()).toBe(true);
        expect(title.text()).toBe('Отозвать заявку?');

        expect(wrapper.find('.C2bAuctionExitNotification__description').exists()).toBe(true);

        const buttons = wrapper.find(Button);

        expect(buttons).toHaveLength(2);

        const leaveButon = buttons.first();
        leaveButon.simulate('click');

        expect(onLeaveMock).toHaveBeenCalledTimes(1);

        const stayButon = buttons.last();
        stayButon.simulate('click');

        expect(onStayMock).toHaveBeenCalledTimes(1);
    });
});
