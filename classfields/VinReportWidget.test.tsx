jest.mock('../../../lib/local_storage');
jest.mock('../../../lib/metrika', () => {
    return { send_page_event: jest.fn() };
});

import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import MockDate from 'mockdate';

import type { OwnProps as AbstractOwnProps } from '../Widget';
import type { ModelState, ModelChatImplementation } from '../../../models';
import { VasService } from '../../../lib/vas_logger';
import configMock from '../../../../mocks/state/config.mock';
import * as ls from '../../../lib/local_storage';
import * as metrika from '../../../lib/metrika';
import * as vas_logger from '../../../lib/vas_logger';

const get_ls_item = ls.get_item as jest.MockedFunction<typeof ls.get_item>;
const sendMetrikaPageEvent = metrika.send_page_event as jest.MockedFunction<typeof metrika.send_page_event>;

const mockLogVasEvent = jest.fn();
jest.mock('../../../lib/vas_logger', () => {
    return {
        VasLogger: jest.fn().mockImplementation(() => {
            return {
                log_vas_event: mockLogVasEvent,
            };
        }),
        VasService: {
            reports: 'reports',
        },
        VasEvent: {
            show: 'show',
            click: 'click',
        },
    };
});

import VinReportWidget from './VinReportWidget';
import type { OwnProps } from './VinReportWidget';
const mockStore = configureStore([ thunk ]);
const OFFER_ID = 'offer-id';

let props: OwnProps & AbstractOwnProps;
let initialState: Partial<ModelState>;

beforeEach(() => {
    props = {
        show_reason: 'vin',
        info: {
            status: 'NEED_PAYMENT',
            payment_data: {
                price: 777,
                original_price: 777,
                service: VasService.reports,
                counter: '1',
            },
        },
        time: '13:43',
        is_mobile: false,
        on_vin_report_page_redirect: jest.fn(),
        on_buy_report_with_quota: jest.fn(),
        message_id: '123',
        user_type: 'user',
        sendMetrikaPageEvent: jest.fn(),
    };
    initialState = {
        config: configMock.value(),
        chat_list: [ { id: 'foo', source: { id: OFFER_ID, category: 'cars' } } ] as Array<ModelChatImplementation>,
        chat_id: 'foo',
    };

    MockDate.set('2020-05-20');
});

afterEach(() => {
    MockDate.reset();
});

describe('маунт', () => {
    describe('компонент не виден', () => {
        it('если нет info', () => {
            props.info = undefined;
            const { page } = shallowRenderComponent({ props, initialState });
            expect(page.isEmptyRender()).toBe(true);
        });

        it('если статус fetching', () => {
            props.info && (props.info.status = 'FETCHING');
            const { page } = shallowRenderComponent({ props, initialState });
            expect(page.isEmptyRender()).toBe(true);
        });

        it('если нет данных', () => {
            props.info && (props.info.payment_data = undefined);
            const { page } = shallowRenderComponent({ props, initialState });
            expect(page.isEmptyRender()).toBe(true);
        });

        it('если был скрыт для этого сообщения', () => {
            get_ls_item.mockReturnValueOnce(JSON.stringify([ { key: 'offer-id_123', is_hidden: true } ]));
            const { page } = shallowRenderComponent({ props, initialState });
            expect(page.isEmptyRender()).toBe(true);
        });
    });

    describe('компонент виден', () => {
        let page: any;

        beforeEach(() => {
            page = shallowRenderComponent({ props, initialState }).page;
        });

        it('нарисует компонент', () => {
            expect(page.isEmptyRender()).toBe(false);
        });

        it('инициализирует логгер', () => {
            expect(vas_logger.VasLogger).toHaveBeenCalledTimes(1);
            expect(vas_logger.VasLogger).toHaveBeenCalledWith(
                'metrika-id',
                { category: 'cars', from: 'desktop_chat_widget', is_mobile: false, offer_id: 'offer-id', service: 'reports' },
            );
        });

        it('отправит метрику и вас лог', () => {
            expect(mockLogVasEvent).toHaveBeenCalledTimes(1);
            expect(mockLogVasEvent.mock.calls[0]).toMatchSnapshot();

            expect(sendMetrikaPageEvent).toHaveBeenCalledTimes(1);
            expect(sendMetrikaPageEvent.mock.calls[0]).toMatchSnapshot();
        });
    });
});

describe('клик на кнопку "купить"', () => {
    beforeEach(() => {
        mockLogVasEvent.mockClear();
        sendMetrikaPageEvent.mockClear();
    });

    it('откроет модал оплаты если нет квоты', () => {
        const { page, store } = shallowRenderComponent({ props, initialState });
        const button = page.find('.VinReportWidget__button');
        button.simulate('click');

        expect(store.getActions()).toMatchSnapshot();
    });

    it('уменьшит квоту если есть квота', () => {
        props.info && (props.info.quota_left = 3);
        const { page } = shallowRenderComponent({ props, initialState });
        const button = page.find('.VinReportWidget__button');
        button.simulate('click');

        expect(props.on_buy_report_with_quota).toHaveBeenCalledTimes(1);
        expect(props.on_buy_report_with_quota).toHaveBeenCalledWith(true);
    });

    it('отправит вас лог', () => {
        const { page } = shallowRenderComponent({ props, initialState });
        const button = page.find('.VinReportWidget__button');
        button.simulate('click');

        expect(mockLogVasEvent).toHaveBeenCalledTimes(2);
        expect(mockLogVasEvent.mock.calls[1]).toMatchSnapshot();
    });

    it('отправит метрику', () => {
        const { page } = shallowRenderComponent({ props, initialState });
        const button = page.find('.VinReportWidget__button');
        button.simulate('click');

        expect(sendMetrikaPageEvent).toHaveBeenCalledTimes(2);
        expect(sendMetrikaPageEvent.mock.calls[1]).toMatchSnapshot();
    });
});

function shallowRenderComponent({ initialState, props }: { props: OwnProps & AbstractOwnProps; initialState: Partial<ModelState> }) {
    const store = mockStore(initialState);

    const page = shallow(
        <Provider store={ store }>
            <VinReportWidget { ...props }/>
        </Provider>,
    );

    return {
        page: page.dive().dive(),
        store: store,
    };
}
