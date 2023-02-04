import { getCommonStoreParams } from '../getCommonStoreParams';

import { CORE_STORE, MOBILE_CORE_STORE, TABLET_CORE_STORE } from '../../__tests__/mocks';

describe('getCommonStoreParams', () => {
    it('Возвращает корректый объект для десктопа', () => {
        expect(getCommonStoreParams(CORE_STORE)).toMatchSnapshot();
    });

    it('Возвращает корректый объект для планшета', () => {
        expect(getCommonStoreParams(TABLET_CORE_STORE)).toMatchSnapshot();
    });

    it('Возвращает корректый объект для тача', () => {
        expect(getCommonStoreParams(MOBILE_CORE_STORE)).toMatchSnapshot();
    });
});
