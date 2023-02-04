// TODO: Отказаться от babel в https://st.yandex-team.ru/GEODISPLAY-1054.
const babelJest = require('babel-jest');

const babelConfig = require('./babel.test.config.json');
const jsBabelTransformer = babelJest.default.createTransformer(babelConfig);

module.exports = jsBabelTransformer;
