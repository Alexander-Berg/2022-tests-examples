module.exports = {
    "env": {
        "browser": true,
        "commonjs": true,
        "es2021": true,
    },
    "extends": [
        "eslint:recommended",
        'plugin:@typescript-eslint/eslint-recommended',
        'plugin:@typescript-eslint/recommended',
    ],
    "parser": "@typescript-eslint/parser",
    "parserOptions": {
        "ecmaVersion": 13,
    },
    "plugins": [
        "@typescript-eslint",
    ],
    "rules": {
        'comma-dangle': [ 'error', 'always-multiline' ],
        indent: 'off',
        'no-unused-vars': 'off',
        semi: [ 'error', 'always' ],
        '@typescript-eslint/indent': [ 'error', 4, {
            SwitchCase: 1,
        } ],
        '@typescript-eslint/no-unused-vars': [ 'error', { ignoreRestSiblings: true } ],
    },
};
