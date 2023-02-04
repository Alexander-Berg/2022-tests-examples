"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.logger = void 0;
const winston = require("winston");
const config_1 = require("app/config");
const format = winston.format.printf(({ level, message }) => `[${level}] ${message}`);
exports.logger = winston.createLogger(!process.env.DISABLE_LOGGING ? {
    transports: [
        new winston.transports.Console({
            level: config_1.config['logger.level'],
            format: config_1.config['logger.colorize'] ?
                winston.format.combine(winston.format.colorize(), format) :
                winston.format.combine(format)
        })
    ]
} : {});
