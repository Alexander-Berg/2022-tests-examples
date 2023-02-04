import { isNeedActivationButton } from '../offers';

const cases = [
    [ 'free, active', { placement: { free: {} }, status: 'active' }, false ],
    [ 'free, inactive', { placement: { free: {} }, status: 'inactive' }, false ],
    [ 'quota, active', { placement: { quota: {} }, status: 'active' }, false ],
    [ 'quota, inactive', { placement: { quota: {} }, status: 'inactive' }, true ],
    [ 'quota, moderation', { placement: { quota: {} }, status: 'medration' }, true ],
    [ 'paid === false, active', { placement: { paymentRequired: {} }, status: 'active' }, true ],
    [ 'paid === false, moderation', { placement: { paymentRequired: {} }, status: 'moderation' }, true ],
    [ 'paid === true, active', { placement: { paymentRequired: { paid: true } }, status: 'active' }, false ],
    [ 'paid === true, moderation', { placement: { paymentRequired: { paid: true } }, status: 'moderation' }, false ],
    [ 'paid === false inactive', { placement: { paymentRequired: {} }, status: 'inactive' }, true ],
    [ 'paid === true inactive', { placement: { paymentRequired: { paid: true } }, status: 'inactive' }, false ]
];

describe('isNeedActivationButton', () => {
    cases.forEach(testCase => {
        const [ name, input, output ] = testCase;
        const { placement, status } = input;

        it(name, () => expect(isNeedActivationButton(placement, status)).toBe(output));
    });
});
