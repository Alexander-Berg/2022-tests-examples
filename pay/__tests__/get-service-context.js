const {TVM_KEYS_FILE_MAX_AGE_MS, TVM_TICKET_PARSER_PATH} = require('../constants');

jest.mock('../fs');
jest.mock('got');

describe('Ticket parser', () => {
    describe('Getting service context', () => {
        const mockResetContext = jest.fn();
        const mockServiceContext = jest.fn().mockImplementation(function () {
            this.resetKeys = mockResetContext;
        });

        jest.mock(TVM_TICKET_PARSER_PATH, () => ({
            version: 100,
            ServiceContext: mockServiceContext
        }), {virtual: true});

        const got = require('got');
        const fs = require('../fs');
        const {getContext, clearContext} = require('../ticket-parser');

        afterEach(clearContext);

        test('If keys file is expired create new context and return the same context for next calls. Reset keys if needed', async () => {
            expect.assertions(4);

            got.mockResolvedValue({body: 'keys'});

            fs.stat
                .mockResolvedValueOnce({mtimeMs: Date.now() - TVM_KEYS_FILE_MAX_AGE_MS - 10 * 1000}) //suppose keys file is expired
                .mockResolvedValueOnce({mtimeMs: Date.now() - TVM_KEYS_FILE_MAX_AGE_MS - 10 * 1000}) //suppose keys file is still expired, so we nees to reset keys
                .mockResolvedValueOnce({mtimeMs: Date.now() - TVM_KEYS_FILE_MAX_AGE_MS + 10 * 1000});

            const context = await getContext(100, 'Secret');

            let nextContext = await getContext(100, 'Secret');
            expect(fs.ensureDir).toHaveBeenCalled();
            expect(nextContext).toBe(context);

            expect(mockResetContext).toHaveBeenCalled();

            nextContext = await getContext(100, 'Secret');
            expect(nextContext).toBe(context);
        });

        test('If keys file doesn\'t exist download new keys', async () => {
            got.mockResolvedValue({body: 'keys'});

            fs.stat.mockRejectedValueOnce(new Error('file does\'t exist'));

            const context = await getContext(100, 'Secret');

            expect(mockServiceContext.mock.instances[0]).toEqual(context);
        });
    });
});