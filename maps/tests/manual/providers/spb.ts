import {loadSecret} from '../../../app/lib/secrets';

const {URL} = require('url');
const got = require('got');

const PARKING_URL = 'https://parking.fitdev.ru/integration/terminal/3.0/qiwi/'; // dev
const ZONES_URL = 'https://parkingqa.fitdev.ru/api/2.6/objects?types=zone';

type ReservationInterval = {
    start: number;
    end: number;
    sum: number;
    savingSum: number;
};
type SpbInitResponse = {
    sum: number;
    savingSum: number;
    transactionId: number;
    tariff: {
        name: string;
    };
    reservationIntervals: ReservationInterval[];
};
type SpbFinishResponse = {
    start: number;
    end: number;
};
type SpbZone = {
    active: boolean;
    number: number;
};
type SpbZoneResponse = {
    objects: SpbZone[];
};

class SpbParkingProvider {
    private readonly _baseUrl: string;
    private readonly _auth: string;

    constructor(url: string) {
        this._baseUrl = url;
        this._auth = Buffer.from(`${loadSecret('spbLogin')}:${loadSecret('spbPassword')}`).toString('base64');
    }

    async initPayment(duration: number) {
        return this._sendRequest<SpbInitResponse>(
            'payment/init',
            {
                zoneNumber: '101',
                vrp: 'a123aa22',
                vrpFormat: 'local',
                vehicleType: 'car',
                duration
            }
        );
    }

    async finishPayment(transactionId: number, paymentId: string, sum: number) {
        const result = await this._sendRequest<SpbFinishResponse>(
            'payment/finish',
            {
                transactionId,
                sum, // in kopecks
                paymentId,
                phone: '79000000020'
            }
        );

        return {
            start: new Date(result.start),
            end: new Date(result.end)
        };
    }

    async getZonesNumbers() {
        const response = await got(ZONES_URL, {responseType: 'json'});

        return (response.body as SpbZoneResponse).objects.map((zone) => zone.number);
    }

    async _sendRequest<T>(path: string, body: {[key: string]: any}): Promise<T> {
        const url = new URL(this._baseUrl);
        url.pathname = url.pathname + path;

        const response = await got(url, {
            headers: {
                Authorization: `Basic ${this._auth}`,
                'Content-Type': 'application/json'
            },
            method: 'post',
            responseType: 'json',
            json: body,
            throwHttpErrors: false
        });

        // tslint:disable-next-line:no-console
        console.log(`${url} -> ${response.statusCode}, ${JSON.stringify(response.body)}`);

        if (response.statusCode !== 200) {
            throw new Error(`Error: code=${response.body.errorName}`);
        }

        return response.body;
    }
}

export const spbProvider = new SpbParkingProvider(PARKING_URL);
