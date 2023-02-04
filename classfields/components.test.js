const React = require('react');
const components = require('./components');

describe('is', () => {
    describe('React.Component', () => {
        class Foo extends React.PureComponent {
            render() {
                return <div/>;
            }
        }

        it('должен подтвердить, что это компонент', () => {
            const foo = React.createElement(Foo);

            expect(components.is(foo)).toEqual(true);
        });

        it('должен подтвердить, что это экземпляр Foo', () => {
            const foo = React.createElement(Foo);

            expect(components.is(foo, Foo)).toEqual(true);
        });
    });

    describe('React.PureComponent', () => {
        class Foo extends React.PureComponent {
            render() {
                return <div/>;
            }
        }

        it('должен подтвердить, что это компонент', () => {
            const foo = React.createElement(Foo);

            expect(components.is(foo)).toEqual(true);
        });

        it('должен подтвердить, что это экземпляр Foo', () => {
            const foo = React.createElement(Foo);

            expect(components.is(foo, Foo)).toEqual(true);
        });
    });

    describe('stateless', () => {
        const Foo = () => <div/>;

        it('должен подтвердить, что это компонент', () => {
            const foo = React.createElement(Foo);

            expect(components.is(foo)).toEqual(true);
        });

        it('должен подтвердить, что это экземпляр Foo', () => {
            const foo = React.createElement(Foo);

            expect(components.is(foo, Foo)).toEqual(true);
        });
    });

    describe('false', () => {
        it('должен вернуть false на строку', () => {
            expect(components.is('foo bar')).toEqual(false);
        });

    });
});
