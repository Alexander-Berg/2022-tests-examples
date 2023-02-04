declare global {
    // eslint-disable-next-line @typescript-eslint/no-namespace
    namespace NodeJS {
        interface Global {
            location: Location;
        }
    }
}

jest.mock('../lib/local_storage');
jest.mock('../lib/request');

import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import _ from 'lodash';
import MockDate from 'mockdate';

import type { ModelPaymentModal, ModelState } from '../models';
import { ModelChatType, ModelClientType } from '../models';
import * as ls from '../lib/local_storage';
import request from '../lib/request';
import { DAY } from '../lib/consts';
import chat_mock from '../../mocks/state/chat.mock';
import config_mock from '../../mocks/state/config.mock';
import message_mock from '../../mocks/state/message.mock';
import presets_mock from '../../mocks/state/presets.mock';
import user_mock from '../../mocks/state/user.mock';

import ChatMessages from './ChatMessages';
import type { OwnProps } from './ChatMessages';

const get_ls_item = ls.get_item as jest.MockedFunction<typeof ls.get_item>;

const ajax_request = request as jest.MockedFunction<typeof request>;

const mockStore = configureStore([ thunk ]);

let props: OwnProps;
let initialState: Partial<ModelState>;

const { location } = global;

beforeEach(() => {
    props = {
        chat: chat_mock.value(),
        padding_bottom: 0,
    };
    initialState = {
        config: config_mock.value(),
        user: user_mock.value(),
        visible: true,
        bunker: {
            'common/chat_preset_messages': presets_mock.value(),
        },
        payment_modal: {},
    };

    MockDate.set('2020-05-20');

    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.location;
    global.location = {
        assign: jest.fn(),
    } as unknown as Location;
});

afterEach(() => {
    MockDate.reset();

    global.location = location;
});

describe('виджет "похожие объявления"', () => {
    it('покажется, если в сообщении продавца есть ключевое слово', () => {
        props.chat = chat_mock
            .withIsOwner(false)
            .withMessages([
                message_mock.withId('0').withIsMe(false).withText('привет').value(),
                message_mock.withId('1').withIsMe(false).withText('уже продал').value(),
            ])
            .withSubject({ seller_type: 'PRIVATE' })
            .withOrigin(ModelClientType.AUTORU)
            .value();

        const { page } = shallowRenderComponent({ props, initialState });
        const widget = page.find('Connect(SameOffersWidget)');

        expect(widget.isEmptyRender()).toBe(false);
        expect(widget.prop('show_reason')).toBe('prodal');
    });

    it('покажется, если объявление снято с продажи', () => {
        props.chat = chat_mock
            .withIsOwner(false)
            .withMessages([
                message_mock.withId('0').withIsMe(false).withText('привет').value(),
            ])
            .withSubject({ status: 'INACTIVE', seller_type: 'PRIVATE' })
            .withOrigin(ModelClientType.AUTORU)
            .value();

        const { page } = shallowRenderComponent({ props, initialState });
        const widget = page.find('Connect(SameOffersWidget)');

        expect(widget.isEmptyRender()).toBe(false);
        expect(widget.prop('show_reason')).toBe('status');
    });

    describe('не покажется', () => {
        it('в чате продавца', () => {
            props.chat = chat_mock
                .withIsOwner(true)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withText('уже продал').value(),
                ])
                .withSubject({ seller_type: 'PRIVATE' })
                .withOrigin(ModelClientType.AUTORU)
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(SameOffersWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('в чате с тех поддежкой', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withChatType(ModelChatType.ROOM_TYPE_TECH_SUPPORT)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withText('уже продал').value(),
                ])
                .withSubject({ seller_type: 'PRIVATE' })
                .withOrigin(ModelClientType.AUTORU)
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(SameOffersWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('если пользователь - дилер', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withText('уже продал').value(),
                ])
                .withSubject({ seller_type: 'PRIVATE' })
                .withOrigin(ModelClientType.AUTORU)
                .value();
            initialState.user = user_mock.withIsDealer(true).value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(SameOffersWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('если объявление подано дилером', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withText('уже продал').value(),
                ])
                .withSubject({ seller_type: 'COMMERCIAL' })
                .withOrigin(ModelClientType.AUTORU)
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(SameOffersWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('если сообщение от меня', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withIsMe(true).withText('привет').value(),
                    message_mock.withId('1').withIsMe(true).withText('уже продал?').value(),
                ])
                .withSubject({ seller_type: 'PRIVATE' })
                .withOrigin(ModelClientType.AUTORU)
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(SameOffersWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('если был скрыт до этого', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withRoomId('room-0').withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withRoomId('room-0').withId('1').withIsMe(false).withText('уже продал?').value(),
                ])
                .withSubject({ seller_type: 'PRIVATE' })
                .withOrigin(ModelClientType.AUTORU)
                .value();

            // это для виджета про мошенника
            get_ls_item.mockReturnValueOnce(JSON.stringify([ { key: 'room-0__id-1', ts: Date.now() - DAY, is_hidden: true } ]));
            // это для виджета про похожие тачки
            get_ls_item.mockReturnValueOnce(JSON.stringify([ { key: 'room-0', ts: Date.now() - DAY, is_hidden: true } ]));

            const { page } = shallowRenderComponent({ props, initialState });

            const widget = page.find('Connect(SameOffersWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });
    });
});

describe('виджет "мошенник в чате"', () => {
    describe('добавится', () => {
        it('если в сообщении продавца есть все ключевые слова', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withText('переведи деньги на телефон').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(MaliciousUserWarningWidget)');

            expect(widget.isEmptyRender()).toBe(false);
            expect(widget.prop('show_reason')).toBe('perevedi_dengi');
        });

        it('только один', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withText('переведи деньги на телефон').value(),
                    message_mock.withId('2').withIsMe(false).withText('МНЕ НУЖНА ПРЕДОПЛАТА!!!').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(MaliciousUserWarningWidget)');

            expect(widget).toHaveLength(1);
        });
    });

    describe('не покажется', () => {
        it('в чате продавца', () => {
            props.chat = chat_mock
                .withIsOwner(true)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withText('переведи деньги на телефон').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(MaliciousUserWarningWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('в чате с тех поддежкой', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withChatType(ModelChatType.ROOM_TYPE_TECH_SUPPORT)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withText('переведи деньги на телефон').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(MaliciousUserWarningWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('в чате у дилера', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withText('переведи деньги на телефон').value(),
                ])
                .value();
            initialState.user = user_mock.withIsDealer(true).value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(MaliciousUserWarningWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('если тип сообщения не текст', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withIsMe(false).withText('привет').value(),
                    message_mock.withId('1').withIsMe(false).withHtml('<a href="http://sber.ru">переведи деньги на телефон</a>').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(MaliciousUserWarningWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('если сообщение от меня', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withIsMe(true).withText('привет').value(),
                    message_mock.withId('1').withIsMe(true).withText('переведи деньги на телефон').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(MaliciousUserWarningWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('если был скрыт до этого', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withRoomId('room-0').withId('id-0').withIsMe(false).withText('привет').value(),
                    message_mock.withRoomId('room-0').withId('id-1').withIsMe(false).withText('переведи деньги на телефон').value(),
                ])
                .value();
            // это для виджета про мошенника
            get_ls_item.mockReturnValueOnce(JSON.stringify([ { key: 'room-0__id-1', ts: Date.now() - DAY, is_hidden: true } ]));

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(MaliciousUserWarningWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });
    });

});

describe('виджет "отчет по вину"', () => {
    describe('добавится', () => {
        it('если в сообщении пользователя есть ключевое слово', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withText('привет').value(),
                    message_mock.withId('1').withText('какое состояние у тачки?').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(VinReportWidget)');

            expect(widget.isEmptyRender()).toBe(false);
            expect(widget.prop('show_reason')).toBe('sostoyanie');
        });

        it('если в сообщении от владельца есть что-то похожее на вин/госномер', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withText('привет').value(),
                    message_mock.withId('1').withText('скинь идентификатор').value(),
                    message_mock.withId('2').withText('вотъ Z8TND5FS9DM047548').withIsMe(false).value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(VinReportWidget)');

            expect(widget.isEmptyRender()).toBe(false);
            expect(widget.prop('show_reason')).toBe('vin_seller');
        });

        it('если в чате больше 10 сообщений', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages(_.fill(Array(11), message_mock.withText('привет').value()))
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(VinReportWidget)');

            expect(widget.isEmptyRender()).toBe(false);
            expect(widget.prop('show_reason')).toBe('10-messages');
        });

        it('только один у последнего подходящего сообщения', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withText('привет').value(),
                    message_mock.withId('1').withText('битая?').value(),
                    message_mock.withId('2').withText('какое состояние у тачки?').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });

            const widget = page.find('Connect(VinReportWidget)');

            expect(widget).toHaveLength(1);
            expect(widget.props()).toHaveProperty('message_id', '2');
        });

        it('если в чате больше 10 сообщений и есть сообщение с подходящим тесктом, покажет один виджет', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    ..._.fill(Array(11), message_mock.withText('привет').value()),
                    message_mock.withId('123').withText('какое состояние у тачки?').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(VinReportWidget)');

            expect(widget.isEmptyRender()).toBe(false);
            expect(widget).toHaveLength(1);
        });
    });

    describe('не покажется', () => {
        it('в чате продавца', () => {
            props.chat = chat_mock
                .withIsOwner(true)
                .withMessages([
                    message_mock.withId('0').withText('привет').value(),
                    message_mock.withId('1').withText('какое состояние у тачки?').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(VinReportWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('в чате с тех поддежкой', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withChatType(ModelChatType.ROOM_TYPE_TECH_SUPPORT)
                .withMessages([
                    message_mock.withId('0').withText('привет').value(),
                    message_mock.withId('1').withText('какое состояние у тачки?').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(VinReportWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });

        it('если тип сообщения не текст', () => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withText('привет').value(),
                    message_mock.withId('1').withHtml('<a href="http://autoteka.ru">какое состояние у тачки?</a>').value(),
                ])
                .value();

            const { page } = shallowRenderComponent({ props, initialState });
            const widget = page.find('Connect(VinReportWidget)');

            expect(widget.isEmptyRender()).toBe(true);
        });
    });

    it('при маунте возьмет статус закрытия виджета из local storage', () => {
        props.chat = chat_mock
            .withIsOwner(false)
            .withMessages([
                message_mock.withId('0').withText('привет').value(),
                message_mock.withId('1').withText('какое состояние у тачки?').value(),
            ])
            .value();
        get_ls_item.mockReturnValueOnce(JSON.stringify([ { key: '1099744590-d19f2739', ts: Date.now() - DAY, is_hidden: true } ]));

        const { page } = shallowRenderComponent({ props, initialState });
        const widget = page.find('Connect(VinReportWidget)');

        expect(widget.prop('info')).toMatchSnapshot();
    });

    describe('при успешной оплате', () => {
        const fetch_vin_report_promise = Promise.resolve({});
        const get_vin_page_url_promise = Promise.resolve({ urls: [ { url: 'url-to-vin-report-page' } ] });

        beforeEach(() => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withText('привет').value(),
                    message_mock.withId('1').withText('какое состояние у тачки?').value(),
                ])
                .value();
            initialState.payment_modal && (initialState.payment_modal = {
                status: 'PAID',
                params: {
                    offerId: '1099744590-d19f2739',
                } as ModelPaymentModal['params'],
            });
            ajax_request.mockReturnValueOnce(fetch_vin_report_promise);
            ajax_request.mockReturnValueOnce(get_vin_page_url_promise);
        });

        it('передаст статус в виджет', () => {
            const { page } = shallowRenderComponent({ props, initialState });
            simulatePaymentStatusChange(page, 'OPENED');
            const widget = page.find('Connect(VinReportWidget)');

            expect(widget.prop('info')).toMatchSnapshot();
        });

        it('применит квоту', () => {
            const { page } = shallowRenderComponent({ props, initialState });
            simulatePaymentStatusChange(page, 'OPENED');

            expect(ajax_request).toHaveBeenCalledTimes(1);
            expect(ajax_request.mock.calls[0]).toMatchSnapshot();
        });

        it('сделает редирект', () => {
            const { page } = shallowRenderComponent({ props, initialState });
            simulatePaymentStatusChange(page, 'OPENED');

            return get_vin_page_url_promise
                .then(() => {
                    expect(global.location.assign).toHaveBeenCalledTimes(1);
                    expect(global.location.assign).toHaveBeenCalledWith('https://autoru_frontend.base_domain/history/1099744590-d19f2739/');
                });
        });
    });

});

describe('запрос статуса вин репорта:', () => {
    it('не будет запрашивать данные, если виджета нет', () => {
        const { page } = shallowRenderComponent({ props, initialState });
        simulateLastChatMessageChange(page);

        expect(ajax_request).toHaveBeenCalledTimes(0);
    });

    describe('если виджет есть', () => {
        beforeEach(() => {
            props.chat = chat_mock
                .withIsOwner(false)
                .withMessages([
                    message_mock.withId('0').withText('привет').value(),
                    message_mock.withId('1').withText('какое состояние у тачки?').value(),
                ])
                .value();
        });

        it('запросит данные', () => {
            ajax_request.mockReturnValue(Promise.resolve({}));
            const { page } = shallowRenderComponent({ props, initialState });
            simulateLastChatMessageChange(page);

            expect(ajax_request).toHaveBeenCalledTimes(1);
            expect(ajax_request.mock.calls[0]).toMatchSnapshot();
        });

        it('не будет запрашивать данные второй раз', () => {
            ajax_request.mockReturnValue(Promise.resolve({}));
            const { page } = shallowRenderComponent({ props, initialState });
            simulateLastChatMessageChange(page);
            simulateLastChatMessageChange(page);

            expect(ajax_request).toHaveBeenCalledTimes(1);
        });

        it('при удачном ответе передаст данные в виджет', () => {
            const carfax_get_offer_report_promise = Promise.resolve({
                report: {
                    report_type: 'FREE_REPORT',
                    photo_block: {
                        photos: [
                            { sizes: { '456x342': 'car-photo.jpeg' } },
                        ],
                    },
                    content: {
                        items: [
                            { type: 'DTP', record_count: 3 },
                            { type: 'AUTORU_OFFERS', record_count: 5 },
                            { type: 'TAXI' },
                            { type: 'CAR_SHARING' },
                        ],
                    },
                },
                billing: {
                    service_prices: [
                        { price: 777 },
                    ],
                },
            });
            ajax_request.mockReturnValue(carfax_get_offer_report_promise);
            const { page } = shallowRenderComponent({ props, initialState });
            simulateLastChatMessageChange(page);

            return carfax_get_offer_report_promise
                .then(() => {
                    const widget = page.find('Connect(VinReportWidget)');
                    expect(widget.prop('info')).toMatchSnapshot();
                });
        });

        it('при удачном ответе если отчет уже куплен сообщит об этом виджету', () => {
            const carfax_get_offer_report_promise = Promise.resolve({
                report: {
                    report_type: 'FREE_REPORT',
                },
                billing: undefined,
            });
            ajax_request.mockReturnValue(carfax_get_offer_report_promise);
            const { page } = shallowRenderComponent({ props, initialState });
            simulateLastChatMessageChange(page);

            return carfax_get_offer_report_promise
                .then(() => {
                    const widget = page.find('Connect(VinReportWidget)');
                    expect(widget.prop('info')).toMatchSnapshot();
                });
        });

        it('при ошибке сообщит об этом виджету', () => {
            const carfax_get_offer_report_promise = Promise.reject();
            ajax_request.mockReturnValue(carfax_get_offer_report_promise);
            const { page } = shallowRenderComponent({ props, initialState });
            simulateLastChatMessageChange(page);

            return carfax_get_offer_report_promise
                .catch(() => {})
                .then(() => {
                    const widget = page.find('Connect(VinReportWidget)');
                    expect(widget.prop('info')).toMatchSnapshot();
                });
        });

        describe('при покупке с квотой', () => {
            const get_vin_page_url_promise = Promise.resolve({ urls: [ { url: 'url-to-vin-report-page' } ] });

            beforeEach(() => {
                ajax_request.mockReturnValueOnce(Promise.resolve({}));
                ajax_request.mockReturnValueOnce(Promise.resolve(get_vin_page_url_promise));
                const { page } = shallowRenderComponent({ props, initialState });
                const widget = page.find('Connect(VinReportWidget)');
                const cb = widget.prop('on_buy_report_with_quota') as (has_quote: boolean) => void;
                cb(true);
            });

            it('запросит отчет с правильными данными', () => {
                expect(ajax_request).toHaveBeenCalledTimes(1);
                expect(ajax_request.mock.calls[0]).toMatchSnapshot();
            });

            it('сделает редирект при успехе', () => {
                return get_vin_page_url_promise
                    .then(() => {
                        expect(global.location.assign).toHaveBeenCalledTimes(1);
                        expect(global.location.assign).toHaveBeenCalledWith('https://autoru_frontend.base_domain/history/1099744590-d19f2739/');
                    });
            });
        });

    });

});

describe('пресеты', () => {
    it('покажет пресеты в чате частника', () => {
        props.chat = chat_mock
            .withOrigin(ModelClientType.AUTORU)
            .value();

        const { page } = shallowRenderComponent({ props, initialState });
        const presets = page.find('.ChatMessages__presets');

        expect(presets.isEmptyRender()).toBe(false);
    });

    it('не покажет пресеты в чате дилера', () => {
        props.chat = chat_mock.withSubject({ seller_type: 'COMMERCIAL' }).value();
        const { page } = shallowRenderComponent({ props, initialState });
        const presets = page.find('.ChatMessages__presets');

        expect(presets.isEmptyRender()).toBe(true);
    });

    it('не покажет пресеты, если я уже написал 2 сообщения', () => {
        props.chat = chat_mock
            .withMessages([
                message_mock.withId('0').withIsMe(true).withText('привет').value(),
                message_mock.withId('1').withIsMe(false).withText('привет').value(),
                message_mock.withId('2').withIsMe(true).withText('продаешь еще?').value(),
            ])
            .value();

        const { page } = shallowRenderComponent({ props, initialState });
        const presets = page.find('.ChatMessages__presets');

        expect(presets.isEmptyRender()).toBe(true);
    });
});

describe('сообщения про время ответа продавца', () => {
    it('если отвечает редко, покажет текст и кнопку "позвонить"', () => {
        props.chat = chat_mock
            .withOrigin(ModelClientType.AUTORU)
            .withUsers([
                { id: '01', is_me: true },
                { id: '02', is_me: false, average_reply_delay_minutes: 24 * 60 + 1 },
            ])
            .withMessages([])
            .withChatOnly(false)
            .withMe('01')
            .value();

        const { page } = shallowRenderComponent({ props, initialState });
        const text = page.find('.ChatMessages__no-content-text');
        const button = page.find('.ChatMessages__no-content').find('Button');

        expect(text.text()).toBe('Продавец отвечает редко, лучше позвонить');
        expect(button.dive().text()).toBe('Позвонить');
    });

    it('если отвечает редко и закрыл звонки, покажет обычный текст', () => {
        props.chat = chat_mock
            .withOrigin(ModelClientType.AUTORU)
            .withUsers([
                { id: '01', is_me: true },
                { id: '02', is_me: false, average_reply_delay_minutes: 24 * 60 + 1 },
            ])
            .withMessages([])
            .withChatOnly(true)
            .withMe('01')
            .value();

        const { page } = shallowRenderComponent({ props, initialState });
        const text = page.find('.ChatMessages__no-content-text');
        const button = page.find('.ChatMessages__no-content').find('Button');

        expect(text.text()).toBe('В этом чате еще нет сообщений.');
        expect(button.isEmptyRender()).toBe(true);
    });
});

function simulateLastChatMessageChange(page: ShallowWrapper) {
    const instance = page.instance();
    if (instance && typeof instance.componentDidUpdate === 'function') {
        instance.componentDidUpdate({
            ...props,
            chat: {
                ...props.chat,
                messages: [
                    ...props.chat.messages,
                    message_mock.withId('0').withText('привет').value(),
                ],
            },
            payment_modal: {},
        }, {});
    }

    return { page };
}

function simulatePaymentStatusChange(page: ShallowWrapper, prev_status: string) {
    const instance = page.instance();
    if (instance && typeof instance.componentDidUpdate === 'function') {
        instance.componentDidUpdate({
            ...props,
            payment_modal: {
                status: prev_status,
            },
        }, {});
    }

    return { page };
}

function shallowRenderComponent({ initialState, props }: { props: OwnProps; initialState: Partial<ModelState> }) {
    const store = mockStore(initialState);

    const wrapper = shallow(
        <Provider store={ store }>
            <ChatMessages { ...props }/>
        </Provider>,
    );

    return {
        wrapper: wrapper,
        page: wrapper.dive().dive(),
        store: store,
    };
}
