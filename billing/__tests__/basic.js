import { fetchGet } from '../index';

describe('Basic tests', () => {
    it("Ensure fetch doesn't modify data object", () => {
        const url = 'http://test/test/test';
        const data = { test: 'test' };

        const len = Object.keys(data).length;
        const value = data.test;

        fetchGet(url, data);

        expect(Object.keys(data).length).toEqual(len);
        expect(data.test).toEqual(value);
    });
});
