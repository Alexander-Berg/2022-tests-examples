const {
    postMatchParamsTransform,
    preBuildParamsTransform,
} = require('./paramsTransformer');

describe('preBuildParamsTransform: трансформация параметров ЧПУ в id', () => {
    it('игнорирует пустые параметры', () => {
        expect(postMatchParamsTransform({
            param1: '',
            param2: null,
            param3: undefined,
            param: 'value',
        })).toEqual({
            param: 'value',
        });
    });

    it('преобразует массив из одного значения в само значение', () => {
        expect(postMatchParamsTransform({
            param: [ 'value' ],
        })).toEqual({
            param: 'value',
        });
    });

    it('массив из нескольких значений оставляет как есть', () => {
        expect(postMatchParamsTransform({
            seo_param: [ 'value1', 'value2' ],
            param2: [ 'value1', 'value2' ],
        })).toEqual({
            param: [ 'value1', 'value2' ],
            param2: [ 'value1', 'value2' ],
        });
    });

    it('преобразует ЧПУ значения в id по словарю', () => {
        expect(postMatchParamsTransform({
            seo_mark: 'ford',
        })).toEqual({
            mark: 'FORD',
        });
    });

    it('преобразует ЧПУ значения в id из старого словаря (удаленные из базы)', () => {
        expect(postMatchParamsTransform({
            seo_categoryId: 'amortizator-v-sbore',
        })).toEqual({
            categoryId: '6932',
        });
    });

    it('прокидывает параметры без словарей как есть', () => {
        expect(postMatchParamsTransform({
            seo_generation: '1234532',
        })).toEqual({
            generation: '1234532',
        });
    });

    it('мапит по старым словарям', () => {
        expect(postMatchParamsTransform({
            mark: 'ford',
        }, getRouteMock())).toEqual({
            mark: 'FORD',
        });

        expect(postMatchParamsTransform({
            mark: 'FORD',
        }, getRouteMock())).toEqual({
            mark: 'FORD',
        });
    });
});

function getRouteMock({
    conditions = {},
    mainParamsMap = {},
} = {}) {
    return {
        conditions: {
            seo_mark: [ 'ford', 'mark2' ],
            ...conditions,
        },
        mainParamsMap: {
            seo_generation: true,
            seo_mark: true,
            ...mainParamsMap,
        },
    };
}

describe('preBuildParamsTransform: трансформация параметров в ЧПУ значения', () => {
    it('игнорирует пустые параметры', () => {
        expect(preBuildParamsTransform({
            param1: '',
            param2: null,
            param3: undefined,
            param: 'value',
        }, getRouteMock())).toEqual({
            param: 'value',
        });
    });

    it('преобразует массив из одного значения в само значение', () => {
        expect(preBuildParamsTransform({
            param: [ 'value' ],
        }, getRouteMock())).toEqual({
            param: 'value',
        });
    });

    it('массив из нескольких значений оставляет как есть', () => {
        expect(preBuildParamsTransform({
            param: [ 'value1', 'value2' ],
            param2: [ 'value1', 'value2' ],
        }, getRouteMock())).toEqual({
            param: [ 'value1', 'value2' ],
            param2: [ 'value1', 'value2' ],
        });
    });

    it('неизвестные сео параметры пропускает как есть', () => {
        expect(preBuildParamsTransform({
            seo_param: 'value',
        }, getRouteMock())).toEqual({
            seo_param: 'value',
        });
    });

    it('прокидывает поколение как есть в ЧПУ', () => {
        expect(preBuildParamsTransform({
            generation: '12sdfd',
        }, getRouteMock())).toEqual({
            seo_generation: '12sdfd',
        });
    });

    it('все, что подходит по кондишнам и есть в ЧПУ параметрах, кладется в ЧПУ', () => {
        expect(preBuildParamsTransform({
            mark: 'FORD',
        }, getRouteMock())).toEqual({
            seo_mark: 'ford',
        });
    });

    it('все, что не подходит по кондишнам и есть в ЧПУ параметрах, кладется в GET', () => {
        expect(preBuildParamsTransform({
            mark: 'bmw',
        }, getRouteMock())).toEqual({
            mark: 'bmw',
        });
    });
});
