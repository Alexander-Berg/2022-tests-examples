const isAvailableLoyaltyProgram = require('./isAvailableLoyaltyProgram');

const withApplyMock = require('../mocks/withApply.mock');
const withoutApplyMock = require('../mocks/withoutApply.mock');

it('должен отдавать true, если удовлетворяются условия лояльности', () => {
    const state = {
        loyalty: withApplyMock,
    };

    expect(isAvailableLoyaltyProgram(state)).toBe(true);
});

it('должен отдавать false, если не удовлетворяются условия лояльности', () => {
    const state = {
        loyalty: withoutApplyMock,
    };

    expect(isAvailableLoyaltyProgram(state)).toBe(false);
});

it('должен отдавать false, если loyalty = {}', () => {
    const state = {
        loyalty: {},
    };

    expect(isAvailableLoyaltyProgram(state)).toBe(false);
});
