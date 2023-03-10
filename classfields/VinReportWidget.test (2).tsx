import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import MockDate from 'mockdate';

import VinReportWidget, { OwnProps } from './VinReportWidget';
import { OwnProps as AbstractOwnProps } from '../Widget';
import { ModelState, ModelChatImplementation } from '../../../models';
import { VasService } from '../../../lib/vas_logger';
import configMock from '../../../../mocks/state/config.mock';
import * as ls from '../../../lib/local_storage';

jest.mock('../../../lib/local_storage');
const get_ls_item = ls.get_item as jest.MockedFunction<typeof ls.get_item>;

jest.mock('../../../lib/metrika');
import * as metrika from '../../../lib/metrika';
const sendMetrikaPageEvent = metrika.send_page_event as jest.MockedFunction<typeof metrika.send_page_event>;

const logVasEventMock = jest.fn();
jest.mock('../../../lib/vas_logger', () => {
    return {
        VasLogger: jest.fn().mockImplementation(() => {
            return {
                log_vas_event: logVasEventMock,
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
import * as vas_logger from '../../../lib/vas_logger';
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

describe('??????????', () => {
    describe('?????????????????? ???? ??????????', () => {
        it('???????? ?????? info', () => {
            props.info = undefined;
            const { page } = shallowRenderComponent({ props, initialState });
            expect(page.isEmptyRender()).toBe(true);
        });

        it('???????? ???????????? fetching', () => {
            props.info && (props.info.status = 'FETCHING');
            const { page } = shallowRenderComponent({ props, initialState });
            expect(page.isEmptyRender()).toBe(true);
        });

        it('???????? ?????? ????????????', () => {
            props.info && (props.info.payment_data = undefined);
            const { page } = shallowRenderComponent({ props, initialState });
            expect(page.isEmptyRender()).toBe(true);
        });

        it('???????? ?????? ?????????? ?????? ?????????? ??????????????????', () => {
            get_ls_item.mockReturnValueOnce(JSON.stringify([ { key: 'offer-id_123', is_hidden: true } ]));
            const { page } = shallowRenderComponent({ props, initialState });
            expect(page.isEmptyRender()).toBe(true);
        });
    });

    describe('?????????????????? ??????????', () => {
        let page: any;

        beforeEach(() => {
            page = shallowRenderComponent({ props, initialState }).page;
        });

        it('???????????????? ??????????????????', () => {
            expect(page.isEmptyRender()).toBe(false);
        });

        it('???????????????????????????? ????????????', () => {
            expect(vas_logger.VasLogger).toHaveBeenCalledTimes(1);
            expect(vas_logger.VasLogger).toHaveBeenCalledWith(
                'metrika-id',
                { category: 'cars', from: 'desktop_chat_widget', is_mobile: false, offer_id: 'offer-id', service: 'reports' },
            );
        });

        it('???????????????? ?????????????? ?? ?????? ??????', () => {
            expect(logVasEventMock).toHaveBeenCalledTimes(1);
            expect(logVasEventMock.mock.calls[0]).toMatchSnapshot();

            expect(sendMetrikaPageEvent).toHaveBeenCalledTimes(1);
            expect(sendMetrikaPageEvent.mock.calls[0]).toMatchSnapshot();
        });
    });
});

describe('???????? ???? ???????????? "????????????"', () => {
    beforeEach(() => {
        logVasEventMock.mockClear();
        sendMetrikaPageEvent.mockClear();
    });

    it('?????????????? ?????????? ???????????? ???????? ?????? ??????????', () => {
        const { page, store } = shallowRenderComponent({ props, initialState });
        const button = page.find('.VinReportWidget__button');
        button.simulate('click');

        expect(store.getActions()).toMatchSnapshot();
    });

    it('???????????????? ?????????? ???????? ???????? ??????????', () => {
        props.info && (props.info.quota_left = 3);
        const { page } = shallowRenderComponent({ props, initialState });
        const button = page.find('.VinReportWidget__button');
        button.simulate('click');

        expect(props.on_buy_report_with_quota).toHaveBeenCalledTimes(1);
        expect(props.on_buy_report_with_quota).toHaveBeenCalledWith(true);
    });

    it('???????????????? ?????? ??????', () => {
        const { page } = shallowRenderComponent({ props, initialState });
        const button = page.find('.VinReportWidget__button');
        button.simulate('click');

        expect(logVasEventMock).toHaveBeenCalledTimes(2);
        expect(logVasEventMock.mock.calls[1]).toMatchSnapshot();
    });

    it('???????????????? ??????????????', () => {
        const { page } = shallowRenderComponent({ props, initialState });
        const button = page.find('.VinReportWidget__button');
        button.simulate('click');

        expect(sendMetrikaPageEvent).toHaveBeenCalledTimes(2);
        expect(sendMetrikaPageEvent.mock.calls[1]).toMatchSnapshot();
    });
});

function shallowRenderComponent({ props, initialState }: { props: OwnProps & AbstractOwnProps; initialState: Partial<ModelState> }) {
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
