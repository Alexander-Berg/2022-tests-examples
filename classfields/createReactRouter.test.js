const Susanin = require('@vertis/susanin');
const createReactRouter = require('auto-core/router/libs/createReactRouter');

let router;
beforeEach(() => {
    const susanin = new Susanin();
    susanin.addRoute({
        name: 'index',
        pattern: '/',
        defaults: {
            category: 'cars',
            section: 'all',
        },
        data: {
            controller: 'index',
            react: true,
        },
    });

    router = createReactRouter(susanin);
});

it('should match "index" for "/"', () => {
    const route = router({ pathname: '/' });
    if (!route) {
        throw 'NOT_FOUND';
    }
    expect(route.route.getName()).toEqual('index');
});

it('should match "index" for "/1/"', () => {
    const route = router({ pathname: '/1/' });
    if (!route) {
        throw 'NOT_FOUND';
    }
    expect(route.route.getName()).toEqual('index');
});

it('should match "index" for "/moskva/"', () => {
    const route = router({ pathname: '/moskva/' });
    if (!route) {
        throw 'NOT_FOUND';
    }
    expect(route.route.getName()).toEqual('index');
});

it('should match "index" for "/krasnoyarskiy_kray/"', () => {
    const route = router({ pathname: '/krasnoyarskiy_kray/' });
    if (!route) {
        throw 'NOT_FOUND';
    }
    expect(route.route.getName()).toEqual('index');
});

it('should match "index" for "/1-e_otdelenie_sovhoza_podem/"', () => {
    const route = router({ pathname: '/1-e_otdelenie_sovhoza_podem/' });
    if (!route) {
        throw 'NOT_FOUND';
    }
    expect(route.route.getName()).toEqual('index');
});

it('should match "index" for "/kubinka-10/"', () => {
    const route = router({ pathname: '/kubinka-10/' });
    if (!route) {
        throw 'NOT_FOUND';
    }
    expect(route.route.getName()).toEqual('index');
});

it('should match "index" for "/sankt-peterburg/"', () => {
    const route = router({ pathname: '/sankt-peterburg/' });
    if (!route) {
        throw 'NOT_FOUND';
    }
    expect(route.route.getName()).toEqual('index');
});

it('should match "index" for "/bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb/"', () => {
    const route = router({ pathname: '/bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb/' });
    expect(route).toBeNull();
});

it('should match "index" for "/5c0849h0R9-2fP3yDgWd1IBek4Ka9HEJXCSKVU8R6BaMn8cfZWomdv/"', () => {
    const route = router({ pathname: '/5c0849h0R9-2fP3yDgWd1IBek4Ka9HEJXCSKVU8R6BaMn8cfZWomdv/' });
    expect(route).toBeNull();
});
