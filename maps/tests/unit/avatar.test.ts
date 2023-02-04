import {expect} from 'chai';
import {getAvatarUrl} from 'app/v1/helpers/avatar';

describe('getAvatarUrl', () => {
    it('should return empty string when avatarId is undefined', () => {
        const actual = getAvatarUrl();

        expect(actual).to.equal('');
    });

    it('should return correct avatar url', () => {
        const actual = getAvatarUrl('some-avatar-id');

        expect(actual).to.equal('https://avatars.mds.yandex.net/get-yapic/some-avatar-id/{size}');
    });
});
