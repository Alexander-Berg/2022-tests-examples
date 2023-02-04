import { PHONE_NUMBER } from '../../__tests__/mocks';

import { getPhoneInfo } from '../getPhoneInfo';

describe('getPhoneInfo', () => {
    it('Возвращает ничего', () => {
        expect(getPhoneInfo({ phone: undefined })).toMatchSnapshot();
    });

    it('Возвращает заполненный объект', () => {
        expect(getPhoneInfo({ phone: PHONE_NUMBER, redirectId: PHONE_NUMBER })).toMatchSnapshot();
    });
});
