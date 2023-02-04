import { ClientType } from 'realty-core/types/eventLog';

import { getClientInfo } from '../getClientInfo';

describe('getClientInfo', () => {
    it('Возвращает корректый объект', () => {
        expect(getClientInfo({ clientType: ClientType.DESKTOP })).toMatchSnapshot();
    });
});
