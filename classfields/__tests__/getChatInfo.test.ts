import { CHAT } from '../../__tests__/mocks';

import { getChatInfo } from '../getChatInfo';

describe('getChatInfo', () => {
    it('Возвращает ничего', () => {
        expect(getChatInfo({ chat: undefined })).toMatchSnapshot();
    });

    it('Возвращает заполненный объект', () => {
        expect(getChatInfo({ chat: CHAT })).toMatchSnapshot();
    });
});
