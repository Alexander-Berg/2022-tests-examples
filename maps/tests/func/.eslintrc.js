module.exports = {
    "extends": "loris/es6",
    "env": {
        "browser": true
    },
    "rules": {
        "no-var": "off",
        "no-invalid-this": "off",
        "strict": ["error", "never"]
    },
    "globals": {
        "module": true,
        "describe": true,
        "it": true,
        "expect": true,
        "beforeEach": true,
        "afterEach": true,
        "before": true,
        "after": true,
        "assert": true,
        "require": true,
        "PO": true,
        "ymaps": true,
        "myMap": true,
        "hermione": true,
        "process": true
    }
};
