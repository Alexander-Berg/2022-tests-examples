const formErrors = require('./formErrors');

import mockFormFieldsCarsEvaluation from 'auto-core/react/dataDomain/formFields/mocks/carsEvaluation';

it('должен вернуть ошибку при незаполненной причине оценки в экспе', () => {
    expect(formErrors.getErrorMessages(
        mockFormFieldsCarsEvaluation.data,
        'evaluation',
        'cars',
        {},
        false,
        'general',
        {},
        true,
    )).toEqual({
        reason: {
            humanReadableName: undefined,
            message: 'Укажите причину оценки',
        },
    });
});

it('не должен вернуть ошибку при незаполненной причине оценки вне экспа', () => {
    expect(formErrors.getErrorMessages(
        mockFormFieldsCarsEvaluation.data,
        'evaluation',
        'cars',
        {},
        false,
        'general',
        {},
        false,
    )).toEqual({});
});
