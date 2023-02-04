const packageVersion = require('./package.json').version;

module.exports = {
    registry: {
        tag: packageVersion
    },
    abcServiceName: "toloka-test-viewer",
    geoSpecific: {
        static: [
            {
                from: "./index.html",
                addTag: true
            },
            {
                from: "./index.css",
                addTag: true
            },
            {
                from: "./index.js",
                addTag: true
            }
        ]
    }
};
