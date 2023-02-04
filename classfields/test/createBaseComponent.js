const createBaseComponent = () => {
    let currentProps = {};

    const BaseComponent = props => {
        currentProps = props;

        return null;
    };

    Object.defineProperty(BaseComponent, 'props', {
        value: () => currentProps,
    });

    return BaseComponent;
};

module.exports = createBaseComponent;
