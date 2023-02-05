const assert = require('assert');
const preprocessHtmlLayout = require('./preprocessHtmlLayout');

assert.equal(
    preprocessHtmlLayout(`
        AAA
        BBB
        <ymaps></ymaps>
        CCC <ymaps></ymaps> DDD
    `),
    'AAA BBB<ymaps></ymaps>CCC <ymaps></ymaps> DDD');

assert.equal(
    preprocessHtmlLayout(`
        <ymaps style="
            color: red;
            {% if x %} display: none; {% endif %}
        ">
        </ymaps>
    `),
    '<ymaps {% style %}color: red;\n{% if x %} display: none; {% endif %}{% endstyle %}></ymaps>');

assert.equal(
    preprocessHtmlLayout(`
        <ymaps class="foo bar {{xxx}} prefix-{{yyy}}"></ymaps>
    `, { cssPrefix: 'y-42-' }),
    '<ymaps class="y-42-foo y-42-bar y-42-{{xxx}} y-42-prefix-{{yyy}}"></ymaps>');
