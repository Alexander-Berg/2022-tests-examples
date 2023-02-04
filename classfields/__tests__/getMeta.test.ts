import MockDate from 'mockdate';

import { MOCK_DATE } from '../../__tests__/mocks';

import { getMeta } from '../getMeta';

describe('getMeta', () => {
    beforeEach(() => {
        MockDate.set(MOCK_DATE);
    });

    it('Возвращает корректый объект', () => {
        expect(getMeta()).toMatchSnapshot();
    });
});
