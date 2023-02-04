import got from 'got';
import {appPort} from './constants';

const testRequest = got.extend({
    throwHttpErrors: false,
    responseType: 'json',
    retry: 0,
    hooks: {
        init: [
            (options) => {
                options.prefixUrl = `http://localhost:${appPort}`;
            }
        ]
    }
});

export {testRequest};
