import { QUERY_KEY } from '../constants';
import { getDataFromUrl, setDataToUrl, removeDataFromUrl } from '../helpers';

describe('helpers', () => {
    describe('getDataFromUrl', () => {
        it('should return data from the url', () => {
            const data = String(Date.now());
            const urlWithData = `http://some.yp.local.com/path/a/b/c?${QUERY_KEY}=${data}`;

            const actualData = getDataFromUrl(urlWithData);

            expect(actualData).toBe(data);
        });

        it('should return an empty string if no data is specified', () => {
            const url = 'http://some.yp.local.com/path/a/b/c';

            const actualData = getDataFromUrl(url);

            expect(actualData).toBe('');
        });
    });

    describe('setDataToUrl', () => {
        it('should return the url with the data', () => {
            const data = String(Date.now());
            const url = 'http://some.yp.local.com/path/a/b/c';

            const actualUrl = setDataToUrl(url, data);

            expect(actualUrl).toBe(`${url}?${QUERY_KEY}=${data}`);
        });
    });

    describe('removeDataFromUrl', () => {
        it('should return the url with no data', () => {
            const data = String(Date.now());
            const url = 'http://some.yp.local.com/path/a/b/c';
            const urlWithData = `${url}?${QUERY_KEY}=${data}`;

            const actualUrl = removeDataFromUrl(urlWithData);

            expect(actualUrl).toBe(url);
        });
    });
});
