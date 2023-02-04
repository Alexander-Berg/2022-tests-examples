jest.mock('./stat_api');
jest.mock('./metrika');

import _ from 'lodash';

import { VasLogger, VasEvent, VasService } from './vas_logger';
import { BillingFrom } from './billing';
import * as stat_api from './stat_api';
import * as metrika from './metrika';

const log_event = stat_api.log as jest.MockedFunction<typeof stat_api.log>;

let event_log_params: Record<string, any>;
const event_log_promise = Promise.resolve();
log_event.mockImplementation((params: Record<string, any>) => {
    event_log_params = params;
    return event_log_promise;
});

const send_page_event = metrika.send_page_event as jest.MockedFunction<typeof metrika.send_page_event>;

let metrika_params: Array<string>;
send_page_event.mockImplementation((counter: string, params: Array<string>) => {
    metrika_params = params;
});

let params: Record<string, any>;
const DEFAULT_PARAMS = {
    event: VasEvent.show,
    service: VasService.reports,
    price: 999,
    original_price: 1499,
    offer_id: '12345678-abcdef',
    from: BillingFrom.desktop_chat_widget,
    category: 'cars',
};
const COUNTER_ID = 'counter-id';

beforeEach(() => {
    params = _.cloneDeep(DEFAULT_PARAMS);

    send_page_event.mockClear();
    log_event.mockClear();
});

it('правильно формирует параметры для логов', () => {
    const logger = new VasLogger(COUNTER_ID);
    logger.log_vas_event(params);

    expect(send_page_event).toHaveBeenCalledTimes(1);
    expect(log_event).toHaveBeenCalledTimes(1);

    expect(metrika_params).toMatchSnapshot();
    expect(event_log_params).toMatchSnapshot();
});

describe('умеет запоминать события показов', () => {
    let logger: any;

    beforeEach(() => {
        logger = new VasLogger(COUNTER_ID);
    });

    it('для "объявы + место + сервис" не будет логировать повторно', async() => {
        logger.log_vas_event(params);

        return event_log_promise
            .then(() => {
                logger.log_vas_event(params);
                logger.log_vas_event(params);
                logger.log_vas_event(params);
            })
            .then(() => {
                expect(send_page_event).toHaveBeenCalledTimes(1);
                expect(log_event).toHaveBeenCalledTimes(1);
            });
    });

    it('для "объявы + место" сделает два лога', () => {
        logger.log_vas_event(params);
        return event_log_promise
            .then(() => {
                params.from = 'card-vas';
                logger.log_vas_event(params);
            })
            .then(() => {
                expect(send_page_event).toHaveBeenCalledTimes(2);
                expect(log_event).toHaveBeenCalledTimes(2);
            });
    });

    it('для "сервис + место" сделает два лога', () => {
        logger.log_vas_event(params);

        return event_log_promise
            .then(() => {
                params.offer_id = 'fooooooo-baaar';
                logger.log_vas_event(params);
            })
            .then(() => {
                expect(send_page_event).toHaveBeenCalledTimes(2);
                expect(log_event).toHaveBeenCalledTimes(2);
            });
    });

    it('для фронт лога ориентируется на from а для метрики на place', () => {
        params.serviceId = 'offers-history-reports';
        params.from = 'offers-history-reports_button';
        logger.log_vas_event(params);

        return event_log_promise
            .then(() => {
                params.from = 'offers-history-reports_area';
                logger.log_vas_event(params);
            })
            .then(() => {
                expect(send_page_event).toHaveBeenCalledTimes(1);
                expect(log_event).toHaveBeenCalledTimes(2);
            });
    });

    it('для других событий ничего не запоминает', () => {
        params.event = VasEvent.click;
        logger.log_vas_event(params);
        return event_log_promise
            .then(() => {
                logger.log_vas_event(params);
            })
            .then(() => {
                expect(send_page_event).toHaveBeenCalledTimes(2);
                expect(log_event).toHaveBeenCalledTimes(2);
            });
    });
});

it('правильно отправит метрику для мобилки', () => {
    const logger = new VasLogger(COUNTER_ID);
    params.is_mobile = true;
    logger.log_vas_event(params);

    expect(metrika_params).toMatchSnapshot();
});

it('умеет запоминать дефолтные параметры и переопределять их', () => {
    const logger = new VasLogger(COUNTER_ID, {
        from: BillingFrom.desktop_chat_widget,
        category: 'moto',
    });
    delete params.from;
    logger.log_vas_event(params);

    expect(event_log_params).toMatchSnapshot();
    expect(metrika_params).toMatchSnapshot();
});

describe('для события отмены', () => {
    it('если есть код ошибки отправит метрику с ним', () => {
        const logger = new VasLogger(COUNTER_ID);
        params.error_code = 'my_error';
        params.event = VasEvent.error;
        logger.log_vas_event(params);

        expect(metrika_params).toMatchSnapshot();
    });

    it('если нет кода ошибки отправит метрику с NA', () => {
        const logger = new VasLogger(COUNTER_ID);
        params.event = VasEvent.error;
        logger.log_vas_event(params);

        expect(metrika_params).toMatchSnapshot();
    });
});
