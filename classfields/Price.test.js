const React = require('react');
const { shallow } = require('enzyme');

const Price = require('./Price');

it('должен вернуть цену в рублях', () => {
    const tree = shallow(
        <Price
            free="бесплатно"
            price={ 10 }
        />,
    );
    expect(tree.text()).toBe('10\u00a0₽');
});

it('должен вернуть текст, если цена 0', () => {
    const tree = shallow(
        <Price
            free="бесплатно"
            price={ 0 }
        />,
    );
    expect(tree.text()).toBe('бесплатно');
});

it('должен вернуть цену в рублях с префиксом и постфиксом', () => {
    const tree = shallow(
        <Price
            free="бесплатно"
            postfix="штука"
            prefix="Купить за"
            price={ 10 }
        />,
    );
    expect(tree.text()).toBe('Купить за 10\u00a0₽ штука');
});

it('должен вернуть текст с префиксом и постфиксом, если цена 0', () => {
    const tree = shallow(
        <Price
            free="бесплатно"
            postfix="всё"
            prefix="Купить за"
            price={ 0 }
            showPrefixIfFree
            showPostfixIfFree
        />,
    );
    expect(tree.text()).toBe('Купить за бесплатно всё');
});

it('должен вернуть текст бех префикса и постфикса, если цена 0', () => {
    const tree = shallow(
        <Price
            free="бесплатно"
            postfix="всё"
            prefix="Купить за"
            price={ 0 }
        />,
    );
    expect(tree.text()).toBe('бесплатно');
});

it('должен вернуть цену в usd', () => {
    const tree = shallow(
        <Price
            free="бесплатно"
            price={ 10 }
            currency="USD"
        />,
    );
    expect(tree.text()).toBe('10\u00a0$');
});
