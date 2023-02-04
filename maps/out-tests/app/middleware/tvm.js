"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.tvmMiddleware = void 0;
const tvm = require("@yandex-int/tvm");
const express_async_1 = require("@yandex-int/express-async");
exports.tvmMiddleware = ({ daemonBaseUrl, token }) => {
    const client = new tvm.Client({ daemonBaseUrl, token });
    return express_async_1.asyncMiddleware(async (req, _res) => {
        const [zora, apikeysInt] = await Promise.all(client.getServiceTickets(['zora', 'apikeysInt']));
        req.tvm = { zora, apikeysInt };
    });
};
