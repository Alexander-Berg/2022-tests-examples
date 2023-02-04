import { getSiteBadgeParams } from '../getSiteBadgeParams';

import { ALL_SPECIAL_PROPOSALS } from '../../__tests__/mocks';

describe('getSiteBadgeParams', () => {
    it('Возвращает пустой объект', () => {
        expect(getSiteBadgeParams({ specialProposals: undefined })).toMatchSnapshot();
    });

    it('Возвращает объект со всеми вариантами', () => {
        expect(getSiteBadgeParams({ specialProposals: ALL_SPECIAL_PROPOSALS })).toMatchSnapshot();
    });
});
