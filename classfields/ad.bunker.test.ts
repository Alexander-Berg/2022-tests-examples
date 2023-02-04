/* Базовый смоук на интеграцию с @vertis/ads */
jest.mock('../luster-bunker', () => {
    return {
        getNode: jest.fn(() => ({})),
    };
});

jest.mock('@vertis/pino', () => {
    const mock = {
        child: () => mock,
        error: jest.fn(() => {}),
    };
    return mock;
});

import type { ExperimentsProvider, ServerRequestProcessor } from '@vertis/ads/build/server/lib/ad.bunker';
import VertisAdBunker from '@vertis/ads/build/server/lib/ad.bunker';
import logger from '@vertis/pino';

import type { THttpRequest } from 'auto-core/http';

import bunkerData from '../luster-bunker';

import adBunker from './ad.bunker';

let mathRandomSpy: jest.SpyInstance;
let bunkerExposeRefsSpy: jest.SpyInstance;
let experiments: ExperimentsProvider;
let processRequest: ServerRequestProcessor;
let req: THttpRequest;
beforeEach(() => {
    experiments = {
        has: () => false,
        statId: '123',
    };
    VertisAdBunker.resetData();
    processRequest = jest.fn((request) => {
        return Promise.resolve({ ...request });
    });
    mathRandomSpy = jest.spyOn(global.Math, 'random').mockReturnValue(0.123456789);
    bunkerExposeRefsSpy = jest.spyOn(VertisAdBunker, '_exposeRefs');
    req = {} as THttpRequest;
});

afterEach(() => {
    mathRandomSpy.mockRestore();
    bunkerExposeRefsSpy.mockRestore();
});

it('должен вернуть настройки рекламы без раскрытия refs', () => {
    bunkerData.getNode.mockImplementation(() => {
        return {
            '/auto_ru/ad/desktop/index': {
                top: {
                    sources: [
                        {
                            type: 'rtb',
                            async: true,
                            extParams: {
                                awaps_section: '29337',
                            },
                            code: 'R-A-148383-1',
                        },
                    ],
                },
            },
        };
    });

    return adBunker.get('desktop', 'index', req, processRequest, experiments)
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен пропустить блок с неправильным конфигом', () => {
    bunkerData.getNode.mockImplementation(() => {
        return {
            '/auto_ru/ad/desktop/index': {
                top: 'bad declaation',
            },
        };
    });

    return adBunker.get('desktop', 'index', req, processRequest, experiments)
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
    bunkerData.getNode.mockImplementation(() => {
        return {
            '/auto_ru/ad/desktop/index': {
                top: {
                    sources: [
                        {
                            type: 'rtb',
                            async: true,
                            code: 'R-A-148383-1',
                        },
                    ],
                },
            },
        };
    });

    return adBunker.get('desktop', 'index1', req, processRequest, experiments)
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

describe('раскрытие refs', () => {
    it('должен вернуть настройки рекламы', () => {
        bunkerData.getNode.mockImplementation(() => {
            return {
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
                        async: true,
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
                                async: true,
                                code: 'R-A-148383-1',
                            },
                        ],
                    },
                },
            };
        });

        return adBunker.get('desktop', 'index', req, processRequest, experiments)
            .then((result) => {
                expect(result).toMatchSnapshot();
            });
    });
});

describe('серверный запросы', () => {
    describe('direct', () => {
        it('должен подготовить запрос за данными', () => {
            bunkerData.getNode.mockImplementation(() => {
                return {
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
                };
            });

            return adBunker.get('desktop', 'index', req, processRequest, experiments)
                .then((result) => {
                    expect(result).toMatchSnapshot();
                });
        });
    });

    describe('rtb', () => {
        it('должен подготовить запрос за данными', () => {
            bunkerData.getNode.mockImplementation(() => {
                return {
                    '/auto_ru/ad/desktop/index': {
                        top: {
                            sources: [
                                {
                                    type: 'rtb',
                                    async: true,
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
                };
            });

            return adBunker.get('desktop', 'index', req, processRequest, experiments)
                .then((result) => {
                    expect(result).toMatchSnapshot();
                });
        });
    });
});
