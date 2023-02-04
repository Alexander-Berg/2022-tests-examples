const isRealUserBrowserGuard = require('./isRealUserBrowserGuard');

it('должен вернуть undefined, если isRobot === false', () => {
    expect(
        isRealUserBrowserGuard({
            context: { req: { headers: {}, isRobot: false } },
        }),
    ).toBeUndefined();
});

it('должен вернуть undefined, если isRobot не определен', () => {
    expect(
        isRealUserBrowserGuard({
            context: { req: { headers: {} } },
        }),
    ).toBeUndefined();
});

it('должен вернуть ошибку, если isRobot === true', () => {
    function guard() {
        isRealUserBrowserGuard({
            context: { req: { headers: {}, isRobot: true } },
        });
    }

    expect(guard).toThrow();
});

it('должен вернуть ошибку, если isRobot === false и x-yandex-antirobot-degradation === 1', () => {
    function guard() {
        isRealUserBrowserGuard({
            context: {
                req: {
                    headers: { 'x-yandex-antirobot-degradation': '1' },
                    isRobot: false,
                },
            },
        });
    }

    expect(guard).toThrow();
});

it('должен вернуть undefined, если isRobot === false и x-yandex-antirobot-degradation === 0', () => {
    expect(
        isRealUserBrowserGuard({
            context: {
                req: {
                    headers: { 'x-yandex-antirobot-degradation': '0' },
                    isRobot: false,
                },
            },
        }),
    ).toBeUndefined();
});
