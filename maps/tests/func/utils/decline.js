/*eslint quote-props: ["error", "as-needed", { "keywords": true, "unnecessary": false }]*/

const VOCABULARY = {
    'ошибка': {
        some: 'ошибки',
        many: 'ошибок'
    },
    'предупреждение': {
        some: 'предупреждения',
        many: 'предупреждений'
    }
};

module.exports = function decline(word, number) {
    const found = Object.assign({some: word, many: word}, VOCABULARY[word]);

    function declension(word, some, many, number) {
        const lastTwoDigits = number % 100;
        const lastDigit = number % 10;

        if (lastTwoDigits > 10 && lastTwoDigits < 20) {
            return many;
        }
        if (lastDigit === 1) {
            return word;
        }
        if (lastDigit > 1 && lastDigit < 5) {
            return some;
        }

        return many;
    }

    return declension(word, found.some, found.many, number);
};
