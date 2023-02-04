import { AppConfig } from './AppConfig';

import createHttpReq from 'mocks/createHttpReq';
import type { Request } from 'express';

let req: Request;
beforeEach(() => {
    req = createHttpReq();
});

describe('version', () => {
    it('должен распарсить версию из заголовка', () => {
        req.headers['x-android-app-version'] = '9.6.1';
        const appConfig = new AppConfig({}, req);

        expect(appConfig.version).toEqual({
            major: 9,
            minor: 6,
            patch: 1,
        });
    });

    it('должен распарсить версию с веткой из заголовка', () => {
        req.headers['x-android-app-version'] = '9.7.1_AUTORUAPPS-16799';
        const appConfig = new AppConfig({}, req);

        expect(appConfig.version).toEqual({
            major: 9,
            minor: 7,
            patch: 1,
        });
    });

    it('должен распарсить версию в 0, если заголовка нет', () => {
        const appConfig = new AppConfig({}, req);

        expect(appConfig.version).toEqual({
            major: 0,
            minor: 0,
            patch: 0,
        });
    });
});
