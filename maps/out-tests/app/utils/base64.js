"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.encode = exports.decode = void 0;
exports.decode = (s) => Buffer.from(s, 'base64').toString('ascii');
exports.encode = (s) => {
    return Buffer.from(s).toString('base64')
        .replace(/\//gmi, '_')
        .replace(/\+/gmi, '-');
};
