import * as nock from 'nock';
import {expect} from 'chai';
import * as tvm from '../../lib';
import * as responses from '../../lib/responses';
import {expectRejection} from '../assertions';

const TEST_DAEMON_URL = 'http://localhost:1';
const TEST_TOKEN = 'test_token';
const TEST_SERVICE_TICKET = 'test_service_ticket';
const TEST_USER_TICKET = 'test_user_ticket';
const TEST_ALIAS = 'test_alias';
// Add 2 milliseconds to avoid time rounding problem. A callback of `setTimeout` can be called on 1
// ms earlier, than time interval measurable with `Date.now`.
// See https://gist.github.com/ikokostya/bdb67c90cb58a276405715dcb906d72f
const TIME_DELTA_MS = 2;

function mockCheckSrv(): nock.Interceptor {
    const scope = nock(TEST_DAEMON_URL, {
        reqheaders: {
            Authorization: TEST_TOKEN,
            'X-Ya-Service-Ticket': TEST_SERVICE_TICKET
        }
    });
    return scope.get('/tvm/checksrv');
}

function mockCheckUsr(): nock.Interceptor {
    const scope = nock(TEST_DAEMON_URL, {
        reqheaders: {
            Authorization: TEST_TOKEN,
            'X-Ya-User-Ticket': TEST_USER_TICKET
        }
    });
    return scope.get('/tvm/checkusr');
}

function mockGetTickets(): nock.Interceptor {
    const scope = nock(TEST_DAEMON_URL, {
        reqheaders: {
            Authorization: TEST_TOKEN
        }
    });
    return scope.get('/tvm/tickets');
}

function mockGetRoles(): nock.Interceptor {
    const scope = nock(TEST_DAEMON_URL, {
        reqheaders: {
            Authorization: TEST_TOKEN
        }
    });
    return scope.get('/v2/roles').query({self: TEST_ALIAS});
}

function delay(ms: number): Promise<void> {
    return new Promise((resolve) => {
        setTimeout(resolve, ms);
    });
}

describe('TvmClient', () => {
    afterEach(() => {
        nock.cleanAll();
    });

    describe('checkTicket()', () => {
        let client: tvm.Client;

        beforeEach(() => {
            client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                serviceTicketCacheEnabled: false
            });
        });

        describe('when request to daemon failed', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockCheckSrv().replyWithError('something awful happened');

                const serviceInfoPromise = client.checkServiceTicket(TEST_SERVICE_TICKET);
                await expectRejection(serviceInfoPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 500', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockCheckSrv().reply(500, 'Internal server error');

                const serviceInfoPromise = client.checkServiceTicket(TEST_SERVICE_TICKET);
                await expectRejection(serviceInfoPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 400', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockCheckSrv().reply(400, 'Bad request');

                const serviceInfoPromise = client.checkServiceTicket(TEST_SERVICE_TICKET);
                await expectRejection(serviceInfoPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 403', () => {
            it('should reject promise with InvalidTicketError', async () => {
                const checkError: responses.CheckError = {
                    error: 'Wrong ticket dst, expected 242, got 222',
                    debug_string: 'ticket_type=service;expiration_time=1513036860;src=999;dst=222;scope=;',
                    logging_string: '3:serv:COsRELy4vNEFIgYI8gEQ8gE:'
                };
                const tvmCall = mockCheckSrv().reply(403, checkError);

                const serviceInfoPromise = client.checkServiceTicket(TEST_SERVICE_TICKET);
                await expectRejection(serviceInfoPromise, (err: tvm.InvalidTicketError) => {
                    expect(err).to.be.instanceof(tvm.InvalidTicketError);
                    expect(err.debugString).to.be.equal(checkError.debug_string);
                    expect(err.loggingString).to.be.equal(checkError.logging_string);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 200', () => {
            it('should fulfill promise with service info', async () => {
                const checkOk: responses.CheckSrvOk = {
                    src: 242,
                    dst: 242,
                    scopes: null,
                    debug_string: 'ticket_type=service;expiration_time=1513036860;src=242;dst=242;scope=;',
                    logging_string: '3:serv:COsRELy4vNEFIgYI8gEQ8gE:'
                };

                const tvmCall = mockCheckSrv().reply(200, checkOk);

                const serviceInfo = await client.checkServiceTicket(TEST_SERVICE_TICKET);
                expect(serviceInfo).to.deep.equal({
                    src: checkOk.src,
                    dst: checkOk.dst,
                    scopes: checkOk.scopes,
                    debugString: checkOk.debug_string,
                    loggingString: checkOk.logging_string
                });

                tvmCall.done();
            });
        });
    });

    describe('checkServiceTicket() caching', () => {
        const checkOk: responses.CheckSrvOk = {
            src: 242,
            dst: 242,
            scopes: null,
            debug_string: 'ticket_type=service;expiration_time=1513036860;src=242;dst=242;scope=;',
            logging_string: '3:serv:COsRELy4vNEFIgYI8gEQ8gE:'
        };

        it('should not cache errors', async () => {
            const client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                serviceTicketCacheTimeMs: 1000
            });

            const tvmCall1 = mockCheckSrv().reply(500, 'Internal server error');
            await expectRejection(client.checkServiceTicket(TEST_SERVICE_TICKET));
            expect(tvmCall1.isDone()).to.be.equal(true, 'TVM daemon must be called first time');

            const checkError: responses.CheckError = {
                error: 'Wrong ticket dst, expected 242, got 222',
                debug_string: 'ticket_type=service;expiration_time=1513036860;src=999;dst=222;scope=;',
                logging_string: '3:serv:COsRELy4vNEFIgYI8gEQ8gE:'
            };
            const tvmCall2 = mockCheckSrv().reply(403, checkError);
            await expectRejection(client.checkServiceTicket(TEST_SERVICE_TICKET));
            expect(tvmCall2.isDone()).to.be.equal(true, 'TVM daemon must be called second time');

            const tvmCall3 = mockCheckSrv().reply(200, checkOk);
            await client.checkServiceTicket(TEST_SERVICE_TICKET);
            expect(tvmCall3.isDone()).to.be.equal(true, 'TVM daemon must be called third time');
        });

        it('should cache success responses', async () => {
            const CACHE_TIME_MS = 100;

            const client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                serviceTicketCacheTimeMs: CACHE_TIME_MS
            });

            const tvmCall1 = mockCheckSrv().reply(200, checkOk);
            await client.checkServiceTicket(TEST_SERVICE_TICKET);
            expect(tvmCall1.isDone()).to.be.equal(true, 'TVM daemon must be called first time');

            const tvmCall2 = mockCheckSrv().reply(200, checkOk);
            await client.checkServiceTicket(TEST_SERVICE_TICKET);
            expect(tvmCall2.isDone()).to.be.equal(false, 'TVM daemon must not be called second time');
            nock.cleanAll();

            await delay(CACHE_TIME_MS + TIME_DELTA_MS);

            const tvmCall3 = mockCheckSrv().reply(200, checkOk);
            await client.checkServiceTicket(TEST_SERVICE_TICKET);
            expect(tvmCall3.isDone()).to.be.equal(true, 'TVM daemon must be called third time');
        });
    });

    describe('getServiceTickets()', () => {
        let client: tvm.Client;

        beforeEach(() => {
            client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                serviceTicketCacheEnabled: false
            });
        });

        describe('when request to daemon failed', () => {
            it('should reject all promises with ClientError', async () => {
                const tvmCall = mockGetTickets()
                    .query({dsts: 'a,b'})
                    .replyWithError('something awful happened');

                const [aTicketPromise, bTicketPromise] = client.getServiceTickets(['a', 'b']);
                await expectRejection(aTicketPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });
                await expectRejection(bTicketPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 500', () => {
            it('should reject all promises with ClientError', async () => {
                const tvmCall = mockGetTickets()
                    .query({dsts: 'a,b'})
                    .reply(500, 'Internal server error');

                const [aTicketPromise, bTicketPromise] = client.getServiceTickets(['a', 'b']);
                await expectRejection(aTicketPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });
                await expectRejection(bTicketPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 400', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockGetTickets()
                    .query({dsts: 'a,b'})
                    .reply(400, 'Bad request');

                const [aTicketPromise, bTicketPromise] = client.getServiceTickets(['a', 'b']);
                await expectRejection(aTicketPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });
                await expectRejection(bTicketPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 200', () => {
            it('should resolve and reject corresponding promises', async () => {
                const tickets: responses.Tickets = {
                    a: {
                        error: 'Dst is not found',
                        tvm_id: 1
                    },
                    b: {
                        ticket: 'b_ticket',
                        tvm_id: 2
                    }
                };

                const tvmCall = mockGetTickets()
                    .query({dsts: 'a,b'})
                    .reply(200, tickets);

                const [aTicketPromise, bTicketPromise] = client.getServiceTickets(['a', 'b']);
                await expectRejection(aTicketPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });
                expect(await bTicketPromise).to.equal('b_ticket');

                tvmCall.done();
            });

            it('should reject corresponding promise if there is not data for the requested dst', async () => {
                const tickets: responses.Tickets = {
                    b: {
                        ticket: 'b_ticket',
                        tvm_id: 2
                    }
                };

                const tvmCall = mockGetTickets()
                    .query({dsts: 'a,b'})
                    .reply(200, tickets);

                const [aTicketPromise, bTicketPromise] = client.getServiceTickets(['a', 'b']);
                await expectRejection(aTicketPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });
                expect(await bTicketPromise).to.equal('b_ticket');

                tvmCall.done();
            });
        });
    });

    describe('getServiceTickets() caching', () => {
        it('should cache success tickets responses', async () => {
            const CACHE_TIME_MS = 200;

            const client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                serviceTicketCacheTimeMs: CACHE_TIME_MS
            });

            const tickets1: responses.Tickets = {
                a: {
                    error: 'Dst is not found',
                    tvm_id: 1
                },
                b: {
                    ticket: 'b_ticket',
                    tvm_id: 2
                }
            };
            const tvmCall1 = mockGetTickets()
                .query({dsts: 'a,b'})
                .reply(200, tickets1);

            let [aTicketPromise, bTicketPromise] = client.getServiceTickets(['a', 'b']);
            await expectRejection(aTicketPromise);
            expect(await bTicketPromise).to.equal('b_ticket');
            expect(tvmCall1.isDone()).to.equal(true, 'TVM daemon must be called first time');

            const tickets2: responses.Tickets = {
                a: {
                    ticket: 'a_ticket',
                    tvm_id: 1
                }
            };
            const tvmCall2 = mockGetTickets()
                // Request only a.
                .query({dsts: 'a'})
                .reply(200, tickets2);

            [aTicketPromise, bTicketPromise] = client.getServiceTickets(['a', 'b']);
            expect(await aTicketPromise).to.equal('a_ticket');
            expect(await bTicketPromise).to.equal('b_ticket');
            expect(tvmCall2.isDone()).to.equal(true, 'TVM daemon must be called second time');

            const tvmCall3 = mockGetTickets()
                .query(true)
                .reply(200, {});

            [aTicketPromise, bTicketPromise] = client.getServiceTickets(['a', 'b']);
            expect(await aTicketPromise).to.equal('a_ticket');
            expect(await bTicketPromise).to.equal('b_ticket');
            expect(tvmCall3.isDone()).to.equal(false, 'TVM daemon must not be called third time');
            nock.cleanAll();

            // Wait cache expiration.
            await delay(CACHE_TIME_MS + TIME_DELTA_MS);

            const tickets4: responses.Tickets = {
                a: {
                    ticket: 'a_ticket',
                    tvm_id: 1
                },
                b: {
                    ticket: 'b_ticket',
                    tvm_id: 2
                }
            };
            const tvmCall4 = mockGetTickets()
                .query({dsts: 'a,b'})
                .reply(200, tickets4);

            [aTicketPromise, bTicketPromise] = client.getServiceTickets(['a', 'b']);
            expect(await aTicketPromise).to.equal('a_ticket');
            expect(await bTicketPromise).to.equal('b_ticket');
            expect(tvmCall4.isDone()).to.equal(true, 'TVM daemon must be called fourth time');
        });
    });

    describe('checkUserTicket()', () => {
        let client: tvm.Client;

        beforeEach(() => {
            client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                userTicketCacheEnabled: false
            });
        });

        describe('when request to daemon failed', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockCheckUsr().replyWithError('something awful happened');

                const userInfoPromise = client.checkUserTicket(TEST_USER_TICKET);
                await expectRejection(userInfoPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 500', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockCheckUsr().reply(500, 'Internal server error');

                const userInfoPromise = client.checkUserTicket(TEST_USER_TICKET);
                await expectRejection(userInfoPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 400', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockCheckUsr().reply(400, 'Bad request');

                const userInfoPromise = client.checkUserTicket(TEST_USER_TICKET);
                await expectRejection(userInfoPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 403', () => {
            it('should reject promise with InvalidTicketError', async () => {
                const checkError: responses.CheckError = {
                    error: 'invalid ticket format',
                    debug_string: 'foo',
                    logging_string: 'bar'
                };
                const tvmCall = mockCheckUsr().reply(403, checkError);

                const userInfoPromise = client.checkUserTicket(TEST_USER_TICKET);
                await expectRejection(userInfoPromise, (err: tvm.InvalidTicketError) => {
                    expect(err).to.be.instanceof(tvm.InvalidTicketError);
                    expect(err.debugString).to.be.equal(checkError.debug_string);
                    expect(err.loggingString).to.be.equal(checkError.logging_string);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 200', () => {
            it('should fulfill promise with user info', async () => {
                const checkOk: responses.CheckUsrOk = {
                    default_uid: 4000849384,
                    uids: [4000849384],
                    scopes: ['bb:sessionid'],
                    debug_string: 'ticket_type=user;expiration_time=842354073952;default_uid=40' +
                        '00849384;uid=4000849384;',
                    logging_string: '3:user:CJ8SEMz_3tEFGiAKBgjou-DzDhDou-DzDhoMYmI6c2Vzc2lvbmlkIAsoAQ:'
                };

                const tvmCall = mockCheckUsr().reply(200, checkOk);

                const userInfo = await client.checkUserTicket(TEST_USER_TICKET);
                expect(userInfo).to.deep.equal({
                    defaultUid: '4000849384',
                    uids: ['4000849384'],
                    scopes: checkOk.scopes,
                    debugString: checkOk.debug_string,
                    loggingString: checkOk.logging_string
                });

                tvmCall.done();
            });
        });
    });

    describe('checkUserTicket() caching', () => {
        const CACHE_TIME_MS = 100;

        const checkOk: responses.CheckUsrOk = {
            default_uid: 1,
            uids: [1, 2],
            scopes: ['bb:sessionid'],
            debug_string: 'foo',
            logging_string: 'bar'
        };

        it('should not cache errors', async () => {
            const client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                userTicketCacheTimeMs: CACHE_TIME_MS
            });

            const tvmCall1 = mockCheckUsr().reply(500, 'Internal server error');
            await expectRejection(client.checkUserTicket(TEST_USER_TICKET));
            expect(tvmCall1.isDone()).to.be.equal(true, 'TVM daemon must be called first time');

            const checkError: responses.CheckError = {
                error: 'invalid ticket format',
                debug_string: 'foo',
                logging_string: 'bar'
            };
            const tvmCall2 = mockCheckUsr().reply(403, checkError);
            await expectRejection(client.checkUserTicket(TEST_USER_TICKET));
            expect(tvmCall2.isDone()).to.be.equal(true, 'TVM daemon must be called second time');

            const tvmCall3 = mockCheckUsr().reply(200, checkOk);
            await client.checkUserTicket(TEST_USER_TICKET);
            expect(tvmCall3.isDone()).to.be.equal(true, 'TVM daemon must be called third time');
        });

        it('should cache success responses', async () => {
            const client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                userTicketCacheTimeMs: CACHE_TIME_MS
            });

            const tvmCall1 = mockCheckUsr().reply(200, checkOk);
            await client.checkUserTicket(TEST_USER_TICKET);
            expect(tvmCall1.isDone()).to.be.equal(true, 'TVM daemon must be called first time');

            const tvmCall2 = mockCheckUsr().reply(200, checkOk);
            await client.checkUserTicket(TEST_USER_TICKET);
            expect(tvmCall2.isDone()).to.be.equal(false, 'TVM daemon must not be called second time');
            nock.cleanAll();

            await delay(CACHE_TIME_MS + TIME_DELTA_MS);

            const tvmCall3 = mockCheckUsr().reply(200, checkOk);
            await client.checkUserTicket(TEST_USER_TICKET);
            expect(tvmCall3.isDone()).to.be.equal(true, 'TVM daemon must be called third time');
        });
    });

    describe('getRoles()', () => {
        let client: tvm.Client;

        beforeEach(() => {
            client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                userTicketCacheEnabled: false
            });
        });

        describe('when request to daemon failed', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockGetRoles().replyWithError('something awful happened');

                const rolesPromise = client.getRoles(TEST_ALIAS);
                await expectRejection(rolesPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 500', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockGetRoles().reply(500, 'Internal server error');

                const rolesPromise = client.getRoles(TEST_ALIAS);
                await expectRejection(rolesPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 400', () => {
            it('should reject promise with ClientError', async () => {
                const tvmCall = mockGetRoles().reply(400, 'Bad request');

                const rolesPromise = client.getRoles(TEST_ALIAS);
                await expectRejection(rolesPromise, (err) => {
                    expect(err).to.be.instanceof(tvm.ClientError);
                });

                tvmCall.done();
            });
        });

        describe('when daemon returns 200', () => {
            it('should fulfill promise with roles', async () => {
                const checkOk: responses.GetRoles = {
                    revision: 'REVISION',
                    born_date: 0,
                    tvm: {
                        0: {
                            '/role1': [{}]
                        }
                    },
                    user: {
                        31337: {
                            '/role2': [{}]
                        }
                    }
                };

                const tvmCall = mockGetRoles().reply(200, checkOk);

                const roles = await client.getRoles(TEST_ALIAS);
                expect(roles).to.deep.equal({
                    tvm: {
                        0: {
                            '/role1': [{}]
                        }
                    },
                    user: {
                        31337: {
                            '/role2': [{}]
                        }
                    }
                });

                tvmCall.done();
            });
        });
    });

    describe('getRoles() caching', () => {
        const CACHE_TIME_MS = 100;

        const checkOk: responses.GetRoles = {
            revision: 'REVISION',
            born_date: 0,
            tvm: {
                0: {
                    '/role1': [{}]
                }
            },
            user: {
                31337: {
                    '/role2': [{}]
                }
            }
        };

        it('should not cache errors', async () => {
            const client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                rolesCacheTimeMs: CACHE_TIME_MS
            });

            const tvmCall1 = mockGetRoles().reply(500, 'Internal server error');
            await expectRejection(client.getRoles(TEST_ALIAS));
            expect(tvmCall1.isDone()).to.be.equal(true, 'TVM daemon must be called first time');

            const tvmCall2 = mockGetRoles().reply(200, checkOk);
            await client.getRoles(TEST_ALIAS);
            expect(tvmCall2.isDone()).to.be.equal(true, 'TVM daemon must be called second time');
        });

        it('should cache success responses', async () => {
            const client = new tvm.Client({
                token: TEST_TOKEN,
                daemonBaseUrl: TEST_DAEMON_URL,
                rolesCacheTimeMs: CACHE_TIME_MS
            });

            const tvmCall1 = mockGetRoles().reply(200, checkOk);
            await client.getRoles(TEST_ALIAS);
            expect(tvmCall1.isDone()).to.be.equal(true, 'TVM daemon must be called first time');

            const tvmCall2 = mockGetRoles().reply(200, checkOk);
            await client.getRoles(TEST_ALIAS);
            expect(tvmCall2.isDone()).to.be.equal(false, 'TVM daemon must not be called second time');
            nock.cleanAll();

            await delay(CACHE_TIME_MS + TIME_DELTA_MS);

            const tvmCall3 = mockGetRoles().reply(200, checkOk);
            await client.getRoles(TEST_ALIAS);
            expect(tvmCall3.isDone()).to.be.equal(true, 'TVM daemon must be called third time');
        });
    });
});
