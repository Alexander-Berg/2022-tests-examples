'use strict';

const babelJest = require('babel-jest');
const {getLevels} = require('../tools');

module.exports = babelJest.createTransformer({
    plugins: [
        [
            'bem-import',
            {
                naming: 'origin',
                langs: ['ru'],
                techs: ['js'],
                techMap: {
                    js: ['react.js']
                },
                levels: getLevels()
            }
        ]
    ]
});
