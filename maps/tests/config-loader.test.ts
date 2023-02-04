import {expect} from 'chai';
import * as mockFs from 'mock-fs';
import {ConfigLoaderImpl, ConfigLoader} from '../lib/config-loader';

/**
 * Returns a new promise that fulfilled if the given promise will be rejected.
 */
export async function expectRejection(promise: Promise<any>): Promise<void> {
    try {
        await promise;
    } catch (err) {
        return;
    }
    throw new Error('Expect promise to be rejected, but it was fulfilled');
}

function delay(ms: number): Promise<void> {
    return new Promise((resolve) => {
        setTimeout(() => resolve(), ms);
    });
}

describe('ConfigLoaderImpl', () => {
    const LOADER_OPTIONS = {
        maxCacheAge: 0,
        overrideKey: 'hosts' as 'hosts',
        basePath: '/cfg'
    };

    afterEach(() => {
        mockFs.restore();
    });

    describe('loading environment configuration', () => {
        it('should search configs in the given directory', async () => {
            mockFs({
                '/cfg/hosts-testing.json': '{"a": "a"}'
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing',
                basePath: '/'
            });

            await expectRejection(loader.get());
        });

        it('should load config by environment', async () => {
            mockFs({
                '/cfg': {
                    'hosts-production.json': '{"a": "a.prod"}',
                    'hosts-testing.json': '{"a": "a.test"}'
                }
            });

            const loaderTest = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing'
            });

            const loaderProd = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production'
            });

            let config = await loaderTest.get();
            expect(config).to.deep.equal({a: 'a.test'});

            config = await loaderProd.get();
            expect(config).to.deep.equal({a: 'a.prod'});
        });

        it('should cache environment config', async () => {
            mockFs({
                '/cfg/hosts-testing.json': '{"a": "a.test"}'
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing',
                maxCacheAge: 200
            });

            await loader.get();

            mockFs({
                '/cfg/hosts-testing.json': '{"a": "a.test.new"}'
            });

            await delay(100);

            let config = await loader.get();
            expect(config).to.deep.equal({a: 'a.test'});

            await delay(150);

            config = await loader.get();
            expect(config).to.deep.equal({a: 'a.test.new'});
        });

        it('should reject promise if config does not exist', async () => {
            mockFs({
                '/cfg/hosts-testing.json': '{"a": "a.test"}'
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'unknown'
            });

            await expectRejection(loader.get());
        });

        it('should reject promise when config has invalid format', () => {
            mockFs({
                '/cfg/hosts-testing.json': '{"a": a"}'
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing'
            });

            return expectRejection(loader.get());
        });

        it('should not cache config in invalid format', async () => {
            mockFs({
                '/cfg/hosts-testing.json': '{"a": a"}'
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing'
            });

            await expectRejection(loader.get());

            mockFs({
                '/cfg/hosts-testing.json': '{"a": "a.correct"}'
            });
            const config = await loader.get();
            expect(config).to.deep.equal({a: 'a.correct'});
        });
    });

    describe('loading hostname configuration', () => {
        it('should return environment config extended with host config', async () => {
            mockFs({
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod',
                    b: 'b.prod'
                }),
                '/cfg/production/maps.yandex.ru.json': JSON.stringify({
                    a: 'a.prod.maps',
                    c: 'c.prod.maps'
                }),
                '/cfg/testing/maps.yandex.ru.json': JSON.stringify({
                    a: 'a.test.maps',
                    c: 'c.test.maps'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production'
            });

            const config = await loader.get({hostname: 'maps.yandex.ru'});
            expect(config).to.deep.equal({
                a: 'a.prod.maps',
                b: 'b.prod',
                c: 'c.prod.maps'
            });
        });

        it('should not apply non existent config', async () => {
            mockFs({
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod',
                    b: 'b.prod'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production'
            });

            const config = await loader.get({hostname: 'maps.yandex.ru'});
            expect(config).to.deep.equal({
                a: 'a.prod',
                b: 'b.prod'
            });
        });

        it('should cache hostname config', async () => {
            mockFs({
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod',
                    b: 'b.prod'
                }),
                '/cfg/production/maps.yandex.ru.json': JSON.stringify({
                    a: 'a.prod.maps'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production',
                maxCacheAge: 200
            });

            await loader.get({hostname: 'maps.yandex.ru'});

            mockFs({
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod',
                    b: 'b.prod'
                }),
                '/cfg/production/maps.yandex.ru.json': JSON.stringify({
                    a: 'a.prod.maps.new'
                })
            });

            await delay(100);

            let config = await loader.get({hostname: 'maps.yandex.ru'});
            expect(config).to.deep.equal({
                a: 'a.prod.maps',
                b: 'b.prod'
            });

            await delay(150);

            config = await loader.get({hostname: 'maps.yandex.ru'});
            expect(config).to.deep.equal({
                a: 'a.prod.maps.new',
                b: 'b.prod'
            });
        });

        it('should not change environment config', async () => {
            mockFs({
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod',
                    b: 'b.prod'
                }),
                '/cfg/production/maps.yandex.ru.json': JSON.stringify({
                    a: 'a.prod.maps'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production'
            });

            await loader.get({hostname: 'maps.yandex.ru'});
            const config = await loader.get();
            expect(config).to.deep.equal({
                a: 'a.prod',
                b: 'b.prod'
            });
        });

        it('should not allow load config from parent directory', async () => {
            mockFs({
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod'
                }),
                '/cfg/private.json': JSON.stringify({
                    a: 'a.private'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production'
            });

            await expectRejection(loader.get({hostname: '../private'}));
        });
    });

    describe('hostname override', () => {
        let files: Record<string, string>;
        let loader: ConfigLoader;

        beforeEach(() => {
            files = {
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod',
                    b: 'b.prod'
                }),
                '/cfg/production/foo.json': JSON.stringify({
                    a: 'a.prod.foo'
                }),
                '/cfg/production/bar.json': JSON.stringify({
                    b: 'b.prod.bar'
                }),
                '/cfg/hosts-testing.json': JSON.stringify({
                    a: 'a.test',
                    b: 'b.test'
                }),
                '/cfg/testing/foo.json': JSON.stringify({
                    a: 'a.test.foo'
                }),
                '/cfg/testing/bar.json': JSON.stringify({
                    b: 'b.test.bar'
                })
            };

            mockFs(files);

            loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production'
            });
        });

        it('should load configs in order environment -> overrides.hostname', async () => {
            const config = await loader.get({overrides: {hostname: 'bar'}});
            expect(config).to.deep.equal({
                a: 'a.prod',
                b: 'b.prod.bar'
            });
        });

        it('should load configs in order environment -> hostname -> overrides.hostname', async () => {
            const config = await loader.get({hostname: 'foo', overrides: {hostname: 'bar'}});
            expect(config).to.deep.equal({
                a: 'a.prod.foo',
                b: 'b.prod.bar'
            });
        });

        it('should skip nonexistent hostname config', async () => {
            const config = await loader.get({hostname: 'foo', overrides: {hostname: 'baz'}});
            expect(config).to.deep.equal({
                a: 'a.prod.foo',
                b: 'b.prod'
            });
        });

        it('should not change base hostname config', async () => {
            await loader.get({hostname: 'foo', overrides: {hostname: 'bar'}});
            const config = await loader.get({hostname: 'foo'});
            expect(config).to.deep.equal({
                a: 'a.prod.foo',
                b: 'b.prod'
            });
        });

        it('should cache hostname config from query param', async () => {
            loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production',
                maxCacheAge: 200
            });

            function load() {
                return loader.get({hostname: 'foo', overrides: {hostname: 'bar'}});
            }

            await load();

            mockFs({
                ...files,
                '/cfg/production/bar.json': JSON.stringify({
                    a: 'a.prod.bar',
                    b: 'b.prod.bar'
                })
            });

            await delay(100);

            let config = await load();
            expect(config).to.deep.equal({
                a: 'a.prod.foo',
                b: 'b.prod.bar'
            });

            await delay(150);

            config = await load();
            expect(config).to.deep.equal({
                a: 'a.prod.bar',
                b: 'b.prod.bar'
            });
        });

        it('should ignore non-object overrides', async () => {
            let config = await loader.get({overrides: 'str'});
            expect(config).to.deep.equal({
                a: 'a.prod',
                b: 'b.prod'
            });

            config = await loader.get({overrides: null});
            expect(config).to.deep.equal({
                a: 'a.prod',
                b: 'b.prod'
            });

            config = await loader.get({overrides: []});
            expect(config).to.deep.equal({
                a: 'a.prod',
                b: 'b.prod'
            });

        });

        it('should ignore non-string overrides.hostname property', async () => {
            const overrides = {
                hostname: ['foo']
            };

            const config = await loader.get({overrides});
            expect(config).to.deep.equal({
                a: 'a.prod',
                b: 'b.prod'
            });
        });

        it('should not allow to load config from parent directory', async () => {
            mockFs({
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod'
                }),
                '/cfg/private.json': JSON.stringify({
                    a: 'a.private'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production'
            });

            const config = await loader.get({overrides: {hostname: '../private'}});
            expect(config).to.deep.equal({a: 'a.prod'});
        });
    });

    describe('specific hosts override', () => {
        it('should apply values from overrides.hosts', async () => {
            mockFs({
                '/cfg/hosts-testing.json': JSON.stringify({
                    a: 'a.test',
                    b: 'b.test'
                }),
                '/cfg/testing/foo.json': JSON.stringify({
                    a: 'a.test.foo'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing'
            });

            const overrides = {
                hosts: {
                    b: 'b.override.host'
                },
                inthosts: {
                    b: 'b.override.inthost'
                }
            };

            const config = await loader.get({hostname: 'foo', overrides});
            expect(config).to.deep.equal({
                a: 'a.test.foo',
                b: 'b.override.host'
            });
        });

        it('should ignore unknown overrides', async () => {
            mockFs({
                '/cfg/hosts-testing.json': JSON.stringify({
                    a: 'a.test',
                    b: 'b.test'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing'
            });

            const overrides = {
                myHosts: {
                    b: 'b.override'
                }
            };

            const config = await loader.get({overrides});
            expect(config).to.deep.equal({
                a: 'a.test',
                b: 'b.test'
            });
        });

        it('should ignore non-string values in overrides.hosts', async () => {
            mockFs({
                '/cfg/hosts-testing.json': JSON.stringify({
                    a: 'a.test',
                    b: 'b.test'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing'
            });

            const overrides = {
                hosts: {
                    b: {
                        c: 'b.override'
                    },
                    d: null,
                    a: 'a.override'
                }
            };

            const config = await loader.get({overrides});
            expect(config).to.deep.equal({
                a: 'a.override',
                b: 'b.test'
            });
        });

        it('should ignore specific hosts in `production` environment', async () => {
            mockFs({
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production'
            });

            const overrides = {
                hosts: {
                    a: 'a.override'
                }
            };

            const config = await loader.get({overrides});
            expect(config).to.deep.equal({
                a: 'a.prod'
            });
        });

        it('should apply specific hosts in `production` environment with enableOverrides option', async () => {
            mockFs({
                '/cfg/hosts-production.json': JSON.stringify({
                    a: 'a.prod'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'production',
                enableOverrides: true
            });

            const overrides = {
                hosts: {
                    a: 'a.override'
                }
            };

            const config = await loader.get({overrides});
            expect(config).to.deep.equal({
                a: 'a.override'
            });
        });

        it('should not cache specific hosts', async () => {
            mockFs({
                '/cfg/hosts-testing.json': JSON.stringify({
                    a: 'a.test'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing',
                maxCacheAge: 200
            });

            const overrides = {
                hosts: {
                    b: 'b.override'
                }
            };

            await loader.get({overrides});

            const newOverrides = {
                hosts: {
                    b: 'b.override.new'
                }
            };
            const config = await loader.get({overrides: newOverrides});
            expect(config).to.deep.equal({
                a: 'a.test',
                b: 'b.override.new'
            });
        });

        it('should not change previous config', async () => {
            mockFs({
                '/cfg/hosts-testing.json': JSON.stringify({
                    a: 'a.test'
                })
            });

            const loader = new ConfigLoaderImpl({
                ...LOADER_OPTIONS,
                env: 'testing'
            });

            const overrides = {
                hosts: {
                    b: 'b.override'
                }
            };

            await loader.get({overrides});
            const config = await loader.get();
            expect(config).to.deep.equal({a: 'a.test'});
        });
    });

});
