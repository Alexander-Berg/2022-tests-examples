import bem from '../src';

it('should build css class for block', () => {
    const b = bem('my-block');

    expect(b()).toBe('my-block');
});

it('should build css class for block with modifiers', () => {
    const b = bem('my-block');

    expect(
        b({
            mod1: 'val',
            mod2: 4,
            mod3: null,
            mod4: 0,
            mod5: undefined,
            mod6: true,
            mod7: false
        })
    ).toBe('my-block my-block_mod1_val my-block_mod2_4 my-block_mod4_0 my-block_mod6');
});

it('should build css class for block element', () => {
    const b = bem('my-block');

    expect(b('elem')).toBe('my-block__elem');
});

it('should build css class for block element with modifiers', () => {
    const b = bem('my-block');

    expect(
        b(
            'elem',
            {
                mod1: 'val',
                mod2: 4,
                mod3: null,
                mod4: 0,
                mod5: undefined,
                mod6: true,
                mod7: false
            }
        )
    ).toBe('my-block__elem my-block__elem_mod1_val my-block__elem_mod2_4 my-block__elem_mod4_0 my-block__elem_mod6');
});

it('should mix css classes', () => {
    const b1 = bem('my-block1');
    const b2 = bem('my-block2');

    expect(bem.mix(b1(), undefined, '', null, b2())).toBe('my-block1 my-block2');
});
