const {TVM_API, TVM_TICKET_PARSER_PATH} = require('../constants');

jest.mock('../fs');
jest.mock('got');

describe('Ticket parser', () => {
    describe('Getting keys', () => {
        jest.mock(TVM_TICKET_PARSER_PATH, () => ({
            version: 100
        }), {virtual: true});

        const fs = require('../fs');
        const got = require('got');
        const {getKeys} = require('../ticket-parser');

        test('Need download new keys', async () => {
            got.mockResolvedValueOnce({body: 'keys'});

            const keys = await getKeys('file path', true);

            expect(keys).toEqual('keys');
            expect(got).toHaveBeenCalledWith(`${TVM_API}/keys?lib_version=100`, expect.objectContaining({retries: expect.any(Number)}));
            expect(fs.writeFile).toHaveBeenCalledWith('file path', 'keys');
            expect(fs.readFile).not.toHaveBeenCalled();
        });

        test('Need read actual keys from file', async () => {
            fs.readFile.mockReturnValueOnce('keys');

            const keys = await getKeys('file path', false);

            expect(keys).toEqual('keys');
            expect(got).not.toHaveBeenCalled();
            expect(fs.readFile).toHaveBeenCalledWith('file path', 'utf8');
        });

        test('Error on reading file', async () => {
            fs.readFile.mockRejectedValueOnce(new Error('read error'));
            return expect(getKeys('file path', false)).rejects.toThrow('read error');
        });

        test('Error on writing file', async () => {
            got.mockResolvedValueOnce({body: 'keys'});
            fs.writeFile.mockRejectedValueOnce(new Error('write error'));

            expect(getKeys('file path', true)).rejects.toThrow('write error');
        });

        test('Error on downloading keys', async () => {
            got.mockRejectedValueOnce(new Error('network error'));
            return expect(getKeys('file path', true)).rejects.toThrow('network error');
        });
    });
});