jest.mock('../../../lib/metrika');
jest.mock('../../../lib/local_storage');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import MockDate from 'mockdate';

import * as ls from '../../../lib/local_storage';
import { ModelChatMessageContentType, ModelState } from '../../../models';
import * as metrika from '../../../lib/metrika';
import configMock from '../../../../mocks/state/config.mock';

import { OwnProps as AbstractOwnProps } from '../Widget';
import ExternalLinkWarningWidget, { OwnProps } from './ExternalLinkWarningWidget';

const mock_store = configureStore([ thunk ]);
const set_ls_item_mock = ls.set_item as jest.MockedFunction<typeof ls.set_item>;
const send_metrika_page_event_mock = metrika.send_page_event as jest.MockedFunction<typeof metrika.send_page_event>;

const on_message_hide_mock = jest.fn();

let props: AbstractOwnProps & OwnProps;
let initial_state: Partial<ModelState>;
beforeEach(() => {
    props = {
        show_reason: '',
        is_mobile: false,
        time: '23:23',
        on_message_hide: on_message_hide_mock,
        message: {
            id: '111',
            room_id: '333',
            author: '',
            user: {
                id: '',
                is_me: true,
            },
            created: '01-01-2020 12:00',
            payload: { content_type: ModelChatMessageContentType.TEXT_PLAIN, value: '' },
            properties: {},
        },
        is_last_message: false,
    };
    initial_state = {
        config: configMock.value(),
        chat_id: 'foo',
    };
    MockDate.set('2022-01-01');
});
afterEach(() => {
    MockDate.reset();
});

describe('ExternalLinkWarningWidget', () => {
    it('метрика на маунт', () => {
        shallowRenderComponent({ props, initial_state });

        expect(send_metrika_page_event_mock).toHaveBeenCalledWith('metrika-id', [ 'chat_widgets', 'external_link', 'show' ]);
    });

    it('при клике отправит метрику close, запишет новые данные в ls и не будет отправлять никаких экшенов', () => {
        const { wrapper, store } = shallowRenderComponent({ props, initial_state });

        wrapper.find('span').simulate('click');

        expect(send_metrika_page_event_mock).toHaveBeenLastCalledWith('metrika-id', [ 'chat_widgets', 'external_link', 'close' ]);
        expect(set_ls_item_mock).toHaveBeenCalledWith('autoru_external_link_chat_message', '[{"key":"333_111","is_hidden":true,"ts":1640995200000}]');
        expect(on_message_hide_mock).toHaveBeenCalled();
        expect(store.getActions()).toEqual([]);
    });

    it('при клике отправит экшен, если это последнее соообщение в чате', () => {
        const { wrapper, store } = shallowRenderComponent({ props: { ...props, is_last_message: true }, initial_state });

        wrapper.find('span').simulate('click');

        expect(store.getActions()).toEqual([ { payload: '333_111', type: 46 } ]);
    });
});

function shallowRenderComponent({ props, initial_state }: { props: OwnProps & AbstractOwnProps; initial_state: Partial<ModelState> }) {
    const store = mock_store(initial_state);

    const wrapper = shallow(
        <Provider store={ store }>
            <ExternalLinkWarningWidget { ...props }/>
        </Provider>,
    );

    return {
        wrapper: wrapper.dive().dive(),
        store: store,
    };
}
