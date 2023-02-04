const motoTypeGroup = require('./motoTypeGroup');
describe('moto_type', () => {
    const TESTS = [
        {
            params: { moto_type: [ 'CHOPPER', 'MINIBIKE' ] },
            result: { moto_type: [ 'CHOPPER', 'MINIBIKE' ] },
        },

        {
            params: { moto_type: [ 'CHOPPER', 'MINIBIKE', 'SPORTBIKE', 'SPORTTOURISM', 'SUPERSPORT' ] },
            result: { moto_type: [ 'CHOPPER', 'MINIBIKE', 'SPORTBIKE', 'SPORTTOURISM', 'SUPERSPORT', 'SPORT_GROUP' ] },
        },
    ];

    TESTS.forEach(test =>
        it(JSON.stringify(test.params), () => {
            const params = { ...test.params };
            motoTypeGroup(params);
            expect(params).toEqual(test.result);
        }),
    );
});
