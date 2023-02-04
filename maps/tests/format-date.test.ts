import formatDate from '../lib/format-date';

test('formatDate from common string', () => {
    expect(formatDate('2018-05-15T11:21:33.328204000Z')).toBe('14:21 15.05.18');
});

test('formatDate from minutes < 10', () => {
    expect(formatDate('2018-05-15T11:01:33.328204000Z')).toBe('14:01 15.05.18');
});

test('formatDate from empty string', () => {
    expect(formatDate('')).toBe('Invalid Date');
});

test('formatDate from not valid  dateString', () => {
    expect(formatDate('awdawdawd')).toBe('Invalid Date');
});
