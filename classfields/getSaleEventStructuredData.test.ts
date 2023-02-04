import MockDate from 'mockdate';

import getSaleEventStructuredData from './getSaleEventStructuredData';

describe('Разметка sale event', () => {
    beforeEach(() => {
        MockDate.set('2021-12-01T21:00');
    });

    afterEach(() => {
        MockDate.reset();
    });

    it('Должен вернуть валидную разметку', () => {
        expect(getSaleEventStructuredData()).toMatchSnapshot();
    });
});
