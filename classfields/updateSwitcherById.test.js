const updateSwitcherByIdTest = require('./updateSwitcherById');

it('должен вернуть корректные объект', () => {
    expect(updateSwitcherByIdTest('newCarsSwitcher', { isActive: true, isDisabled: false }))
        .toEqual({
            type: 'TRADE_IN_UPDATE_SWITCHER_BY_ID',
            payload: {
                id: 'newCarsSwitcher',
                params: {
                    isActive: true,
                    isDisabled: false,
                },
            },
        });
});
