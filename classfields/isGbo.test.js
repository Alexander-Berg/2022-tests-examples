const isGbo = require('./isGbo');

it('должен вернуть true', () => {
    const name = isGbo({
        vehicle_info: {
            equipment: { gbo: true },
        },
    });

    expect(name).toEqual(true);
});

it('должен вернуть false', () => {
    const name = isGbo({});

    expect(name).toEqual(false);
});
