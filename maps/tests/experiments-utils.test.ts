import * as experimentsUtils from 'server/utils/experiments-utils';

describe('Утилиты для работы с экспериментами', () => {
    describe('Парсинг параметров из конфига', () => {
        test('Мерджит объекты', () => {
            expect(
                experimentsUtils.parseUIExperimentsFromConfig({
                    pron: ['extendConfig'],
                    params: [
                        '{"extendConfig":{"banner":{"blockIds":{"taxi":"111"}}}}',
                        '{"extendConfig":{"banner":{"blockIds":{"maps":"222"}}}}'
                    ]
                })
            ).toEqual({
                extendConfig: {
                    banner: {blockIds: {taxi: '111', maps: '222'}}
                }
            });
        });
        test('Заменяет массивы', () => {
            expect(
                experimentsUtils.parseUIExperimentsFromConfig({
                    pron: ['serviceBar'],
                    params: ['{"serviceBar":["metro","profile"]}', '{"serviceBar":["masstransit","traffic"]}']
                })
            ).toEqual({
                serviceBar: ['masstransit', 'traffic']
            });
        });
        test('Заменяет примитивы', () => {
            expect(
                experimentsUtils.parseUIExperimentsFromConfig({
                    pron: ['extendConfig'],
                    params: ['{"extendConfig":"111"}', '{"extendConfig":"222"}']
                })
            ).toEqual({
                extendConfig: '222'
            });
        });
    });
});
