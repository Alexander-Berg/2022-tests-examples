'use strict';

const moment = require('moment');
const enzyme = require('enzyme');
const Adapter = require('enzyme-adapter-react-16');

const createBaseComponent = require('./createBaseComponent');

moment.locale('ru');
enzyme.configure({ adapter: new Adapter() });

global.createBaseComponent = createBaseComponent;
global.mount = enzyme.mount;
global.shallow = enzyme.shallow;
global.render = enzyme.render;
