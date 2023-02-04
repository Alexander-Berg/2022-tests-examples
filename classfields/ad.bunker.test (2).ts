/**
 * @jest-environment node
 */

import type { ExperimentsProvider, ServerRequestProcessor, BunkerPageDefinition } from './ad.bunker';

jest.mock('@vertis/pino', () => {
    const mock = {
        child: () => mock,
        error: jest.fn(() => {}),
    };
    return mock;
});

import { cloneDeep } from 'lodash';
import adBunker from './ad.bunker';
import logger from '@vertis/pino';

let mathRandomSpy: jest.SpyInstance;
let bunkerExposeRefsSpy: jest.SpyInstance;
let experiments: ExperimentsProvider;
let processRequest: ServerRequestProcessor;
let req: { url: string };
beforeEach(() => {
    experiments = {
        has: () => false,
        statId: '123',
    };
    adBunker.resetData();
    processRequest = jest.fn((request) => {
        return Promise.resolve({ ...request });
    });
    mathRandomSpy = jest.spyOn(global.Math, 'random').mockReturnValue(0.123456789);
    bunkerExposeRefsSpy = jest.spyOn(adBunker, '_exposeRefs');
    req = { url: '/' };
});

afterEach(() => {
    mathRandomSpy.mockRestore();
    bunkerExposeRefsSpy.mockRestore();
});

it('должен вернуть настройки рекламы без раскрытия refs', () => {
    const data = {
        '/auto_ru/ad/desktop/index': {
            top: {
                sources: [
                    {
                        type: 'rtb',
                        extParams: {
                            awaps_section: '29337',
                        },
                        code: 'R-A-148383-1',
                    },
                ],
            },
        },
    } as unknown as Record<string, BunkerPageDefinition>;

    return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен вернуть настройки рекламы с короткой записью без sources', () => {
    const data = {
        '/auto_ru/ad/desktop/index': {
            top: [
                {
                    type: 'rtb',
                    extParams: {
                        awaps_section: '29337',
                    },
                    code: 'R-A-148383-1',
                },
            ],
        },
    } as unknown as Record<string, BunkerPageDefinition>;

    return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен обработать order у блоков', () => {
    const data = {
        '/auto_ru/ad/desktop/index': {
            ad1: {
                order: 2,
                sources: [
                    {
                        type: 'rtb',
                        code: 'R-A-148383-1',
                    },
                ],
            },
            ad2: {
                order: 1,
                sources: [
                    {
                        type: 'rtb',
                        code: 'R-A-148383-2',
                    },
                ],
            },
        },
    } as unknown as Record<string, BunkerPageDefinition>;

    return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен пропустить блок с неправильным конфигом', () => {
    const data = {
        '/auto_ru/ad/desktop/index': {
            top: 'bad declaation',
        },
    } as unknown as Record<string, BunkerPageDefinition>;

    return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
        .then((result) => {
            expect(result).toMatchSnapshot();
            expect(logger.error).toHaveBeenCalledWith({
                pageId: 'index',
                blockId: 'top',
                block: 'bad declaation',
            }, 'AD_BAD_BUNKER_DECLARATION');
        });
});

it('должен вернуть пустой результат для неизвестного типа страницы', () => {
    const data = {
        '/auto_ru/ad/desktop/index': {
            top: {
                sources: [
                    {
                        type: 'rtb',
                        code: 'R-A-148383-1',
                    },
                ],
            },
        },
    } as unknown as Record<string, BunkerPageDefinition>;

    return adBunker.get('desktop', 'index1', req, processRequest, experiments, data, '/auto_ru/ad')
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

describe('block_pathnames', () => {
    it('должен убрать блок, если url попадает в block_pathnames', () => {
        const data = {
            '/auto_ru/ad/desktop/index': {
                ad1: {
                    sources: [
                        {
                            type: 'rtb',
                            code: 'R-A-148383-1',
                        },
                    ],
                    block_pathnames: [
                        '/test/',
                    ],
                },
                ad2: {
                    sources: [
                        {
                            type: 'rtb',
                            code: 'R-A-148383-2',
                        },
                    ],
                },
            },
        } as unknown as Record<string, BunkerPageDefinition>;

        req.url = '/test/';

        return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('не должен убрать блок, если url не попадает в block_pathnames', () => {
        const data = {
            '/auto_ru/ad/desktop/index': {
                ad1: {
                    sources: [
                        {
                            type: 'rtb',
                            code: 'R-A-148383-1',
                        },
                    ],
                    block_pathnames: [
                        '/test/',
                    ],
                },
                ad2: {
                    sources: [
                        {
                            type: 'rtb',
                            code: 'R-A-148383-2',
                        },
                    ],
                },
            },
        } as unknown as Record<string, BunkerPageDefinition>;

        req.url = '/test1/';

        return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });
});

describe('раскрытие refs', () => {
    it('должен вернуть настройки рекламы', () => {
        const data = {
            '/auto_ru/ad/desktop/_common': {
                reloadable: {
                    key: 'value',
                },
                top: {
                    '&': 'reloadable',
                    reload: 45,
                },
            },
            '/auto_ru/ad/desktop/_sources': {
                rtb: {
                    type: 'rtb',
                    extParams: {
                        awaps_section: '29337',
                    },
                },
            },
            '/auto_ru/ad/desktop/index': {
                top: {
                    '&': 'top',
                    sources: [
                        {
                            '&': 'rtb',
                            code: 'R-A-148383-1',
                        },
                    ],
                },
            },
        } as unknown as Record<string, BunkerPageDefinition>;

        return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('не должен мутировать данные в бункере', () => {
        const bunkerMock = {
            '/auto_ru/ad/desktop/_common': {
                reloadable: {
                    key: 'value',
                },
                top: {
                    '&': 'reloadable',
                    reload: 45,
                },
            },
            '/auto_ru/ad/desktop/_sources': {
                rtb: {
                    type: 'rtb',
                    extParams: {
                        awaps_section: '29337',
                    },
                },
            },
            '/auto_ru/ad/desktop/index': {
                top: {
                    '&': 'top',
                    sources: [
                        {
                            '&': 'rtb',
                            code: 'R-A-148383-1',
                        },
                    ],
                },
            },
        } as unknown as Record<string, BunkerPageDefinition>;
        const bunkerMockCopy = cloneDeep(bunkerMock);

        return adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad')
            .then(() => {
                expect(bunkerMock).toEqual(bunkerMockCopy);
            });
    });

    it('должен вернуть настройки рекламы перетирая одинаковые ключи', () => {
        const data = {
            '/auto_ru/ad/desktop/_common': {
                reloadable: {
                    key: 'value',
                    reload: 30,
                },
                top: {
                    '&': 'reloadable',
                    reload: 45,
                },
            },
            '/auto_ru/ad/desktop/_sources': {
                rtb: {
                    type: 'rtb',
                    extParams: {
                        awaps_section: '29337',
                    },
                },
            },
            '/auto_ru/ad/desktop/index': {
                top: {
                    '&': 'top',
                    sources: [
                        {
                            '&': 'rtb',
                            code: 'R-A-148383-1',
                        },
                    ],
                },
            },
        } as unknown as Record<string, BunkerPageDefinition>;

        return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('должен вернуть настройки рекламы, когда указан одна только ссылка на блок', () => {
        const data = {
            '/auto_ru/ad/desktop/_sources': {
                rtb: {
                    type: 'rtb',
                    extParams: {
                        awaps_section: '29337',
                    },
                },
                journal1: {
                    '&': 'rtb',
                    code: 'R-A-148383-11',
                },
            },
            '/auto_ru/ad/desktop/index': {
                journal: [
                    'journal1',
                ],
            },
        } as unknown as Record<string, BunkerPageDefinition>;

        return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('должен вернуть настройки рекламы, раскрыв все refs', () => {
        const data = {
            '/auto_ru/ad/desktop/_sources': {
                'rtb-top': {
                    '&': 'rtb-native',
                },
                'rtb-native': {
                    '&': 'rtb',
                    'native': true,
                },
                rtb: {
                    '&': 'with-style',
                    type: 'rtb',
                    extParams: { awaps_section: '29337' },
                },
                'with-style': {
                    style: 'margin:20px',
                },
            },
            '/auto_ru/ad/desktop/index': {
                journal: [
                    'rtb-top',
                ],
            },
        } as unknown as Record<string, BunkerPageDefinition>;

        return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('должен вернуть пустой sources, если не найден указанный блок', () => {
        const data = {
            '/auto_ru/ad/desktop/_sources': {
                journal1: {
                    type: 'rtb',
                    code: 'R-A-148383-11',
                },
            },
            '/auto_ru/ad/desktop/index': {
                journal: [
                    'journal2',
                ],
            },
        } as unknown as Record<string, BunkerPageDefinition>;

        return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('должен вернуть ошибку, если не найден указанный блок по &', () => {
        const data = {
            '/auto_ru/ad/desktop/_sources': {
                journal: {
                    '&': 'rtb',
                    code: 'R-A-148383-11',
                },
            },
            '/auto_ru/ad/desktop/index': {
                journal: [
                    'journal',
                ],
            },
        } as unknown as Record<string, BunkerPageDefinition>;

        function test() {
            adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad');
        }

        expect(test).toThrow('[vertis-ads] There is no definition for source "rtb"');
    });
});

describe('серверный запросы', () => {
    describe('direct', () => {
        it('должен подготовить запрос за данными', () => {
            const data = {
                '/auto_ru/ad/desktop/index': {
                    c3: {
                        sources: [
                            {
                                type: 'direct',
                                sections: [
                                    'premium',
                                    'direct',
                                ],
                                method: 'getStaticContextDirectJSON',
                                params: {
                                    pageId: '151547',
                                },
                                groupKey: 'c3',
                            },
                        ],
                    },
                },
            } as unknown as Record<string, BunkerPageDefinition>;

            return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
                .then((result) => {
                    expect(result).toMatchSnapshot();
                });
        });

        it('не должен дублировать запросы и одним groupKey', () => {
            const data = {
                '/auto_ru/ad/desktop/index': {
                    c3: {
                        sources: [
                            {
                                type: 'direct',
                                sections: [
                                    'premium',
                                    'direct',
                                ],
                                method: 'getStaticContextDirectJSON',
                                params: {
                                    pageId: '151547',
                                },
                                groupKey: 'special',
                            },
                        ],
                    },
                    c4: {
                        sources: [
                            {
                                type: 'direct',
                                sections: [
                                    'premium',
                                    'direct',
                                ],
                                method: 'getStaticContextDirectJSON',
                                params: {
                                    pageId: '151547',
                                },
                                groupKey: 'special',
                            },
                        ],
                    },
                },
            } as unknown as Record<string, BunkerPageDefinition>;

            return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
                .then((result) => {
                    expect(result).toMatchSnapshot();
                    expect(processRequest).toHaveBeenCalledTimes(1);
                });
        });
    });

    describe('rtb', () => {
        it('должен подготовить запрос за данными', () => {
            const data = {
                '/auto_ru/ad/desktop/index': {
                    top: {
                        sources: [
                            {
                                type: 'rtb',
                                code: 'R-A-148383-1',
                                data: {
                                    meta: {
                                        method: 'getDirectS2S',
                                        params: {
                                            pageId: '148790',
                                            'imp-id': '20',
                                        },
                                    },
                                    widget_settings: {
                                        method: 'getDirectWidgetSettings',
                                        params: {
                                            'imp-id': '148790-20',
                                        },
                                    },
                                },
                            },
                        ],
                    },
                },
            } as unknown as Record<string, BunkerPageDefinition>;

            return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
                .then((result) => {
                    expect(result).toMatchSnapshot();
                });
        });

        it('должен добавить общий partner-stat-id из experiments', () => {
            const data = {
                '/auto_ru/ad/desktop/index': {
                    top: {
                        sources: [
                            {
                                type: 'rtb',
                                code: 'R-A-148383-1',
                                data: {
                                    meta: {
                                        method: 'getDirectS2S',
                                        params: {
                                            pageId: '148790',
                                            'imp-id': '20',
                                        },
                                    },
                                    widget_settings: {
                                        method: 'getDirectWidgetSettings',
                                        params: {
                                            'imp-id': '148790-20',
                                        },
                                    },
                                },
                            },
                        ],
                    },
                },
            } as unknown as Record<string, BunkerPageDefinition>;

            return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
                .then((result) => {
                    expect(result).toMatchSnapshot();
                });
        });

        it('должен добавить экспериментальный partner-stat-id', () => {
            const data = {
                '/auto_ru/ad/desktop/index': {
                    top: {
                        stat_id_by_exp: {
                            exp1: '456',
                            exp2: '789',
                        },
                        sources: [
                            {
                                type: 'rtb',
                                code: 'R-A-148383-1',
                                data: {
                                    meta: {
                                        method: 'getDirectS2S',
                                        params: {
                                            pageId: '148790',
                                            'imp-id': '20',
                                        },
                                    },
                                    widget_settings: {
                                        method: 'getDirectWidgetSettings',
                                        params: {
                                            'imp-id': '148790-20',
                                        },
                                    },
                                },
                            },
                        ],
                    },
                },
            } as unknown as Record<string, BunkerPageDefinition>;

            experiments.has = (exp) => exp === 'exp1';
            return adBunker.get('desktop', 'index', req, processRequest, experiments, data, '/auto_ru/ad')
                .then((result) => {
                    expect(result).toMatchSnapshot();
                });
        });
    });
});

describe('обновление данных', () => {
    let bunkerMock: Record<string, BunkerPageDefinition>;
    beforeEach(() => {
        bunkerMock = {
            '/auto_ru/ad/desktop/index': {
                top: {
                    sources: [
                        {
                            type: 'rtb',
                            code: 'desktopCode',
                        },
                    ],
                },
            },
            '/auto_ru/ad/mobile/index': {
                top: {
                    sources: [
                        {
                            type: 'rtb',
                            code: 'mobileCode',
                        },
                    ],
                },
            },
        } as unknown as Record<string, BunkerPageDefinition>;
    });

    it('не должен пересчитать данные, если данные в бункере не поменялись', () => {
        return adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad')
            .then(() => {
                return adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad');
            })
            .then(() => {
                expect(adBunker._exposeRefs).toHaveBeenCalledTimes(1);
            });
    });

    it('должен пересчитать данные при обновлении данных в бункере', () => {
        return adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad')
            .then(() => {
                return adBunker.get('desktop', 'index', req, processRequest, experiments, { ...bunkerMock }, '/auto_ru/ad');
            })
            .then(() => {
                expect(adBunker._exposeRefs).toHaveBeenCalledTimes(2);
            });
    });

    it('должен посчитать данные при смене платформы', () => {
        return Promise.all([
            adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad'),
            adBunker.get('mobile', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad'),
            adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad'),
            adBunker.get('mobile', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad'),
        ])
            .then((results) => {
                // проверяем кеш
                expect(adBunker._exposeRefs).toHaveBeenCalledTimes(2);
                expect(results[2].settings.top.sources[0]).toEqual({
                    type: 'rtb',
                    code: 'desktopCode',
                });
                expect(results[3].settings.top.sources[0]).toEqual({
                    type: 'rtb',
                    code: 'mobileCode',
                });
            });
    });
});

describe('эксперименты', () => {
    let bunkerMock: Record<string, BunkerPageDefinition>;
    beforeEach(() => {
        bunkerMock = {
            '/auto_ru/ad/desktop/_common': {
                reloadable: {
                    reload: 35,
                },
                r1: {
                    '&': 'reloadable',
                    resize: true,
                    reload: 40,
                    scrollReload: 2000,
                },
            },
            '/auto_ru/ad/desktop/_sources': {
                rtb: {
                    type: 'rtb',
                },
                rtb1: {
                    type: 'rtb1',
                },
                rtb2: {
                    type: 'rtb2',
                },
            },
            '/auto_ru/ad/desktop/index': {
                top: {
                    experiments: {
                        exp1: {
                            reload: 35,
                            sources: [
                                {
                                    '&': 'rtb1',
                                    code: 'exp1-code',
                                },
                            ],
                        },
                        exp2: {
                            sources: [
                                {
                                    '&': 'rtb2',
                                    code: 'exp2-code',
                                },
                            ],
                        },
                        exp3: {
                            reload: 50,
                            scrollReload: 1500,
                            '&': 'r1',
                            sources: [
                                {
                                    '&': 'rtb',
                                    code: [
                                        'R-A-148422-7',
                                        'R-A-148422-4',
                                    ],
                                },
                            ],
                        },
                    },
                    reload: 45,
                    sources: [
                        {
                            '&': 'rtb',
                            code: 'no-exp-code',
                        },
                    ],
                },
            },
        } as unknown as Record<string, BunkerPageDefinition>;
    });

    it('должен вернуть настройки рекламы без эксперимента', () => {
        return adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('должен вернуть настройки рекламы из эксперимента exp1', () => {
        experiments.has = (exp) => exp === 'exp1';

        return adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('должен вернуть настройки рекламы из эксперимента exp2', () => {
        experiments.has = (exp) => exp === 'exp2';

        return adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });

    it('должен вернуть настройки рекламы из эксперимента exp3', () => {
        experiments.has = (exp) => exp === 'exp3';

        return adBunker.get('desktop', 'index', req, processRequest, experiments, bunkerMock, '/auto_ru/ad')
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });
});
