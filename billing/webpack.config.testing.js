/* eslint no-param-reassign: 0 */
const path = require('path');
const autoprefixer = require('autoprefixer');
const HtmlWebpackInlineSourcePlugin = require('html-webpack-inline-source-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CleanWebpackPlugin = require('clean-webpack-plugin');

const paths = require('./paths');

module.exports = {
    entry: {
        dev: paths.dev.indexJs
    },

    cache: true,
    devtool: 'source-map',

    output: {
        path: paths.dist,
        filename: '[name]/js/[name].[hash:8].js',
        chunkFilename: '[name]/js/[name].[hash:8].chunk.js',
        publicPath: '/'
    },

    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules\/(?!(query-string|strict-uri-encode)\/).*/,
                use: [
                    {
                        loader: 'babel-loader',
                        options: {
                            presets: ['@babel/preset-env']
                        }
                    },
                    'eslint-loader'
                ]
            },
            {
                test: /\.js$/,
                use: ['source-map-loader'],
                enforce: 'pre'
            },
            {
                test: /\.css$/,
                use: [
                    {
                        loader: MiniCssExtractPlugin.loader
                    },
                    {
                        loader: 'css-loader',
                        options: {
                            importLoaders: 1
                        }
                    },
                    {
                        loader: 'postcss-loader',
                        options: {
                            plugins: [
                                autoprefixer({
                                    browsers: ['last 2 versions']
                                })
                            ],
                            config: {
                                path: path.resolve(__dirname, 'postcss.config.js')
                            }
                        }
                    }
                ]
            },
            {
                test: /\.(svg)$/,
                use: [
                    {
                        loader: 'url-loader',
                        options: {
                            limit: 4096,
                            name: '[path][name].[ext]'
                        }
                    }
                ]
            }
        ]
    },

    plugins: [
        new CleanWebpackPlugin(path.join(paths.dist, 'dev'), {
            root: paths.root
        }),
        new HtmlWebpackPlugin({
            inject: 'head',
            template: paths.dev.indexHtml,
            filename: paths.dev.serverIndexHtml,
            inlineSource: '.(js|css)$',
            minify: false,
            src: process.env.DEB_CART_FRAME_SRC
        }),
        new HtmlWebpackInlineSourcePlugin(),
        new MiniCssExtractPlugin({
            filename: '[name].css',
            chunkFilename: '[id].css'
        })
    ]
};
