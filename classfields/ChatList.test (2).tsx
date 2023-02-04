import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

jest.mock('../lib/local_storage');
import * as ls from '../lib/local_storage';
const get_ls_item = ls.get_item as jest.MockedFunction<typeof ls.get_item>;

jest.mock('../actions', () => {
    return {
        Action: {},
        action_load_more_chat_rooms: jest.fn(),
    };
});
import { action_load_more_chat_rooms } from '../actions';
const load_more_chat_rooms = action_load_more_chat_rooms as jest.MockedFunction<typeof action_load_more_chat_rooms>;


jest.mock('../lib/block_scroll', () => {
    return {
        block_scroll: jest.fn(),
        unblock_scroll: jest.fn(),
    };
});

import { ChatList, Props, State } from './ChatList';
import chat_mock from '../../mocks/state/chat.mock';

let props: Props;

beforeEach(() => {
    props = {
        chat_list: [
            chat_mock.withId('id__01').value(),
            chat_mock.withId('id__02').value(),
            chat_mock.withId('id__03').value(),
            chat_mock.withId('id__04').value(),
            chat_mock.withId('id__05').value(),
        ],
        chat_id: null,
        visible: true,
        loaded_rooms_num: 3,
        dispatch: jest.fn(),
    };
});

it('покажет только загруженные комнаты', () => {
    const { page } = shallowRenderComponent({ props });
    const items = page.find('Connect(ChatListItem)');

    expect(items).toHaveLength(props.loaded_rooms_num);
});

describe('при скроле блока', () => {
    it('если до конца осталось менее 200 пикселей и не все комнаты загружены, отправит запрос на загрузки следующей партии', () => {
        const { page, component_instance } = shallowRenderComponent({ props });
        component_instance.ref_chat_list && (component_instance.ref_chat_list.scrollTop = 300);
        page.simulate('scroll');

        expect(load_more_chat_rooms).toHaveBeenCalledTimes(1);
    });

    describe('не будет отправлять запрос', () => {
        it('если все комнаты уже загружены', () => {
            props.loaded_rooms_num = props.chat_list.length;
            const { page, component_instance } = shallowRenderComponent({ props });
            component_instance.ref_chat_list && (component_instance.ref_chat_list.scrollTop = 300);
            page.simulate('scroll');

            expect(load_more_chat_rooms).toHaveBeenCalledTimes(0);
        });

        it('если до конца больше 200 пикселей', () => {
            const { page, component_instance } = shallowRenderComponent({ props });
            component_instance.ref_chat_list && (component_instance.ref_chat_list.scrollTop = 100);
            page.simulate('scroll');

            expect(load_more_chat_rooms).toHaveBeenCalledTimes(0);
        });
    });
});

describe('виджет внешних ссылок', () => {
    it('при маунте обновит стейт инфой из ls', () => {
        get_ls_item.mockReturnValueOnce('[{"key":"333_111","is_hidden":true,"ts":1640995200000}]');

        const { page } = shallowRenderComponent({ props });

        expect((page.state() as State).external_link_ls_data).toEqual([ { is_hidden: true, key: '333_111', ts: 1640995200000 } ]);
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const page = shallow(
        <ChatList { ...props }/>,
        { disableLifecycleMethods: true },
    );


    const component_instance = page.instance() as ChatList;

    component_instance.ref_chat_list = {
        scrollHeight: 1200,
        scrollTop: 0,
        clientHeight: 800,
    } as HTMLDivElement;

    component_instance.throttled_on_list_scroll =
        component_instance.on_list_scroll as (() => void) & _.Cancelable;

    if (typeof component_instance.componentDidMount === 'function') {
        component_instance.componentDidMount();
    }

    return {
        page,
        component_instance,
    };
}
