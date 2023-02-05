import * as React from 'react';
import {mount} from 'enzyme';
import Notifications from './notifications';
import NotificationsStore from 'stores/notifications-store';
import {Provider} from 'mobx-react';
import {notifications} from 'mocks/notifications-mock';

describe('<Notifications />', () => {
    it('should close notification', () => {
        const store = new NotificationsStore();
        notifications.forEach((item) => store.add(item));

        const component = mount(
            <Provider notifications={store}>
                <Notifications />
            </Provider>
        );

        const closeButtons = component.find('.notification-item__close');
        expect(closeButtons).toHaveLength(notifications.length);
        closeButtons.at(0).simulate('click');
        closeButtons.at(0).simulate('animationend');
        expect(component.find('.notification-item__close')).toHaveLength(notifications.length - 1);
    });
});
