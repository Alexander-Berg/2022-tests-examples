const fs = require('fs');
const path = require('path');
const { StringDecoder } = require('string_decoder');
const decoder = new StringDecoder('utf8');

const filepath = process.argv[2];

const apiVersion = process.env.API_VERSION_ACTUAL || '2.1-dev';
const vectorBundle = process.env.VECTOR_BUNDLE || '5.3.3';
const night = process.env.NIGHT === '1';

let config = decoder.write(fs.readFileSync(filepath));
let template = decoder.write(fs.readFileSync(path.resolve(__dirname, 'web-template.html')));
let result = template.replace('CONFIG_HERE', config);
result = result.replace('API_VERSION', apiVersion);
result = result.replace('VECTOR_BUNDLE', vectorBundle);
result = result.replace('MODE', night ? 'night' : '');

fs.writeFileSync(path.resolve( __dirname, './temp.html'), result);
