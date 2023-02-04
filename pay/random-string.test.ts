import { randomString } from './random-string';

test('should return random string specific length', () => {
    expect(randomString(10)).toMatch(/[a-z0-9]{10}/);
});
