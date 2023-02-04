const { getUserV2toV1adapter, updateUserV1toV2adapter } = require('../realty3User');

const mocks = require('./mocks');

describe('realty3UserAdapters', () => {
    describe('getAdapter', () => {
        it('correct convert juridical person', () => {
            expect(getUserV2toV1adapter(mocks.get.juridical.v2)).toStrictEqual(mocks.get.juridical.v1);
        });

        it('correct convert natural person', () => {
            expect(getUserV2toV1adapter(mocks.get.natural.v2)).toStrictEqual(mocks.get.natural.v1);
        });

        it('correct convert unknown payment type to natural', () => {
            expect(getUserV2toV1adapter(mocks.get.unknownPT.v2)).toStrictEqual(mocks.get.unknownPT.v1);
        });

        it('correct convert user without name email and phones', () => {
            expect(getUserV2toV1adapter(mocks.get.withoutNameAndEmailAndPhones.v2))
                .toStrictEqual(mocks.get.withoutNameAndEmailAndPhones.v1);
        });
    });

    describe('updateAdapter', () => {
        it('correct convert juridical person', () => {
            expect(updateUserV1toV2adapter(mocks.updade.juridical.v1)).toStrictEqual(mocks.updade.juridical.v2);
        });

        it('correct convert natural person', () => {
            expect(updateUserV1toV2adapter(mocks.updade.natural.v1)).toStrictEqual(mocks.updade.natural.v2);
        });
    });
});
