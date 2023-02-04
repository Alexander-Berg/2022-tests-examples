import {expect} from 'chai';
import nock from 'nock';
import {config} from 'app/config';
import {intHostsLoader} from 'app/lib/hosts';
import {Hosts} from '@yandex-int/maps-host-configs';
import {TvmDaemon} from 'tests/integration/tvm-daemon';
import {randomBytes} from 'crypto';
import {random} from 'tests/helpers/generation';
import {
    getAuthorDataByUid,
    AuthorData
} from 'app/lib/blackbox';

describe('blackbox', () => {
    let intHosts: Hosts;
    let tvmDaemon: TvmDaemon;
    const ip = '127.0.0.1';

    before(async () => {
        intHosts = await intHostsLoader.get();
        tvmDaemon = await TvmDaemon.start();
        nock.disableNetConnect();
        nock.enableNetConnect(/(127.0.0.1|localhost)/);
    });

    after(async () => {
        await tvmDaemon.stop();
        nock.enableNetConnect();
    });

    afterEach(nock.cleanAll);

    describe('getAuthorDataByUid', () => {
        it('should return an empty object if no uids passed', async () => {
            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {users: []});

            const actual = await getAuthorDataByUid([], ip);

            expect(nockBlackbox.isDone()).to.be.false;
            expect(actual).to.be.deep.equal({});
        });

        it('should return an empty object if the blackbox answered with an error', async () => {
            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {error: 'BlackBox error: Missing userip argument'});

            const payload = ['123', '234', '345'];
            const actual = await getAuthorDataByUid(payload, ip);

            expect(nockBlackbox.isDone()).to.be.true;
            expect(actual).to.be.deep.equal({});
        });

        it('should return an empty object when the blackbox didn\'t sent uid and public_name', async () => {
            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {
                    users: [
                        {
                            uid: {},
                            display_name: {}
                        }
                    ]
                });

            const payload = ['123'];
            const actual = await getAuthorDataByUid(payload, ip);

            expect(nockBlackbox.isDone()).to.be.true;
            expect(actual).to.be.deep.equal({});
        });

        it('should return correct responses corresponding to the input array', async () => {
            const expected: Record<string, AuthorData> = {};
            for (let i = 0; i < 2; i++) {
                expected[random(1, 1000)] = {
                    publicName: randomBytes(5).toString('hex'),
                    avatarId: `${random(1, 10)}/${random(1, 10)}`
                };
            }

            const nockUsers = Object.keys(expected).map((uid) => ({
                uid: {value: uid},
                display_name: {
                    public_name: expected[uid].publicName,
                    avatar: {
                        default: expected[uid].avatarId
                    }
                }
            }));
            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {users: nockUsers});

            const actual = await getAuthorDataByUid(Object.keys(expected), ip);

            expect(nockBlackbox.isDone()).to.be.true;
            expect(actual).to.be.deep.equal(expected);
        });

        it(`should send only one chunk if the input array length is less than ${config['blackbox.requestUidsLimit']}`,
            async () => {
                const expected: Record<string, AuthorData> = {};
                for (let i = 0; i < config['blackbox.requestUidsLimit'] - 1; i++) {
                    expected[random(1, 1000)] = {
                        publicName: randomBytes(5).toString('hex'),
                        avatarId: `${random(1, 5)}/${random(1, 5)}`
                    };
                }

                const nockUsers = Object.keys(expected).map((uid) => ({
                    uid: {value: uid},
                    display_name: {
                        public_name: expected[uid].publicName,
                        avatar: {
                            default: expected[uid].avatarId
                        }
                    }
                }));
                const nockBlackbox = nock(intHosts.blackbox)
                    .get('/')
                    .query(true)
                    .times(1)
                    .reply(200, {users: nockUsers});

                const payload = Object.keys(expected);
                const actual = await getAuthorDataByUid(payload, ip);

                expect(nockBlackbox.isDone()).to.be.true;
                expect(actual).to.be.deep.equal(expected);
            }
        );

        it('should send several chunks if the input array length is bigger than limit from config', async () => {
            const expected: Record<string, AuthorData> = {};
            for (let i = 0; i < config['blackbox.requestUidsLimit'] + 2; i++) {
                expected[random(1, 1000)] = {
                    publicName: randomBytes(5).toString('hex'),
                    avatarId: `0/0-0`
                };
            }

            const nockUsers = Object.keys(expected).map((uid) => ({
                uid: {value: uid},
                display_name: {
                    public_name: expected[uid].publicName,
                    avatar: {
                        default: expected[uid].avatarId
                    }
                }
            }));
            const firstChunkNockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {users: nockUsers.slice(0, config['blackbox.requestUidsLimit'])});
            const secondChunkNockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query(true)
                .times(1)
                .reply(200, {users: nockUsers.slice(config['blackbox.requestUidsLimit'])});

            const payload = Object.keys(expected);
            const actual = await getAuthorDataByUid(payload, ip);

            expect(firstChunkNockBlackbox.isDone()).to.be.true;
            expect(secondChunkNockBlackbox.isDone()).to.be.true;
            expect(actual).to.be.deep.equal(expected);
        });

        it(`should send request to blackbox with one uid when all lists have the same author`, async () => {
            const nockBlackbox = nock(intHosts.blackbox)
                .get('/')
                .query({
                    method: 'userinfo',
                    uid: '111',
                    userip: '127.0.0.1',
                    format: 'json',
                    regname: 'yes',
                    get_public_name: 'yes'
                })
                .times(1)
                .reply(200, {users: [
                    {
                        uid: {value: '111'},
                        display_name: {
                            public_name: 'Vasya Pupkin',
                            avatar: {
                                default: '0/0-0'
                            }
                        }
                    }
                ]});

            const payload = ['111', '111', '111', '111'];
            const actual = await getAuthorDataByUid(payload, ip);
            const expected = {
                111: {
                    publicName: 'Vasya Pupkin',
                    avatarId: '0/0-0'
                }
            };

            expect(nockBlackbox.isDone()).to.be.true;
            expect(actual).to.be.deep.equal(expected);
        });
    });
});
