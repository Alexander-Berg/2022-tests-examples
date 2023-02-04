import { email as validateEmail } from '../index';

describe('common', () => {
    describe('validators', () => {
        it('email', () => {
            // @ts-ignore
            expect(validateEmail(undefined)).toBeFalsy();
            // @ts-ignore
            expect(validateEmail(null)).toBeFalsy();
            expect(validateEmail('')).toBeFalsy();
            expect(validateEmail('   ')).toBeFalsy();
            expect(validateEmail('test')).toBeFalsy();
            expect(validateEmail('test@test.com')).toBeTruthy();
            expect(validateEmail('test@test.com;test2@test2.com')).toBeTruthy();
            expect(validateEmail('test@test.com,test2@test2.com')).toBeTruthy();
            expect(validateEmail('test@test.com ; test2@test2.com')).toBeTruthy();
            expect(validateEmail('test@test.com , test2@test2.com')).toBeTruthy();
            expect(validateEmail('test@test.com , test2')).toBeFalsy();
            expect(validateEmail('test@test.com , ')).toBeTruthy();
        });
    });
});
