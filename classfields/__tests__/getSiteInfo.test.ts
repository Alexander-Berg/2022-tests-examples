import { getSiteInfo } from '../getSiteInfo';

describe('getSiteInfo', () => {
    it('Возвращает корректый объект', () => {
        expect(getSiteInfo({ siteId: 1 })).toMatchSnapshot();
    });
});
