import {expect} from 'chai';
import {
    INCORRECT_PUBLIC_ID,
    MAX_INTERNAL_ID,
    MIN_INTERNAL_ID,
    MAX_SEED,
    MIN_SEED,
    encodePublicId,
    decodePublicId
} from 'app/v1/helpers/public-id';

describe('public-id', () => {
    describe('encodePublicId', () => {
        it('should return incorrect public_id if seed is out of bounds', async () => {
            const internalId = '533863'; // Just a random valid id
            const outOfBoundsSeeds = [
                MIN_SEED - 1,
                MAX_SEED + 1
            ];

            for (const seed of outOfBoundsSeeds) {
                const actual = encodePublicId(internalId, seed);
                expect(actual).to.be.deep.equal(INCORRECT_PUBLIC_ID);
            }
        });

        it('should return incorrect public_id if id is out of bounds', async () => {
            const seed = 24853; // Just a random valid seed
            const outOfBoundsInternalIds = [
                `${MIN_INTERNAL_ID - 1}`,
                `${MAX_INTERNAL_ID + BigInt(1)}`
            ];

            for (const internalId of outOfBoundsInternalIds) {
                const actual = encodePublicId(internalId, seed);
                expect(actual).to.be.deep.equal(INCORRECT_PUBLIC_ID);
            }
        });

        it('should return correct public_id for ordinary seed and id value', async () => {
            const seed = 24853;
            const ordinaryInternalIds = [
                {internalId: '533863', expectedPublicId: '4RR_h5MV'},
                {internalId: '9748573082094262', expectedPublicId: '4bczLLUHnFTkFQ'}
            ];

            for (const {internalId, expectedPublicId} of ordinaryInternalIds) {
                const actual = encodePublicId(internalId, seed);
                expect(actual).to.be.deep.equal(expectedPublicId);
            }
        });

        it('should return correct public_id for edge seed values', async () => {
            const internalId = '533863';
            const outOfBoundsSeeds = [
                {seed: MIN_SEED, expectedPublicId: 'ABR_h5MA'},
                {seed: MAX_SEED, expectedPublicId: '_xR_h5P_'}
            ];

            for (const {seed, expectedPublicId} of outOfBoundsSeeds) {
                const actual = encodePublicId(internalId, seed);
                expect(actual).to.be.deep.equal(expectedPublicId);
            }
        });

        it('should return correct public_id for edge id values', async () => {
            const seed = 24853;
            const outOfBoundsInternalIds = [
                {internalId: `${MIN_INTERNAL_ID}`, expectedPublicId: '4TkwAAAV'},
                {internalId: `${2 ** 31 - 1}`, expectedPublicId: '4czhOT4V'},
                {internalId: `${2 ** 31}`, expectedPublicId: '4TkwAIAV'},
                {internalId: `${2 ** 32 - 1}`, expectedPublicId: '4czhOb4V'},
                {internalId: `${2 ** 32}`, expectedPublicId: '4TkwAACmfsZBFQ'},
                {internalId: `${MAX_INTERNAL_ID}`, expectedPublicId: '4TkwAAA5MACAFQ'}
            ];

            for (const {internalId, expectedPublicId} of outOfBoundsInternalIds) {
                const actual = encodePublicId(internalId, seed);
                expect(actual).to.be.deep.equal(expectedPublicId);
            }
        });

        it('should replace all the URL-unsafe symbols with its analogs', async () => {
            const urlUnsafeSamples = [
                {id: '468899846', seed: 31294}, // three pluses, '+sf+234+'
                {id: '1440627974', seed: 32319}, // three slashes, '/sf/234/'
                {id: '14576404177625169158', seed: 24283} // four slashes, '3sf/234//sf/23'
            ];

            for (const urlUnsafeSample of urlUnsafeSamples) {
                const publicId = encodePublicId(urlUnsafeSample.id, urlUnsafeSample.seed);
                expect(publicId).not.to.include('+');
                expect(publicId).not.to.include('/');
            }
        });
    });

    describe('decodePublicId', () => {
        it('should return undefined if public_id is out of bounds', async () => {
            const outOfBoundsPublicIds = [
                '',
                'GhA_',
                '8TewB6AV0nQ',
                '739hgc2-_HGK2934_'
            ];

            for (const publicId of outOfBoundsPublicIds) {
                const actual = decodePublicId(publicId);
                expect(actual).to.be.undefined;
            }
        });

        it('should return undefined if public_id value is invalid', async () => {
            const invalidPublicIds = [
                '~~~~~~~~', '~~~~~~~~~~~~~~',
                '""""""""', '""""""""""""""',
                '========', '==============',
                '........', '..............'
            ];

            for (const publicId of invalidPublicIds) {
                const actual = decodePublicId(publicId);
                expect(actual).to.be.undefined;
            }
        });

        it('should return correct id and seed for a valid public_id', async () => {
            const validPublicIds = [
                {
                    publicId: '////////', // We assume this is valid base64 string
                    expectedId: {id: 230538014, seed: 32767}
                },
                {
                    publicId: '++++++++', // We assume this is valid base64 string
                    expectedId: {id: 4022755278, seed: 31678}
                },
                {
                    publicId: '6k_wAeQp',
                    expectedId: {id: 3555392174, seed: 27177}
                },
                {
                    publicId: '78Fgd_2j',
                    expectedId: {id: 3956997544, seed: 28579}
                },
                {
                    publicId: '4bczLLUHnFTkFQ',
                    expectedId: {id: BigInt(9748573082094262), seed: 24853}
                }
            ];

            for (const {publicId, expectedId} of validPublicIds) {
                const actual = decodePublicId(publicId);
                expect(actual).to.be.deep.equal(expectedId);
            }
        });

        it('should return correct id and seed for edge seed values', async () => {
            const seedEdgeValuePublicIds = [
                {publicId: 'ABR_h5MA', expectedId: {id: 533863, seed: MIN_SEED}},
                {publicId: '_xR_h5P_', expectedId: {id: 533863, seed: MAX_SEED}}
            ];

            for (const {publicId, expectedId} of seedEdgeValuePublicIds) {
                const actual = decodePublicId(publicId);
                expect(actual).to.be.deep.equal(expectedId);
            }
        });

        it('should return correct id and seed for edge id values', async () => {
            const internalIdEdgeValuePublicIds = [
                {publicId: '4TkwAAAV', expectedId: {id: MIN_INTERNAL_ID, seed: 24853}},
                {publicId: '4czhOT4V', expectedId: {id: 2 ** 31 - 1, seed: 24853}},
                {publicId: '4TkwAIAV', expectedId: {id: 2 ** 31, seed: 24853}},
                {publicId: '4czhOb4V', expectedId: {id: 2 ** 32 - 1, seed: 24853}},
                {publicId: '4TkwAACmfsZBFQ', expectedId: {id: BigInt(2 ** 32), seed: 24853}},
                {publicId: '4TkwAAA5MACAFQ', expectedId: {id: MAX_INTERNAL_ID, seed: 24853}}
            ];

            for (const {publicId, expectedId} of internalIdEdgeValuePublicIds) {
                const actual = decodePublicId(publicId);
                expect(actual).to.be.deep.equal(expectedId);
            }
        });

        it('should restore a valid base64 message after it was converted to the URL-safe analog', async () => {
            const samples = [
                {publicId: '-sf-234-', id: 468899846, seed: 31294},
                {publicId: '_sf_234_', id: 3588111622, seed: 32319}
            ];

            for (const sample of samples) {
                const decodedPublicId = decodePublicId(sample.publicId);
                expect(decodedPublicId).not.to.be.undefined;
                expect(decodedPublicId!.id).to.be.equal(sample.id);
                expect(decodedPublicId!.seed).to.be.equal(sample.seed);
            }
        });
    });
});
