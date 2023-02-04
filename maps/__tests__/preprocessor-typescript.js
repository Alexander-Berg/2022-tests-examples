const ts = require('typescript');
const fs = require('fs');
const configs = {};

module.exports = {
    process(src, path) {
        const configPath = ts.findConfigFile(path, ts.sys.fileExists, 'tsconfig.json');

        if (!configs[configPath]) {
            const configData = require(configPath);

            if (configData.extends) {
                const configParentData = require(require.resolve(configPath.replace('tsconfig', configData.extends)));
                configData.compilerOptions = {
                    ...configParentData.compilerOptions,
                    ...configData.compilerOptions
                };
            }
            configData.compilerOptions.module = 'commonjs';
            configs[configPath] = configData.compilerOptions;
        }
        const tsCode = ts.transpile(
            src,
            configs[configPath],
            path
        );

        return tsCode;
    }
};
