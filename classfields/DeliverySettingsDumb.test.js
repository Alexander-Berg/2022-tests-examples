const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const DeliverySettings = require('./DeliverySettingsDumb');
const { noop } = require('lodash');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

it('должен вернуть корректный компонент, если regions = []', () => {
    const regions = [ ];
    const priceTexts = { price: 'text' };
    const offerIDs = [ 1, 2, 3 ];
    expect(shallowToJson(shallow(
        <DeliverySettings
            regions={ regions }
            expandRegionServices={ noop }
            collapseRegionServices={ noop }
            restoreRegion={ noop }
            deleteRegion={ noop }
            offerIDs={ offerIDs }
            visible={ true }
            hideModal={ noop }
            saveDelivery={ noop }
            priceTexts={ priceTexts }
            deleteDelivery={ noop }
            selectRegion={ noop }
            showDeliverySettingsConfirm={ noop }
            hideDeliverySettingsConfirm={ noop }
            addressNeeded={ true }
            confirmAction="delete"
            isShowingLoader={ false }
        />, { context: contextMock })),
    ).toMatchSnapshot();
});

it('должен вернуть корректный компонент, если есть regions', () => {
    const regions = [ 1, 2, 3 ];
    const priceTexts = { price: 'text' };
    const offerIDs = [ 1, 2, 3 ];
    expect(shallowToJson(shallow(
        <DeliverySettings
            regions={ regions }
            expandRegionServices={ noop }
            collapseRegionServices={ noop }
            restoreRegion={ noop }
            deleteRegion={ noop }
            offerIDs={ offerIDs }
            visible={ false }
            hideModal={ noop }
            saveDelivery={ noop }
            priceTexts={ priceTexts }
            deleteDelivery={ noop }
            selectRegion={ noop }
            showDeliverySettingsConfirm={ noop }
            hideDeliverySettingsConfirm={ noop }
            addressNeeded={ false }
            confirmAction="delete"
            isShowingLoader={ false }
        />, { context: contextMock })),
    ).toMatchSnapshot();
});

it('должен вернуть корректный компонент, если есть isShowingLoader', () => {
    const regions = [];
    const priceTexts = { price: 'text' };
    const offerIDs = [ 1, 2, 3 ];
    expect(shallowToJson(shallow(
        <DeliverySettings
            regions={ regions }
            expandRegionServices={ noop }
            collapseRegionServices={ noop }
            restoreRegion={ noop }
            deleteRegion={ noop }
            offerIDs={ offerIDs }
            hideModal={ noop }
            saveDelivery={ noop }
            priceTexts={ priceTexts }
            deleteDelivery={ noop }
            selectRegion={ noop }
            showDeliverySettingsConfirm={ noop }
            hideDeliverySettingsConfirm={ noop }
            isShowingLoader={ true }
        />, { context: contextMock })),
    ).toMatchSnapshot();
});

it('onSelect тест: должен вызвать selectRegion и вызвать this.deliverySettingsSuggest.current.onClearClick, если нет ошибок', () => {
    const selectRegion = jest.fn(() => Promise.resolve());
    const regions = [];
    const geoObject = 'some object';
    const DeliverySettingsInstance = shallow(
        <DeliverySettings
            regions={ regions }
            selectRegion={ selectRegion }
        />, { context: contextMock }).instance();

    DeliverySettingsInstance.onSelect(geoObject);
    DeliverySettingsInstance.deliverySettingsSuggest.current = { onClearClick: jest.fn() };
    expect(selectRegion).toHaveBeenCalledWith(geoObject);
});

describe('onSaveButtonClick тесты', () => {
    it('должен вызвать showDeliverySettingsConfirm c корректными параметрами, ' +
        'если offerIDs > 1 и hasNotDeletedRegions', () => {
        const showDeliverySettingsConfirm = jest.fn();
        const regions = [ { deleted: false } ];
        const offerIDs = [ 1, 2, 3 ];
        const DeliverySettingsInstance = shallow(
            <DeliverySettings
                offerIDs={ offerIDs }
                regions={ regions }
                showDeliverySettingsConfirm={ showDeliverySettingsConfirm }
            />, { context: contextMock }).instance();

        DeliverySettingsInstance.onSaveButtonClick();
        expect(showDeliverySettingsConfirm).toHaveBeenCalledWith('save');
    });

    it('должен вызвать showDeliverySettingsConfirm c корректными параметрами, ' +
        'если offerIDs > 1 и !hasNotDeletedRegions', () => {
        const showDeliverySettingsConfirm = jest.fn();
        const regions = [ { deleted: true } ];
        const offerIDs = [ 1, 2, 3 ];
        const DeliverySettingsInstance = shallow(
            <DeliverySettings
                offerIDs={ offerIDs }
                regions={ regions }
                showDeliverySettingsConfirm={ showDeliverySettingsConfirm }
            />, { context: contextMock }).instance();

        DeliverySettingsInstance.onSaveButtonClick();
        expect(showDeliverySettingsConfirm).toHaveBeenCalledWith('delete');
    });

    it('должен вызвать saveDelivery, если offerIDs = 1, hasNotDeletedRegions', () => {
        const saveDelivery = jest.fn();
        const regions = [ { deleted: false } ];
        const offerIDs = [ 1 ];
        const DeliverySettingsInstance = shallow(
            <DeliverySettings
                saveDelivery={ saveDelivery }
                offerIDs={ offerIDs }
                regions={ regions }
            />, { context: contextMock }).instance();

        DeliverySettingsInstance.onSaveButtonClick();
        expect(saveDelivery).toHaveBeenCalled();
    });

    it('должен вызвать deleteDelivery, если offerIDs = 1, !hasNotDeletedRegions', () => {
        const deleteDelivery = jest.fn();
        const regions = [ { deleted: true } ];
        const offerIDs = [ 1 ];
        const DeliverySettingsInstance = shallow(
            <DeliverySettings
                offerIDs={ offerIDs }
                regions={ regions }
                deleteDelivery={ deleteDelivery }
            />, { context: contextMock }).instance();

        DeliverySettingsInstance.onSaveButtonClick();
        expect(deleteDelivery).toHaveBeenCalled();
    });
});

describe('onDeleteButtonClick тесты', () => {
    it('должен вызвать showDeliverySettingsConfirm c корректными параметрами, ' +
        'если offerIDs > 1', () => {
        const showDeliverySettingsConfirm = jest.fn();
        const regions = [ { deleted: false } ];
        const offerIDs = [ 1, 2, 3 ];
        const DeliverySettingsInstance = shallow(
            <DeliverySettings
                offerIDs={ offerIDs }
                regions={ regions }
                showDeliverySettingsConfirm={ showDeliverySettingsConfirm }
            />, { context: contextMock }).instance();

        DeliverySettingsInstance.onDeleteButtonClick();
        expect(showDeliverySettingsConfirm).toHaveBeenCalledWith('delete');
    });

    it('должен вызвать deleteDelivery, если offerIDs = 1', () => {
        const deleteDelivery = jest.fn();
        const regions = [ { deleted: true } ];
        const offerIDs = [ 1 ];
        const DeliverySettingsInstance = shallow(
            <DeliverySettings
                offerIDs={ offerIDs }
                regions={ regions }
                deleteDelivery={ deleteDelivery }
            />, { context: contextMock }).instance();

        DeliverySettingsInstance.onDeleteButtonClick();
        expect(deleteDelivery).toHaveBeenCalled();
    });
});

describe('onConfirmButtonClick тесты', () => {
    it('должен вызвать deleteDelivery, если confirmAction = delete', () => {
        const deleteDelivery = jest.fn();
        const regions = [];
        const offerIDs = [];
        const DeliverySettingsInstance = shallow(
            <DeliverySettings
                offerIDs={ offerIDs }
                regions={ regions }
                confirmAction="delete"
                deleteDelivery={ deleteDelivery }
            />, { context: contextMock }).instance();

        DeliverySettingsInstance.onDeleteButtonClick();
        expect(deleteDelivery).toHaveBeenCalled();
    });

    it('должен вызвать saveDelivery, если confirmAction = save', () => {
        const saveDelivery = jest.fn();
        const regions = [];
        const offerIDs = [];
        const DeliverySettingsInstance = shallow(
            <DeliverySettings
                offerIDs={ offerIDs }
                regions={ regions }
                confirmAction="save"
                saveDelivery={ saveDelivery }
            />, { context: contextMock }).instance();

        DeliverySettingsInstance.onConfirmButtonClick();
        expect(saveDelivery).toHaveBeenCalled();
    });
});
