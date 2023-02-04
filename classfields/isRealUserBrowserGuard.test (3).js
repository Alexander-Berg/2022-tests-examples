const isRealUserBrowserGuard = require('./isRealUserBrowserGuard');

it('должен вернуть undefined, если isRobot === false', () => {
    expect(
        isRealUserBrowserGuard({
            context: { req: { isRobot: false } },
        }),
    ).toBeUndefined();
});

it('должен вернуть undefined, если isRobot не определен', () => {
    expect(
        isRealUserBrowserGuard({
            context: { req: {} },
        }),
    ).toBeUndefined();
});

it('должен вернуть ошибку, если isRobot === true', () => {
    function guard() {
        isRealUserBrowserGuard({
            context: { req: { isRobot: true } },
        });
    }

    expect(guard).toThrow();
});
