/* eslint-disable @typescript-eslint/no-non-null-assertion */
import { setClickParamsItem, getClickParams } from '../storageClickParams';

class StorageMock {
    constructor() {
        /* eslint-disable @typescript-eslint/ban-ts-comment */
        //@ts-ignore-next-line
        this.storageMock = {};
    }

    clear() {
        //@ts-ignore-next-line
        this.storageMock = {};
    }

    getItem(key: string) {
        //@ts-ignore-next-line
        return this.storageMock[key] || null;
    }

    setItem(key: string, value: string) {
        //@ts-ignore-next-line
        this.storageMock[key] = String(value);
    }

    removeItem(key: string) {
        //@ts-ignore-next-line
        delete this.storageMock[key];
    }
    /* eslint-enable @typescript-eslint/ban-ts-comment */
}

const getStorageMock = (): Storage => (new StorageMock() as unknown) as Storage;

const getLocationMock = (url: string): Location => (new URL(url) as unknown) as Location;

const defaultLocation = window.location;
const defaultLocalStorage = window.localStorage;
const defaultSessionStorage = window.sessionStorage;

describe('Works correctly', () => {
    beforeAll(() => {
        /* eslint-disable @typescript-eslint/ban-ts-comment */
        // @ts-ignore-next-line
        delete window.location;
        //@ts-ignore-next-line
        delete window.localStorage;
        //@ts-ignore-next-line
        delete window.sessionStorage;
        /* eslint-enable @typescript-eslint/ban-ts-comment */

        window.localStorage = getStorageMock();
        window.sessionStorage = getStorageMock();
    });

    afterAll(() => {
        window.location = defaultLocation;
        window.localStorage = defaultLocalStorage;
        window.sessionStorage = defaultSessionStorage;
    });

    it('add first item to localStorage correctly', () => {
        const targetLink = '/offer/123123123/';
        const clickParams = {
            source: 'serp',
        };

        const expectedData = [
            {
                targetLink,
                clickParams,
                timestamp: expect.any(Number),
            },
        ];

        setClickParamsItem(targetLink, clickParams);
        expect(JSON.parse(localStorage.getItem('clickParams')!)).toMatchObject(expectedData);
    });

    it('add second item to localStorage correctly', () => {
        const targetLink1 = '/offer/123123123/';
        const clickParams1 = {
            source: 'serp',
        };
        const targetLink2 = '/moskva/kupit/novostrojka/zhk-site-123123/';
        const clickParams2 = {
            source: 'map',
        };

        const expectedData = [
            {
                targetLink: targetLink1,
                clickParams: clickParams1,
                timestamp: expect.any(Number),
            },
            {
                targetLink: targetLink2,
                clickParams: clickParams2,
                timestamp: expect.any(Number),
            },
        ];

        setClickParamsItem(targetLink2, clickParams2);
        expect(JSON.parse(localStorage.getItem('clickParams')!)).toMatchObject(expectedData);
    });

    it('get and remove params from storage correctly with same location', () => {
        window.location = getLocationMock('https://realty.yandex.ru/offer/123123123/');

        const expectedClickParams = {
            source: 'serp',
        };
        const targetLink2 = '/moskva/kupit/novostrojka/zhk-site-123123/';
        const clickParams2 = {
            source: 'map',
        };

        const expectedLSData = [
            {
                targetLink: targetLink2,
                clickParams: clickParams2,
                timestamp: expect.any(Number),
            },
        ];

        const expectedSSData = {
            source: 'serp',
        };

        const clickParamsFromStorage = getClickParams();

        expect(clickParamsFromStorage).toEqual(expectedClickParams);
        expect(JSON.parse(localStorage.getItem('clickParams')!)).toMatchObject(expectedLSData);
        expect(JSON.parse(sessionStorage.getItem('clickParams')!)).toEqual(expectedSSData);
    });

    it('get and remove params from storage correctly with different location', () => {
        window.location = getLocationMock(
            'https://realty.yandex.ru/moskva_i_moskovskaya_oblast/kupit/novostrojka/zhk-site-123123/'
        );
        window.sessionStorage = getStorageMock();

        const expectedClickParams = {
            source: 'map',
        };

        const expectedSSData = {
            source: 'map',
        };

        const clickParamsFromStorage = getClickParams(true);

        expect(clickParamsFromStorage).toEqual(expectedClickParams);
        expect(JSON.parse(localStorage.getItem('clickParams')!)).toMatchObject([]);
        expect(JSON.parse(sessionStorage.getItem('clickParams')!)).toEqual(expectedSSData);
    });

    it('clears data with invalid timestamps', () => {
        window.localStorage = getStorageMock();
        window.sessionStorage = getStorageMock();
        window.location = getLocationMock('https://realty.yandex.ru/moskva/kupit/novostrojka/zhk-third-site-333333/');

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        //@ts-ignore-next-line
        jest.useFakeTimers().setSystemTime(new Date('2022-03-24').getTime());

        const oldDate = Date.now();

        const mockLSData = [
            {
                targetLink: '/offer/123123123/',
                clickParams: { source: 'serp' },
                timestamp: oldDate,
            },
            {
                targetLink: '/moskva/kupit/novostrojka/zhk-site-123123/',
                clickParams: { source: 'map' },
                timestamp: oldDate,
            },
        ];

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        //@ts-ignore-next-line
        jest.useFakeTimers().setSystemTime(new Date('2022-03-25').getTime());

        const newDate = Date.now();

        const targetItem = {
            targetLink: '/moskva/kupit/novostrojka/zhk-third-site-333333/',
            clickParams: { source: 'offer_card', from: 'someSource' },
            timestamp: newDate,
        };

        const expectedLSRestItem = {
            targetLink: '/moskva/kupit/novostrojka/zhk-another-site-321321/',
            clickParams: { source: 'offer_card' },
            timestamp: newDate,
        };

        mockLSData.push(expectedLSRestItem, targetItem);

        localStorage.setItem('clickParams', JSON.stringify(mockLSData));

        const clickParamsFromStorage = getClickParams(true);

        expect(clickParamsFromStorage).toEqual(targetItem.clickParams);
        expect(JSON.parse(localStorage.getItem('clickParams')!)).toMatchObject([expectedLSRestItem]);
    });
});
