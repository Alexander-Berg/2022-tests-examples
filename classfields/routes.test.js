const ROUTE_NAMES = require('./route-names');
const routes = require('./routes');

const cabinetRoutes = routes(ROUTE_NAMES);
const values = Object.values(ROUTE_NAMES);

cabinetRoutes.forEach((route) => {
    it(`роут ${ route.name } с pattern: ${ route.pattern } должен быть указан в route-names.json`, () => {
        const result = values.includes(route.name);

        expect(result).toBe(true);
    });
});
