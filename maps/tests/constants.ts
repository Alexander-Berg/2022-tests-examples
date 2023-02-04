const dbPort = Number(process.env.DB_PORT || 4554);
const appPort = Number(process.env.MAPS_NODEJS_PORT || 8081);

export {dbPort, appPort};
