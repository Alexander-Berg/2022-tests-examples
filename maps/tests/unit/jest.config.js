module.exports = {
    rootDir: '../../',
    preset: 'ts-jest',
    testEnvironment: 'jsdom',
    moduleDirectories: ['node_modules', 'src/client', 'src/shared'],
    moduleFileExtensions: ['js', 'jsx', 'ts', 'tsx'],
    moduleNameMapper: {
        '\\.(css|pcss)$': '<rootDir>/tests/unit/style-mock.ts',
        '^lodash-es/?(.*)$': '<rootDir>/node_modules/lodash/index.js',
        '^date-fns/?(.*)$': '<rootDir>/node_modules/date-fns/index.js',
        '@yandex-int/maps-tanker/client':
            '<rootDir>/node_modules/@yandex-int/maps-tanker/out/src/environments/client.js'
    },
    globals: {
        'ts-jest': {
            tsconfig: 'src/client/tsconfig.json',
            isolatedModules: true
        }
    },
    setupFilesAfterEnv: ['<rootDir>/tests/unit/test-setup.ts'],
    testMatch: ['<rootDir>/src/client/**/*.test.ts?(x)']
};
