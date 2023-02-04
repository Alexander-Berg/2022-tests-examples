import { SITE_PLAN } from '../../__tests__/mocks';

import { getSitePlanInfo } from '../getSitePlanInfo';

describe('getSitePlanInfo', () => {
    it('Возвращает пустой объект', () => {
        expect(getSitePlanInfo({ plan: undefined })).toMatchSnapshot();
    });

    it('Возвращает корректый объект', () => {
        expect(getSitePlanInfo({ plan: SITE_PLAN })).toMatchSnapshot();
    });
});
