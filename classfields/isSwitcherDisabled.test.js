const isSwitcherDisabled = require('./isSwitcherDisabled');

it('должен вернуть true, если usedCarsSwitcher.isDisabled', () => {
    const result = isSwitcherDisabled({
        isDisabled: true,
    });

    expect(result).toBe(true);
});
