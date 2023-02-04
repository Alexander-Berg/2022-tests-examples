import {DateTime} from 'luxon';
import nock from 'nock';
import {URL} from 'url';
import {config} from '../../app/config';

const BLACKBOX_URL = new URL(config['blackbox.url']);
const AMPP_URL = new URL(config['providers.mos.payment.baseUrl']);
const AMPP_DATETIME_FORMAT = 'yyyy-MM-dd HH:mm:ss';

export function nockBlackboxUrl(response: Record<string, any>) {
    return nock(BLACKBOX_URL.origin)
        .get(BLACKBOX_URL.pathname)
        .query(true)
        .times(1)
        .reply(200, response);
}

export function nockBalanceUrl(code: number, balance?: number) {
    const body = code !== 200 ? undefined :
        `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <response errors="0">
            <funds>${balance}</funds>
        </response>`;
    return nock(AMPP_URL.origin)
        .get(`${AMPP_URL.pathname}account`)
        .query(true)
        .times(1)
        .reply(code, body);
}

type AmppSessionData = {
    id: string,
    placeId: string,
    startTime: DateTime,
    stopTime: DateTime,
    carId: string
};

function getParkingResponse(sessionData: AmppSessionData) {
    return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <response errors="0">
            <parking>
                <sessionId>${sessionData.id}</sessionId>
                <placeId>${sessionData.placeId}</placeId>
                <startTime>${sessionData.startTime.toFormat(AMPP_DATETIME_FORMAT)}</startTime>
                <stopTime>${sessionData.stopTime.toFormat(AMPP_DATETIME_FORMAT)}</stopTime>
                <carId>${sessionData.carId}</carId>
            </parking>
        </response>`;
}

export function nockStartSessionUrl(
    code: number,
    sessionData?: AmppSessionData
) {
    const body = !sessionData ? undefined : getParkingResponse(sessionData);

    return nock(AMPP_URL.origin)
        .get(`${AMPP_URL.pathname}parking`)
        .query((queryObj) => queryObj.action === 'start')
        .times(1)
        .reply(code, body);
}

export function nockStopSessionUrl(
    code: number,
    sessionData?: AmppSessionData
) {
    const body = !sessionData ? undefined : getParkingResponse(sessionData);

    return nock(AMPP_URL.origin)
        .get(`${AMPP_URL.pathname}parking`)
        .query((queryObj) => queryObj.action === 'stop')
        .times(1)
        .reply(code, body);
}

export function nockExtendSessionUrl(
    code: number,
    sessionData?: AmppSessionData
) {
    const body = !sessionData ? undefined : getParkingResponse(sessionData);

    return nock(AMPP_URL.origin)
        .get(`${AMPP_URL.pathname}parking`)
        .query((queryObj) => queryObj.action === 'extend')
        .times(1)
        .reply(code, body);
}
