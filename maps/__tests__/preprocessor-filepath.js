module.exports = {
    process(src, path) {
        return `
            "use strict";
            Object.defineProperty(exports, "__esModule", {
               value: true
            });
            exports.default = "${path.replace(__dirname.replace('/__tests__', ''), '')}";
        `;
    }
};
