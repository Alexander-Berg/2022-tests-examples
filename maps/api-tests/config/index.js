const defaultConfig = require('./default');

const envConfig = {
  production: require('./production'),
  testing: require('./testing'),
};

module.exports = {
  ...defaultConfig,
  ...envConfig[process.env.CONFIG],
};
