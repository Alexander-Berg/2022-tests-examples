/**
 * @jest-environment node
 */
const path = require('path');
const fs = require('fs');
const babel = require('@babel/core');

const plugin = [ path.resolve(__dirname, 'babelFreezeExportsPlugin.js'), { freezeModuleName: 'deep-freeze' } ];

it('должен правильно трансформировать source', () => {
    const actual = babel.transformFileSync(
        path.resolve(__dirname, 'fixtures/source.mock.js'),
        { plugins: [ plugin ] },
    ).code;

    const expected = fs.readFileSync(path.resolve(__dirname, 'fixtures/expected.js')).toString();

    expect(trimSpaces(actual)).toBe(trimSpaces(expected));
});

it('не должен трансформировать не .mock.js файлы', () => {
    const actual = babel.transformFileSync(
        path.resolve(__dirname, 'fixtures/not_a_mock.js'),
        { plugins: [ plugin ] },
    ).code;

    const expected = fs.readFileSync(path.resolve(__dirname, 'fixtures/not_a_mock.js')).toString();

    expect(trimSpaces(actual)).toBe(trimSpaces(expected));
});

function trimSpaces(str) {
    return str.replace(/\s/g, '');
}
