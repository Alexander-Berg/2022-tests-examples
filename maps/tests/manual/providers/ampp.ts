import {URL, URLSearchParams} from 'url';
import * as crypto from 'crypto';
import got from 'got';
import * as xml from 'xml-js';
import * as convert from 'xml-js';
import {loadSecret} from '../../../app/lib/secrets';

const AMPP_PARTNER = loadSecret('amppPartner');
const AMPP_SECRET = loadSecret('amppSecret');
const PARKING_URL = 'https://lk.parkingtest.ru/paymentApi/api/1.0/'; // pre-production

type RequestParams = {
    subscriber: string;
    amount?: string;
    paymentId?: string;
    place?: string;
    carId?: string;
    duration?: string;
};

type AmppApiCommonResponse = {
    parking: {
        sessionId: { _text: string };
        placeId: { _text: string };
        startTime: { _text: string };
        stopTime: { _text: string };
        carId: { _text: string };
    };
};

type AmppApiBalance = {
    funds: { _text: string };
    payment?: {
        paymentId: { _text: string };
        subscriber: { _text: string };
        amount: { _text: string };
        date: { _text: string };
    }
};

type AmppUserInfo = {
    funds: { _text: string };
    person: {
        carsList: {
            car: { _text: string };
        };
    };
};

class AmppParkingProvider {
    private readonly _url: string;

    constructor(url: string) {
        this._url = url;
    }

    async getAccountInfo(subscriber: string) {
        return this._sendRequest<AmppUserInfo>('account', {subscriber}, 'info');
    }

    async getAccountBalance(subscriber: string): Promise<number> {
        const result = await this._sendRequest<AmppApiBalance>('account', {subscriber}, 'balance');
        return parseFloat(result.funds._text);
    }

    async topUpBalance(subscriber: string, amount: string, paymentId: string) {
        const result = await this._sendRequest<AmppApiBalance>(
            'payment',
            {subscriber, amount, paymentId}
        );
        return {
            funds: parseFloat(result.funds._text),
            payment: {
                paymentId: result.payment!.paymentId._text,
                subscriber: result.payment!.subscriber._text,
                amount: result.payment!.amount._text,
                date: result.payment!.date._text
            }
        };
    }

    async startParkingSession(subscriber: string, place: string, carId: string, duration: string) {
        const result: AmppApiCommonResponse = await this._sendRequest<AmppApiCommonResponse>(
            'parking',
            {subscriber, place, carId, duration},
            'start'
        );
        return this._formatParkingSession(result);
    }

    async stopParkingSession(subscriber: string, carId: string) {
        const result = await this._sendRequest<AmppApiCommonResponse>('parking', {subscriber, carId}, 'stop');
        return this._formatParkingSession(result);
    }

    async extendParkingSession(subscriber: string, carId: string, duration: string) {
        const result = await this._sendRequest<AmppApiCommonResponse>(
            'parking',
            {subscriber, carId, duration},
            'extend'
        );
        return this._formatParkingSession(result);
    }

    async getCurrentParkingSession(subscriber: string) {
        const result = await this._sendRequest<AmppApiCommonResponse>('parking', {subscriber}, 'check');
        return result.parking && this._formatParkingSession(result);
    }

    _formatParkingSession(result: AmppApiCommonResponse) {
        return {
            parking: {
                sessionId: result.parking.sessionId._text,
                placeId: result.parking.placeId._text,
                startTime: result.parking.startTime._text,
                stopTime: result.parking.stopTime._text,
                carId: result.parking.carId._text
            }
        };
    }

    async _sendRequest<T>(
        handler: string,
        params: RequestParams,
        action?: string
    ): Promise<T> {
        const url = new URL(this._url);
        url.pathname = url.pathname + handler;

        const searchParams = new URLSearchParams(params);

        if (action) {
            searchParams.set('action', action);
        }

        searchParams.set('partner', AMPP_PARTNER);
        searchParams.set('secret', AMPP_SECRET);
        searchParams.sort();

        const shasum = crypto.createHash('sha1');
        shasum.update(searchParams.toString());
        searchParams.set('hash', shasum.digest('hex'));
        searchParams.delete('secret');

        // Query parameters order is of the utmost importance
        // https://st.yandex-team.ru/MAPSHTTPAPI-2612
        const subscriber = searchParams.get('subscriber');
        if (subscriber) {
            searchParams.delete('subscriber');
            searchParams.set('subscriber', subscriber.toString());
        }

        url.search = searchParams.toString();

        let responseJson: xml.ElementCompact & {error?: xml.ElementCompact | xml.ElementCompact[]};
        let hasErrors;
        for (let i = 0; i < 3; i++) {
            const response = await got(url);
            // tslint:disable-next-line:no-console
            console.log(`${url} -> ${response.body}`);
            responseJson = convert.xml2js(response.body, {compact: true});
            hasErrors = parseInt(responseJson.response._attributes.errors, 10);
            const errorCode = hasErrors && responseJson.response.error._attributes.id;

            if (errorCode !== 'err_invalid_partner') {
                break;
            }
            // tslint:disable-next-line:no-console
            console.log(`Retry # ${i + 1} because of ${errorCode}`);
        }

        if (hasErrors) {
            const error = responseJson!.response.error;
            throw new Error(`AMPP error=${error._attributes.id}: ${error._text}`);
        }

        return responseJson!.response;
    }
}

export const amppProvider = new AmppParkingProvider(PARKING_URL);
