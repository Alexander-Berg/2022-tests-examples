const _ = require('lodash');
const React = require('react');
const BillingFrameSberbank = require('./BillingFrameSberbank');
const TextInput = require('auto-core/react/components/islands/TextInput');
const Button = require('auto-core/react/components/islands/Button');
const Link = require('auto-core/react/components/islands/Link');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const DEFAULT_PROPS = {
    isMobile: false,
    initialPhone: '',
};

let props;

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);
});

it('правильно рисует компонент когда не передан телефон', () => {
    const page = shallowRenderSberbankFrame();
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент когда передан телефон', () => {
    props.initialPhone = '+7 999 777-22-66';
    const page = shallowRenderSberbankFrame();
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент  на мобилке', () => {
    props.isMobile = true;
    const page = shallowRenderSberbankFrame();
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('при клике на ссылку "оплатить на сайте сбера" вызовет коллбэк', () => {
    props.onPaySberbankSiteLinkClick = jest.fn();
    const page = shallowRenderSberbankFrame();
    const link = page.find(Link);
    link.simulate('click');

    expect(props.onPaySberbankSiteLinkClick).toHaveBeenCalledTimes(1);
});

describe('поле ввода телефона', () => {
    let page;

    beforeEach(() => {
        page = shallowRenderSberbankFrame();
    });

    it('при вводе телефона маскирует символы', () => {
        const phoneInput = page.find(TextInput);
        phoneInput.simulate('change', '79771234567');

        const displayedValue = page.find(TextInput).prop('value');
        expect(displayedValue).toBe('+7 977 123-45-67');
    });

    it('не даст ввести больше чем можно', () => {
        const phoneInput = page.find(TextInput);
        phoneInput.simulate('change', '7977123456789');

        const displayedValue = page.find(TextInput).prop('value');
        expect(displayedValue).toBe('+7 977 123-45-67');
    });

    it('при сбросе оставит код в поле ввода', () => {
        const phoneInput = page.find(TextInput);
        phoneInput.simulate('change', '', null, { source: 'clear' });

        const displayedValue = page.find(TextInput).prop('value');
        expect(displayedValue).toBe('+7');
    });
});

describe('при сабмите телефона', () => {
    let page;

    beforeEach(() => {
        props.onPayButtonClick = jest.fn();
        page = shallowRenderSberbankFrame();
    });

    describe('если номер корректный', () => {
        beforeEach(() => {
            const phoneInput = page.find(TextInput);
            const button = page.find(Button);
            phoneInput.simulate('change', '79771234567');
            button.simulate('click');
        });

        it('вызовет коллбэк с отформатированным номером', () => {
            expect(props.onPayButtonClick).toHaveBeenCalledWith('79771234567');
        });

        it('покажет сообщение об смс подтверждении и обновит дисклеймер', () => {
            expect(shallowToJson(page)).toMatchSnapshot();
        });

        it('задизэблит поле ввода телефона и кнопку', () => {
            expect(page.find(TextInput).prop('disabled')).toBe(true);
            expect(page.find(Button).prop('disabled')).toBe(true);
        });
    });

    it('если номер некорректный не вызовет коллбэк и покажет ошибку', () => {
        const phoneInput = page.find(TextInput);
        const button = page.find(Button);
        phoneInput.simulate('change', '7977123');
        button.simulate('click');

        const updatedPhoneInput = page.find(TextInput);

        expect(props.onPayButtonClick).not.toHaveBeenCalled();
        expect(updatedPhoneInput.prop('error')).not.toBe('');
    });
});

function shallowRenderSberbankFrame() {
    const page = shallow(<BillingFrameSberbank { ...props }/>, { disableLifecycleMethods: true });
    const instance = page.instance();
    instance.phoneInput = {
        focus: () => { },
    };
    instance.payButton = {
        focus: () => { },
    };
    instance.componentDidMount();

    return page;
}
