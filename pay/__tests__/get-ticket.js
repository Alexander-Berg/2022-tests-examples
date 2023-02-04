jest.mock('../fs');
jest.mock('got');

const { TVM_TICKET_PARSER_PATH, TVM_TICKET_MAX_AGE_MS } = require('../constants');

describe('Ticket parser', () => {
    describe('Getting ticket', () => {
        const got = require('got');
        const fs = require('../fs');

        const mockSignParams = jest.fn();
        jest.mock(TVM_TICKET_PARSER_PATH, () => ({
            version: 100,
            ServiceContext: function () {
                this.signParams = mockSignParams;
            }
        }), { virtual: true });

        const { getTicket, clearTicketsCache, saveTicketToCache } = require('../ticket-parser');

        beforeAll(() => {
            //keys file is actual
            fs.stat.mockResolvedValue({ mtimeMs: Date.now() - 10 * 1000 });
        });

        afterEach(clearTicketsCache);

        test('Ticket does not exist. Download it from TVM', async () => {
            expect.assertions(2);

            const dstId = 200;
            const ticket = 'ticket';

            //will request new ticket
            got.post.mockResolvedValue({ body: { [dstId]: { ticket } } });

            await expect(getTicket(100, 'Secret', dstId, 'SomeService')).resolves.toEqual(ticket);
            return expect(mockSignParams).toHaveBeenCalled();
        });

        test('Ticket exist but is expired. Download new one', async () => {
            expect.assertions(2);

            const dstId = 200;
            const ticket = 'ticket';

            got.post.mockResolvedValue({ body: { [dstId]: { ticket } } });
            //expired by 1 s
            saveTicketToCache(dstId, ticket, Date.now() - TVM_TICKET_MAX_AGE_MS - 1000);

            await expect(getTicket(100, 'Secret', dstId, 'SomeService')).resolves.toEqual(ticket);
            return expect(mockSignParams).toHaveBeenCalled();
        });

        test('Ticket exists and is actual. Return it from cache', async () => {
            expect.assertions(3);

            const dstId = 200;
            const ticket = 'ticket';

            //ticket is not expired
            saveTicketToCache(dstId, ticket, Date.now() - TVM_TICKET_MAX_AGE_MS + 1000);

            expect(getTicket(100, 'Secret', dstId, 'SomeService')).resolves.toEqual(ticket);
            expect(mockSignParams).not.toHaveBeenCalled();
            return expect(got.post).not.toHaveBeenCalled();
        });

        test('Error on downloading ticket from TVM', async () => {
            expect.assertions(1);

            got.post.mockRejectedValue(new Error('network error'));

            return expect(getTicket(100, 'Secret', 200, 'SomeService')).rejects.toThrow('network error');
        });
    });
});
