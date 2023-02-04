const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const BackOnSaleDumb = require('./BackOnSaleDumb');
const backOnSaleProps = require('./mocks/backOnSaleProps');

it('renderPlaceholder должен вернуть подпись с картинкой и кнопкой', () => {
    const backOnSaleDumbInstance = shallow(
        <BackOnSaleDumb
            { ...backOnSaleProps }
            isRenderingPlaceholder={ true }
            isRenderingPlaceholderButton={ true }
        />,
        { context: { ...contextMock } },
    ).instance();

    expect(backOnSaleDumbInstance.renderPlaceholder()).toMatchSnapshot();
});

it('renderPlaceholder должен вернуть подпись с картинкой', () => {
    const backOnSaleDumbInstance = shallow(
        <BackOnSaleDumb
            { ...backOnSaleProps }
            isRenderingPlaceholder={ true }
            isRenderingPlaceholderButton={ false }
        />,
        { context: { ...contextMock } },
    ).instance();

    expect(backOnSaleDumbInstance.renderPlaceholder()).toMatchSnapshot();
});

describe('renderContent', () => {
    it('должен нарисовать компонент с фильтрами, сортировками, листингом и пагинацией', () => {
        const backOnSaleDumbInstance = shallow(
            <BackOnSaleDumb
                { ...backOnSaleProps }
                isRenderingPlaceholder={ false }
            />,
            { context: { ...contextMock } },
        ).instance();

        expect(backOnSaleDumbInstance.renderContent()).toMatchSnapshot();
    });

    it('должен нарисовать ListingEmpty', () => {
        const backOnSaleDumb = shallow(
            <BackOnSaleDumb
                { ...backOnSaleProps }
                offers={ [] }
            />,
            { context: { ...contextMock } },
        );

        expect(backOnSaleDumb.find('ListingEmpty')).toHaveLength(1);
    });
});

describe('render тесты', () => {
    it('должен нарисовать листинг', () => {
        const backOnSaleDumbInstance = shallow(
            <BackOnSaleDumb
                { ...backOnSaleProps }
                isRenderingPlaceholder={ false }
            />,
            { context: { ...contextMock } },
        ).instance();
        backOnSaleDumbInstance.renderContent = () => 'content';
        backOnSaleDumbInstance.renderPlaceholder = () => 'placeholder';

        expect(backOnSaleDumbInstance.renderContent()).toMatchSnapshot();
    });

    it('должен нарисовать заглушку, если isRenderingPlaceholder', () => {
        const backOnSaleDumbInstance = shallow(
            <BackOnSaleDumb
                { ...backOnSaleProps }
                isRenderingPlaceholder={ true }
            />,
            { context: { ...contextMock } },
        ).instance();
        backOnSaleDumbInstance.renderPlaceholder = () => 'placeholder';

        expect(backOnSaleDumbInstance.render()).toMatchSnapshot();
    });
});
