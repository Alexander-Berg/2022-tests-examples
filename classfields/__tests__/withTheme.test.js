import React from 'react';
import { mount } from 'enzyme';

import withTheme from '..';
import ThemeProvider from '../provider';

describe('withTheme', () => {
    const ThemeRenderer = withTheme({ themes: [ 'islands', 'realty' ] })(
        ({ theme }) => <span>{theme}</span>
    );

    it('should provide theme', () => {
        const wrapperWithIslandsTheme = mount(
            <ThemeProvider theme='islands'>
                <ThemeRenderer />
            </ThemeProvider>
        );

        expect(wrapperWithIslandsTheme.text()).toBe('islands');

        const wrapperWithRealtyTheme = mount(
            <ThemeProvider theme='realty'>
                <ThemeRenderer />
            </ThemeProvider>
        );

        expect(wrapperWithRealtyTheme.text()).toBe('realty');

        const wrapperWithUndefinedTheme = mount(
            <ThemeProvider theme={undefined}>
                <ThemeRenderer />
            </ThemeProvider>
        );

        expect(wrapperWithUndefinedTheme.text()).toBe('');
    });

    it('theme should be undefined if there is no ThemeProvider', () => {
        const wrapper = mount(<ThemeRenderer />);

        expect(wrapper.text()).toBe('');
    });

    it('should set displayName', () => {
        function Link() {}
        const Wrapped = withTheme({ themes: [ 'islands', 'realty' ] })(Link);

        expect(Wrapped.displayName).toBe('withTheme(Link)');
    });

    it('prop theme should have a priority over context value', () => {
        const wrapper = mount(
            <ThemeProvider theme='islands'>
                <ThemeRenderer theme='realty' />
            </ThemeProvider>
        );

        expect(wrapper.text()).toBe('realty');
    });

    it('should take theme from nearest provider in tree', () => {
        const wrapper = mount(
            <ThemeProvider theme='islands'>
                <ThemeProvider theme='realty'>
                    <ThemeRenderer />
                </ThemeProvider>
            </ThemeProvider>
        );

        expect(wrapper.text()).toBe('realty');
    });

    it('should pass undefined if theme from provider is not supported', () => {
        const Component = withTheme({ themes: [ 'realty' ] })(({ theme }) => {
            expect(theme).toBe(undefined);

            return null;
        });

        const wrapper = mount(
            <ThemeProvider theme='islands'>
                <Component />
            </ThemeProvider>
        );

        expect(wrapper.text()).toBe('');
    });

    it('should throw an error if themes prop is not provided', () => {
        expect(() => {
            withTheme()(() => null);
        }).toThrow('Need to specify array of supported themes in `themes` option of withTheme() hoc');
    });

    it('should hoist non-react statics for functions', () => {
        function Button() {}

        Button.staticProp = 1;

        expect(withTheme({ themes: [ 'realty' ] })(Button).staticProp).toBe(1);
    });

    it('should hoist non-react statics for classes', () => {
        class Button extends React.Component {
            render() {
                return null;
            }
        }

        Button.staticProp = 1;

        expect(withTheme({ themes: [ 'realty' ] })(Button).staticProp).toBe(1);
    });

    it('should forward ref', () => {
        expect.assertions(1);

        class Button extends React.Component {
            render() {
                return null;
            }
        }

        const ButtonWithTheme = withTheme({ themes: [ 'realty' ] })(Button);

        class Test extends React.Component {
            constructor() {
                super();
                this.buttonRef = React.createRef();
            }

            componentDidMount() {
                expect(this.buttonRef.current).toBeInstanceOf(Button);
            }

            render() {
                return <ButtonWithTheme ref={this.buttonRef} />;
            }
        }

        mount(<Test />);
    });
});
