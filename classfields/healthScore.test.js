const preparer = require('./healthScore');

it('должен вернуть результат c value, если пришёл скор', () => {
    const result = {
        report: { health_score: { score: 83 } },
    };

    const data = preparer({ result });

    expect(data).toEqual({
        key: 'Оценка ПроАвто',
        value: '83 балла',
    });
});

it('должен вернуть значение без value, если не пришёл скор', () => {
    const result = {
        report: { health_score: { } },
    };

    const data = preparer({ result });

    expect(data).toEqual({
        key: 'Оценка ПроАвто',
        value: null,
    });
});

it('должен вернуть значение без value, если не пришёл вообще весь объект скора', () => {
    const result = {
        report: {},
    };

    const data = preparer({ result });

    expect(data).toEqual({
        key: 'Оценка ПроАвто',
        value: null,
    });
});
