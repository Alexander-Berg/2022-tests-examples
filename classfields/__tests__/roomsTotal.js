const {
    TRANSLIT_VALUES, idToTranslit, idToTranslitRooms, translitToId, renewOutdatedValue
} = require('../lib/roomsTotal');

const ROOMS_TOTAL_ACTUAL_REGEXP = new RegExp('/(studiya|odnokomnatnaya|dvuhkomnatnaya|tryohkomnatnaya|4-i-bolee|' +
    '(studiya)?([1-3,]*(4-i-bolee)?-komnatnie)?)/');

describe('check hash', () => {
    it('should have 1023 values in hash', () => {
        expect(TRANSLIT_VALUES.length).toBe(1023);
    });

    it('should ID_TO_TRANSLIT_HASH be inverse for TRANSLIT_TO_ID_HASH', () => {
        for (const translit of TRANSLIT_VALUES) {
            expect(idToTranslit(translitToId(translit))).toBe(translit);
        }
    });

    it('should all outdated TRANSLIT_VALUES be updated', () => {
        const conflicts = [];

        TRANSLIT_VALUES.forEach(translit => {
            const isOutdated = ! ROOMS_TOTAL_ACTUAL_REGEXP.test('/' + translit + '/');
            const updatedTranslit = renewOutdatedValue(translit);

            if (isOutdated && updatedTranslit === translit) {
                conflicts.push({
                    translit,
                    err: 'Outdated, not updated'
                });
            }

            if (! isOutdated && updatedTranslit !== translit) {
                conflicts.push({
                    translit,
                    updated: updatedTranslit,
                    err: 'Updated, not outdated'
                });
            }
        });

        expect(conflicts).toEqual([]);
    });

    it('should return valid id by traslit key', () => {
        const testCases = [
            [ '1,2,3,4,6,7-i-bolee-komnatnie,svobodnaya-planirovka', '1,2,3,4,6,OPEN_PLAN,PLUS_7'.split(',') ],
            [ '3,4,5,4-i-bolee,7-i-bolee-komnatnie', '3,4,5,PLUS_4,PLUS_7'.split(',') ],
            [ 'shestikomnatnaya7', undefined ],
            [ undefined, undefined ],
            [ '4-i-bolee', 'PLUS_4' ],
            [
                'studiya,1,2,3,4-i-bolee-komnatnie,svobodnaya-planirovka',
                '1,2,3,OPEN_PLAN,PLUS_4,STUDIO'.split(',')
            ],
            [ '', undefined ]
        ];

        for (const [ translit, expectedResult ] of testCases) {
            expect(translitToId(translit)).toEqual(expectedResult);
        }
    });

    it('should transform outdated values', () => {
        const testCases = [
            [ '2,3,4,4-i-bolee-komnatnie,svobodnaya-planirovka', 'studiya,2,3,4-i-bolee-komnatnie' ],
            [ '1,2,3,4-komnatnie', '1,2,3,4-i-bolee-komnatnie' ],
            [ '1,3-komnatnie', '1,3-komnatnie' ],
            [ 'dvuhkomnatnaya', 'dvuhkomnatnaya' ],
            [ '4-i-boleee', undefined ],
            [ '1,7-i-bolee-komnatnie,svobodnaya-planirovka', 'studiya,1,4-i-bolee-komnatnie' ],
            [ '', undefined ],
            [ undefined, undefined ]
        ];

        for (const [ translit, expectedResult ] of testCases) {
            expect(renewOutdatedValue(translit)).toBe(expectedResult);
        }
    });

    it('Должен возвращать валидный урл для 1 типа или 2 соседних типов комнатности', () => {
        const testCases = [
            [ '1', 'odnokomnatnaya' ],
            [ '1,2'.split(','), '1,2-komnatnie' ],
            [ '3,PLUS_4'.split(','), '3,4-i-bolee-komnatnie' ],
            [ '1,3'.split(','), undefined ],
            [ '1,2,3'.split(','), undefined ],
            [ 'STUDIO'.split(','), 'studiya' ],
            [ 'STUDIO,1'.split(','), 'studiya,1-komnatnie' ],
            [ 'STUDIO,2'.split(','), undefined ],
            [ 'STUDIO,2,PLUS_4'.split(','), undefined ],
            [ 'STUDIO,1,2,3,PLUS_4'.split(','), undefined ],
        ];

        for (const [ translit, expectedResult ] of testCases) {
            expect(idToTranslitRooms(translit)).toBe(expectedResult);
        }
    });
});
