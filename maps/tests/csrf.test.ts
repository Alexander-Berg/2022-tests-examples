import * as assert from 'assert';
import {Csrf, generateXscriptToken} from '../src';

describe('Csrf', () => {
    let csrf: Csrf;
    const originalDateNow = Date.now;

    beforeEach(() => {
        csrf = new Csrf({key: '2a6B8C64cb50A08ebcFb28'});
        Date.now = () => 1634215047680;
    });

    afterEach(() => {
        Date.now = originalDateNow;
    });

    describe('generateToken', () => {
        it('should generate token without uid', () => {
            const token = csrf.generateToken({
                yandexuid: '6447714881391768016'
            });
            assert(typeof token === 'string');
        });

        it('should generate token with uid', () => {
            const token = csrf.generateToken({
                yandexuid: '6447714881391768016',
                uid: '123142323'
            });
            assert(typeof token === 'string');
        });
    });

    describe('generateXscriptToken', () => {
        it('should generate token without uid', () => {
            const token = generateXscriptToken({
                yandexuid: '6447714881391768016'
            });
            assert.equal(token, 'ye035803b5588c31132ed768e0eb7a9bc');
        });

        it('should generate token with uid', () => {
            const token = generateXscriptToken({
                yandexuid: '6447714881391768016',
                uid: '123142323'
            });
            assert.equal(token, 'ua2bd7032fe1b034910a5526ec8753722');
        });
    });

    describe('isTokenValid', () => {
        it('should return true for token with same yandexuid', () => {
            const options = {
                yandexuid: '6447714881391768016'
            };
            const token = csrf.generateToken(options);
            assert.strictEqual(csrf.isTokenValid(token, options), true);
        });

        it('should return false for token with another yandexuid', () => {
            const token = csrf.generateToken({yandexuid: '6447714881391768016'});
            assert.strictEqual(csrf.isTokenValid(token, {yandexuid: '6447714881391768017'}), false);
        });

        it('should return true for token with same yandexuid and uid', () => {
            const options = {
                yandexuid: '6447714881391768016',
                uid: '123142323'
            };
            const token = csrf.generateToken(options);
            assert.strictEqual(csrf.isTokenValid(token, options), true);
        });

        it('should return false for token with same yandexuid, but another uid', () => {
            const options = {
                yandexuid: '6447714881391768016',
                uid: '123142323'
            };
            const token = csrf.generateToken(options);

            assert.strictEqual(csrf.isTokenValid(token, {
                yandexuid: options.yandexuid,
                uid: '123142324'
            }), false);
        });

        it('should return false for empty token', () => {
            assert.strictEqual(csrf.isTokenValid('', {yandexuid: '6447714881391768016'}), false);
        });

        it('should return false if token is not a string', () => {
            const options = {yandexuid: '6447714881391768016'};
            assert.strictEqual(csrf.isTokenValid(1, options), false);
            assert.strictEqual(csrf.isTokenValid(false, options), false);
            assert.strictEqual(csrf.isTokenValid([], options), false);
            assert.strictEqual(csrf.isTokenValid({}, options), false);
        });

        it('should return false if "yandexuid" option is missing or not a string', () => {
            const token = csrf.generateToken({} as any);
            assert.strictEqual(csrf.isTokenValid(token, {} as any), false);
            assert.strictEqual(csrf.isTokenValid(token, {yandexuid: 1 as any}), false);
        });

        it('should return false for invalid token format', () => {
            assert.strictEqual(
                csrf.isTokenValid('1111aaaa2222bb', {yandexuid: '6447714881391768016'}),
                false
            );
        });

        describe('when token is not alive', () => {
            it('should return false', function (done) {
                this.timeout(3000);
                Date.now = () => 1634215047680;
                const csrf = new Csrf({
                    key: '2a6B8C64cb50A08ebcFb28',
                    lifetime: 1
                });

                const options = {yandexuid: '6447714881391768016'};
                const token = csrf.generateToken(options);

                setTimeout(() => {
                    Date.now = () => 1634215097680;
                    assert.strictEqual(csrf.isTokenValid(token, options), false);
                    done();
                }, 2000);
            });
        });
    });
});
