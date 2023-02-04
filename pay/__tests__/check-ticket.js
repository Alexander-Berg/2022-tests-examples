const {TVM_KEYS_FILE_MAX_AGE_MS, TVM_TICKET_PARSER_PATH} = require('../constants');

jest.mock('../fs');
jest.mock('got');

describe('Ticket parser', () => {
    describe('Checking ticket', () => {
        const mockCheckTicket = jest.fn();

        jest.mock(TVM_TICKET_PARSER_PATH, () => ({
            version: 100,
            ServiceContext: function () {
                this.checkTicket = mockCheckTicket
            }
        }), {virtual: true});

        const fs = require('../fs');
        const {checkTicket, clearContext} = require('../ticket-parser');

        afterEach(clearContext);

        test('Valid ticket', async () => {
            fs.stat.mockResolvedValueOnce({mtimeMs: Date.now() - TVM_KEYS_FILE_MAX_AGE_MS + 10 * 1000}); //suppose keys file is actual

            mockCheckTicket.mockReturnValueOnce('success');
            return expect(checkTicket('ticket', 100, 'Secret')).resolves.toBe('success');
        });

        test('Invalid ticket', async () => {
            fs.stat.mockResolvedValueOnce({mtimeMs: Date.now() - TVM_KEYS_FILE_MAX_AGE_MS + 10 * 1000}); //suppose keys file is actual

            mockCheckTicket.mockImplementationOnce(() => {
                throw new Error('fail');
            });

            return expect(checkTicket('ticket', 100, 'Secret', 'keys')).rejects
                .toEqual(expect.objectContaining({
                    message: 'fail',
                    status: 403
                }));
        });
    });
});