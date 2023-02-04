const path = require('path');
const config = require('../webpack.config.base');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = {
    ...config,
    entry: {
        index: './stand/index.ts',
        index_polyfilled: './stand/index_polyfilled.ts'
    },
    output: {
        path: path.resolve(__dirname, 'build'),
        filename: '[name].js'
    },
    watchOptions: {
        ignored: /node_modules/
    },
    devServer: {
        contentBase: 'build',
        compress: true,
        host: '0.0.0.0',
        disableHostCheck: true,
        port: 8083,
        liveReload: false
    },
    plugins: [
        new CleanWebpackPlugin(),
        new CopyWebpackPlugin({
            patterns: [
                {from: 'stand/index.html', to: './index.html'},
                {from: 'stand/index.html', to: './index_polyfilled.html'},
            ]
        })
    ]
};
