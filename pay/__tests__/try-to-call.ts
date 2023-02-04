import { tryToCall } from '../try-to-call';

describe('try-to-call', () => {
    describe('tryToCall', () => {
        it('should return the result of a function call', () => {
            const result = Symbol();
            const fn = () => result;

            const actual = tryToCall(fn);

            expect(actual).toBe(result);
        });

        it('should return the value "undefined" if the function threw an error', () => {
            const fn = () => {
                throw new Error();
            };

            const actual = tryToCall(fn);

            expect(actual).toBeUndefined();
        });
    });
});
