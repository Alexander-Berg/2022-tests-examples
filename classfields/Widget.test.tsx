jest.mock('../../lib/local_storage');
jest.mock('../../lib/metrika');

import React from 'react';
import { shallow } from 'enzyme';
import { Provider, connect } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import MockDate from 'mockdate';
import _ from 'lodash';

import type { ModelState, ModelChatImplementation } from '../../models';
import configMock from '../../../mocks/state/config.mock';
import * as ls from '../../lib/local_storage';
import { WEEK, DAY } from '../../lib/consts';

import Widget from './Widget';
import type { Props as AbstractProps, State as AbstractState, OwnProps as AbstractOwnProps } from './Widget';

const getLsItem = ls.get_item as jest.MockedFunction<typeof ls.get_item>;
const setLsItem = ls.set_item as jest.MockedFunction<typeof ls.set_item>;

const mockStore = configureStore([ thunk ]);
const OFFER_ID = 'offer-id';

interface MyWidgetProps {
    offer_id: string;
    is_mobile: boolean;
}

let props: MyWidgetProps & AbstractOwnProps;
let initialState: Partial<ModelState>;
let sendMetrikaPageEvent: jest.MockedFunction<() => void>;
beforeEach(() => {
    props = {
        show_reason: 'vin',
        offer_id: OFFER_ID,
        is_mobile: false,
        time: '13:43',
        sendMetrikaPageEvent: jest.fn(),
    };
    initialState = {
        config: configMock.value(),
        chat_list: [ { id: 'foo', source: { id: OFFER_ID, category: 'cars' } } ] as Array<ModelChatImplementation>,
        chat_id: 'foo',
    };
    sendMetrikaPageEvent = props.sendMetrikaPageEvent as jest.MockedFunction<() => void>;

    MockDate.set('2020-05-20');
});

afterEach(() => {
    MockDate.reset();
});

describe('клик на крест / пункт меню', () => {
    it('скрывает компонент в десктопе', () => {
        const page = simulateWidgetClose();
        expect(page.isEmptyRender()).toBe(true);
    });

    it('скрывает компонент в мобилке', () => {
        const page = simulateWidgetClose(true);
        expect(page.isEmptyRender()).toBe(true);
    });

    it('отправляет метрику', () => {
        simulateWidgetClose();
        expect(sendMetrikaPageEvent).toHaveBeenCalledTimes(1);
        expect(sendMetrikaPageEvent.mock.calls[0]).toMatchSnapshot();
    });

    describe('local storage', () => {
        it('если ls пустой, добавит первый элемент', () => {
            getLsItem.mockReturnValueOnce(null);
            simulateWidgetClose();

            expect(setLsItem).toHaveBeenCalledTimes(1);
            expect(setLsItem.mock.calls[0]).toMatchSnapshot();
        });

        it('если ls не пустой, но элемента нет, добавит его туда', () => {
            getLsItem.mockReturnValueOnce(JSON.stringify([ { offerId: 'another-offer-id', ts: Date.now() - DAY, is_hidden: true } ]));
            simulateWidgetClose();

            expect(setLsItem).toHaveBeenCalledTimes(1);
            expect(JSON.parse(setLsItem.mock.calls[0][1])).toMatchSnapshot();
        });

        it('если ls переполнен, почистить его при добавлении', () => {
            const lsData = _.fill(Array(65), { offerId: 'some-id', is_hidden: true, ts: Date.now() - 3 * WEEK });
            lsData.push({ offerId: 'newer-offer-id', is_hidden: true, ts: Date.now() - WEEK });
            getLsItem.mockReturnValueOnce(JSON.stringify(lsData));
            simulateWidgetClose();

            expect(setLsItem).toHaveBeenCalledTimes(1);
            expect(JSON.parse(setLsItem.mock.calls[0][1])).toMatchSnapshot();
        });

        it('если ls переполнен и все данные свежие, оставит в нем не больше лимита и новый элемент', () => {
            const lsData = _.fill(Array(65), { offerId: 'some-id', is_hidden: true, ts: Date.now() - WEEK });
            getLsItem.mockReturnValueOnce(JSON.stringify(lsData));
            simulateWidgetClose();

            expect(setLsItem).toHaveBeenCalledTimes(1);
            expect(JSON.parse(setLsItem.mock.calls[0][1])).toHaveLength(51);
            expect(JSON.parse(setLsItem.mock.calls[0][1])[50]).toMatchSnapshot();
        });
    });
});

function simulateWidgetClose(isMobile?: boolean) {
    props.is_mobile = Boolean(isMobile);
    const page = shallowRenderComponent({ props, initialState });

    if (isMobile) {
        const trigger = page.find('.Widget__menuTrigger');
        trigger.simulate('click', { target: { classList: { contains: (name: string): boolean => name === 'Widget__menuTrigger' } } });
        const deleteItem = page.find('.Widget__menuItem_delete');
        deleteItem.simulate('click');
    } else {
        const closer = page.find('.Widget__closer');
        closer.simulate('click');
    }

    return page;
}

function shallowRenderComponent({ initialState, props }: { props: MyWidgetProps & AbstractOwnProps; initialState: Partial<ModelState> }) {
    const store = mockStore(initialState);

    class MyWidget extends Widget<MyWidgetProps & AbstractProps, AbstractState> {
        state = {
            is_menu_opened: false,
            is_hidden: false,
        }

        metrika_name = 'metrika-name';

        local_storage_key = 'autoru_ls_key';

        local_storage_limit = 50;

        render_content(): JSX.Element {
            return <div>Content</div>;
        }

        get_storage_item_key(): string {
            return this.props.offer_id;
        }
    }

    const ConnectedMyWidget = connect()(MyWidget);

    const page = shallow<MyWidget>(
        <Provider store={ store }>
            <ConnectedMyWidget { ...props }/>
        </Provider>,
    );

    return page.dive().dive();
}
