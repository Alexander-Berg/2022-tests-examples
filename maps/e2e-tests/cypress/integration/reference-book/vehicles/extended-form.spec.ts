import * as vehiclesReferenceBookKeyset from '../../../../../src/translations/vehicles-reference-book';

import selectors from '../../../../src/constants/selectors';

const parametersCouriers = {
  labelsInputs: {
    maxRuns: {
      ru: vehiclesReferenceBookKeyset.ru.form_maxRuns_label,
    },
    imei: {
      ru: vehiclesReferenceBookKeyset.ru.form_imei_label,
    },
    geocodingStartAdressGarage: {
      ru: vehiclesReferenceBookKeyset.ru.extendedForm_startGarage_label,
    },

    geocodingFinishAdressGarage: {
      ru: vehiclesReferenceBookKeyset.ru.extendedForm_endGarage_label,
    },
  },

  imeiInput: {
    placeholder: {
      ru: vehiclesReferenceBookKeyset.ru.form_imei_placeholder,
    },
  },
};

const shiftStartPoint = {
  label: {
    ru: vehiclesReferenceBookKeyset.ru.visitDepotAtStart_label,
  },

  garageAndDepot: {
    ru: vehiclesReferenceBookKeyset.ru.startFrom_garageAndDepot,
  },

  depot: {
    ru: vehiclesReferenceBookKeyset.ru.startFrom_depot,
  },

  garage: {
    ru: vehiclesReferenceBookKeyset.ru.startFrom_garage,
  },
};

const shiftFinishPoint = {
  label: {
    ru: vehiclesReferenceBookKeyset.ru.returnToDepot_label,
  },

  garageAndDepot: {
    ru: vehiclesReferenceBookKeyset.ru.returnTo_garageAndDepot,
  },

  depot: {
    ru: vehiclesReferenceBookKeyset.ru.returnTo_depot,
  },

  garage: {
    ru: vehiclesReferenceBookKeyset.ru.returnTo_garage,
  },

  notReturnToDepot: {
    ru: vehiclesReferenceBookKeyset.ru.returnTo_lastPoint,
  },
};

const simpleModal = {
  nameInput: {
    placeholder: {
      ru: vehiclesReferenceBookKeyset.ru.simpleForm_name_placeholder,
    },
  },
  numberInput: {
    placeholder: {
      ru: vehiclesReferenceBookKeyset.ru.simpleForm_number_placeholder,
    },
  },
  errorTooltips: {
    nameNumberInputsEmpty: {
      ru: vehiclesReferenceBookKeyset.ru.errors_required,
    },
    duplicatedNumber: {
      ru: vehiclesReferenceBookKeyset.ru.errors_duplicatedNumber,
    },
  },
  formDeleteConfirm: {
    title: {
      ru: vehiclesReferenceBookKeyset.ru.deleteConfirmTitle,
    },
    subtitle: {
      ru: vehiclesReferenceBookKeyset.ru.deleteConfirmSubtitleTitle,
    },
  },
};

describe('Testing the functionality in the simple form of adding a transport)', function () {
  before(function () {
    cy.yandexLogin('mvrpManager');
    cy.preserveCookies();
    cy.clearLocalforage();
    cy.openAndCloseVideo();

    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.vehiclesReferenceBook).click();
    cy.get(selectors.modal.closeButton, { timeout: 10000 }).click();
  });

  const { addVehicle, table } = selectors.content.referenceBook.vehicles;
  const { nameInputLabel, nameInput, numberInput } =
    selectors.content.referenceBook.vehicles.modal.simple;
  const {
    dialogRemoveTitle,
    dialogRemoveSubtitle,
    dialogRemoveCloser,
    dialogRemoveCancelButton,
    dialogRemoveButton,
  } = selectors.content.referenceBook.vehicles.modal.dialogRemoveVehicle;
  const {
    submitButton,
    deleteButton,
    errorTooltip,
    closerErrorTooltip,
    editVehicleTitle,
    closerModal,
    inputsHaveError,
    dialogRemove,
  } = selectors.content.referenceBook.vehicles.modal;

  // @see https://testpalm.yandex-team.ru/courier/testcases/660

  describe('Add Vehicle in the Simple Form Reference Book', function () {
    it('cannot add a transport when the required fields are not filled in', () => {
      cy.get(addVehicle).click({
        force: true, // button might be covered by notification ex: BBGEO-11613
      });
      cy.get(submitButton).click();
      cy.get(inputsHaveError).each($input => {
        expect($input).to.have.css('border-color', 'rgb(255, 102, 102)');
      });

      cy.get(nameInput)
        .should('be.visible')
        .and('have.attr', 'placeholder', simpleModal.nameInput.placeholder.ru)
        .click();

      cy.get(errorTooltip).should('have.text', simpleModal.errorTooltips.nameNumberInputsEmpty.ru);
      cy.get(closerErrorTooltip).should('be.visible').click();
      cy.get(errorTooltip).should('not.exist');

      cy.get(numberInput)
        .should('be.visible')
        .and('have.attr', 'placeholder', simpleModal.numberInput.placeholder.ru)
        .click();

      cy.get(errorTooltip).should('have.text', simpleModal.errorTooltips.nameNumberInputsEmpty.ru);
      cy.get(nameInputLabel).click();
      cy.get(errorTooltip).should('not.exist');
    });

    it('add vehicle in the simple form', () => {
      const vehicleName = 'КАМАЗ [660]';
      const vehicleNumber = 'н777ту98 [660]';

      cy.get(nameInput).type(vehicleName);
      cy.get(numberInput).type(vehicleNumber);
      cy.get(submitButton).click();

      cy.get(table).contains(vehicleName).should('be.visible');
      cy.get(table).contains(vehicleNumber).should('be.visible').click();

      cy.get(deleteButton).click();
      cy.get(dialogRemoveButton).click();

      cy.wait(5000);
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/662

  describe('Edit and Delete Vehicle in the Simple Form Reference Book', function () {
    const uuid = () => Cypress._.random(0, 1e6);
    const id = uuid();

    const vehicleNameN1 = `araç 662${id}`;
    const vehicleNumberN1 = `34 AB 1234${id}`;
    const vehicleNameN2 = `vehicle 662${id}`;
    const vehicleNumberN2 = `BD51 SMR${id}`;
    const vehicleNameN3 = `taşıt 662${id}`;

    it('edit test vehicle', () => {
      cy.get(addVehicle).click({
        force: true, // button might be covered by notification ex: BBGEO-11613
      });

      cy.get(nameInput).type(vehicleNameN1);
      cy.get(numberInput).type(vehicleNumberN1);
      cy.get(submitButton).click();

      cy.get(table).contains(vehicleNameN1).click();
      cy.get(editVehicleTitle).should('have.text', vehicleNumberN1);
      cy.get(submitButton).should('be.disabled');

      cy.get(nameInput).clear().type(vehicleNameN3);
      cy.get(submitButton).should('not.be.disabled').click();
      cy.get(table).contains(vehicleNameN3).should('be.visible');
    });

    it('can not save a vehicle with dublicated number', () => {
      cy.get(addVehicle, { timeout: 10000 }).click({
        force: true, // button might be covered by notification ex: BBGEO-11613
      });

      cy.get(nameInput).type(vehicleNameN2);
      cy.get(numberInput).type(vehicleNumberN2);
      cy.get(submitButton).click();

      cy.get(table).contains(vehicleNumberN2).should('be.visible').click();
      cy.get(editVehicleTitle).should('have.text', vehicleNumberN2);

      cy.get(numberInput).clear().type(vehicleNumberN1);
      cy.get(submitButton).click();

      cy.get(inputsHaveError).should('have.css', 'border-color', 'rgb(255, 102, 102)');
      cy.get(numberInput).click();

      cy.get(errorTooltip).should('have.text', simpleModal.errorTooltips.duplicatedNumber.ru);
      cy.get(closerErrorTooltip).should('be.visible').click();
      cy.get(errorTooltip).should('not.exist');

      cy.get(closerModal).click();
      cy.get(table).contains(vehicleNumberN2).should('be.visible');
    });

    it('checking vehicle deletion', () => {
      cy.get(table).contains(vehicleNameN3).click();
      cy.get(deleteButton).click();

      cy.get(dialogRemoveTitle).should('have.text', simpleModal.formDeleteConfirm.title.ru);
      cy.get(dialogRemoveSubtitle).should('have.text', simpleModal.formDeleteConfirm.subtitle.ru);

      cy.get(dialogRemove).parents().eq(1).click(20, 20);

      cy.get(deleteButton).click();
      cy.get(dialogRemoveCloser).click();

      cy.get(deleteButton).click();
      cy.get(dialogRemoveCancelButton).click();

      cy.get(deleteButton).click();
      cy.get(dialogRemoveButton).click();

      cy.get(table).contains(vehicleNameN3).should('not.exist');

      cy.get(table).contains(vehicleNameN2).should('be.visible').click();
      cy.get(deleteButton).click();
      cy.get(dialogRemoveButton).click();
      cy.wait(2000);
    });
  });
});

describe('Testing the functionality in the extended form of adding a transport)', function () {
  const { addVehicle, vehicleFormModeSwitcher, table } = selectors.content.referenceBook.vehicles;
  const { submitButton, deleteButton } = selectors.content.referenceBook.vehicles.modal;
  const { dialogRemoveButton } = selectors.content.referenceBook.vehicles.modal.dialogRemoveVehicle;
  const {
    capacityWeightInput,
    capacityAccordionButton,
    costsAccordionButton,
    shiftsAccordionButton,
    courierAccordionButton,
    nameInput,
    numberInput,
  } = selectors.content.referenceBook.vehicles.modal.extended;
  const {
    startOfShiftDropdown,
    finishOfShiftDropdown,
    startOfShiftLabel,
    finishOfShiftLabel,
    parametersCourierInputsLabel,
    maxRunsInput,
    imeiInput,
    selectStartFinishShift,
    selectAdressGarage,
    geocodingAdressGarageInput,
    geocodingAdressGarageLabel,
  } = selectors.content.referenceBook.vehicles.modal.extended.couriersParameters;

  const { costKmInput, costHourInput, costFixedInput } =
    selectors.content.referenceBook.vehicles.modal.extended.costs;
  const { shiftTimeStart, shiftTimeEnd } =
    selectors.content.referenceBook.vehicles.modal.extended.shifts;

  // @see https://testpalm.yandex-team.ru/courier/testcases/657
  describe('Pre-filled data in the form of adding a transport', function () {
    it('should be pre-filled data', () => {
      cy.get(addVehicle).click({
        force: true, // button might be covered by notification ex: BBGEO-11613
      });
      cy.get(vehicleFormModeSwitcher).click();

      cy.get(costsAccordionButton).click();
      cy.get(costKmInput).should('have.value', 8).and('be.visible');
      cy.get(costHourInput).should('have.value', 100).and('be.visible');
      cy.get(costFixedInput).should('have.value', 3000).and('be.visible');

      cy.get(shiftsAccordionButton).click();
      cy.get(shiftTimeStart).should('have.value', '8:00').and('be.visible');
      cy.get(shiftTimeEnd).should('have.value', '21:00').and('be.visible');
    });

    it('add vehicle with not default shift value', () => {
      cy.get(shiftTimeStart).clear().type('1.8:00');
      cy.get(shiftTimeEnd).clear().type('1.21:00');

      cy.get(nameInput).type('TEST657');
      cy.get(numberInput).type('TEST657');
      cy.get(submitButton).click();
    });

    it('the changes should be saved', () => {
      cy.get(table).contains('TEST657').click();
      cy.get(vehicleFormModeSwitcher).click();

      cy.get(costsAccordionButton).click();
      cy.get(costKmInput).should('have.value', 8).and('be.visible');
      cy.get(costHourInput).should('have.value', 100).and('be.visible');
      cy.get(costFixedInput).should('have.value', 3000).and('be.visible');

      cy.get(shiftsAccordionButton).click();
      cy.get(shiftTimeStart).should('have.value', '1.8:00').and('be.visible');
      cy.get(shiftTimeEnd).should('have.value', '1.21:00').and('be.visible');

      cy.get(deleteButton).click();
      cy.get(dialogRemoveButton).click();
      cy.wait(2000);
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/959

  describe('Parameters Courier Vehicle Reference Book', function () {
    it('Create test vehicle', () => {
      cy.get(addVehicle).click({
        force: true, // button might be covered by notification ex: BBGEO-11613
      });
      cy.get(vehicleFormModeSwitcher).click();

      cy.get(nameInput).type('test case 959');
      cy.get(numberInput).type('test-959');
      cy.get(submitButton).click();
    });

    it('open courier parameters in form edit vehicle', () => {
      cy.get(table).contains('test case 959').click();

      cy.get(vehicleFormModeSwitcher).click();
      cy.get(courierAccordionButton).click();

      cy.get(startOfShiftDropdown).should('be.visible');
      cy.get(finishOfShiftDropdown).should('be.visible');

      cy.get(maxRunsInput).should('be.visible');
      cy.get(imeiInput).should('be.visible');

      cy.get(submitButton).should('be.disabled');
    });

    it('edit start shift and destination after shift', () => {
      cy.get(startOfShiftDropdown).click();
      cy.get(selectStartFinishShift).contains(shiftStartPoint.garageAndDepot.ru).click();

      cy.get(finishOfShiftDropdown).click();
      cy.get(selectStartFinishShift).contains(shiftFinishPoint.notReturnToDepot.ru).click();

      cy.get(geocodingAdressGarageInput).should('be.visible').click().type('СПб');
      cy.wait(2000);
      cy.get(selectAdressGarage).eq(-1).click();

      cy.get(startOfShiftDropdown)
        .should('have.text', shiftStartPoint.garageAndDepot.ru)
        .and('be.visible');
      cy.get(finishOfShiftDropdown)
        .should('have.text', shiftFinishPoint.notReturnToDepot.ru)
        .and('be.visible');

      cy.get(startOfShiftDropdown).click();
      cy.get(selectStartFinishShift).contains(shiftStartPoint.depot.ru).click();
      cy.get(finishOfShiftDropdown).click();
      cy.get(selectStartFinishShift).contains(shiftFinishPoint.garageAndDepot.ru).click();

      cy.get(geocodingAdressGarageInput).should('be.visible').click().type('Moscow');
      cy.wait(2000);
      cy.get(selectAdressGarage).eq(-1).click();

      cy.get(maxRunsInput).click().type('10');

      cy.get(startOfShiftDropdown).should('have.text', shiftStartPoint.depot.ru).and('be.visible');
      cy.get(finishOfShiftDropdown)
        .should('have.text', shiftFinishPoint.garageAndDepot.ru)
        .and('be.visible');

      cy.get(startOfShiftDropdown).click();
      cy.get(selectStartFinishShift).contains(shiftStartPoint.garage.ru).click();
      cy.get(finishOfShiftDropdown).click();
      cy.get(selectStartFinishShift).contains(shiftFinishPoint.depot.ru).click();

      cy.get(imeiInput).click().type('12345678910');

      cy.get(startOfShiftDropdown).should('have.text', shiftStartPoint.garage.ru).and('be.visible');
      cy.get(finishOfShiftDropdown)
        .should('have.text', shiftFinishPoint.depot.ru)
        .and('be.visible');
      cy.get(geocodingAdressGarageInput).should('be.visible');

      cy.get(finishOfShiftDropdown).click();
      cy.get(selectStartFinishShift).contains(shiftFinishPoint.garage.ru).click();

      cy.get(submitButton).should('not.be.disabled').click();
    });

    it('the changes in the courier parameters should be saved', () => {
      cy.get(table).contains('test case 959').click();

      cy.get(vehicleFormModeSwitcher).click();
      cy.get(courierAccordionButton).click();

      cy.get(startOfShiftDropdown).should('have.text', shiftStartPoint.garage.ru).and('be.visible');
      cy.get(finishOfShiftDropdown)
        .should('have.text', shiftFinishPoint.garage.ru)
        .and('be.visible');

      cy.get(geocodingAdressGarageInput).should('have.length', 2).should('be.visible');
      cy.get(geocodingAdressGarageLabel)
        .should('be.visible')
        .contains(parametersCouriers.labelsInputs.geocodingStartAdressGarage.ru);

      cy.get(geocodingAdressGarageLabel)
        .should('be.visible')
        .contains(parametersCouriers.labelsInputs.geocodingFinishAdressGarage.ru);

      cy.get(maxRunsInput).should('have.value', '10');
      cy.get(imeiInput).should('have.value', '12345678910');

      cy.get(deleteButton).click();
      cy.get(dialogRemoveButton).click();
      cy.wait(2000);
    });

    it('open courier parameters in form add vehicle', () => {
      cy.get(addVehicle).click({
        force: true, // button might be covered by notification ex: BBGEO-11613
      });
      cy.get(vehicleFormModeSwitcher).click();
      cy.get(courierAccordionButton).click();

      cy.get(startOfShiftLabel).should('have.text', shiftStartPoint.label.ru).and('be.visible');
      cy.get(startOfShiftDropdown).should('have.text', shiftStartPoint.depot.ru).and('be.visible');

      cy.get(finishOfShiftLabel).should('have.text', shiftFinishPoint.label.ru).and('be.visible');
      cy.get(finishOfShiftDropdown)
        .should('have.text', shiftFinishPoint.depot.ru)
        .and('be.visible');

      cy.get(maxRunsInput).should('be.visible');

      cy.get(imeiInput)
        .should('be.visible')
        .and('have.attr', 'placeholder', parametersCouriers.imeiInput.placeholder.ru);

      cy.get(parametersCourierInputsLabel)
        .contains(parametersCouriers.labelsInputs.maxRuns.ru)
        .should('have.class', 'field-label');

      cy.get(parametersCourierInputsLabel)
        .contains(parametersCouriers.labelsInputs.imei.ru)
        .should('have.class', 'field-label');
    });

    it('should expand accordion with invalid input vehicles by default', () => {
      cy.get(addVehicle).click({
        force: true, // button might be covered by notification ex: BBGEO-11613
      });

      cy.get(nameInput).type('test');
      cy.get(numberInput).type('test-42');

      cy.get(capacityAccordionButton).click();
      cy.get(capacityWeightInput).type('not a width');
      cy.get(capacityAccordionButton).click();

      cy.get(capacityWeightInput).should('not.be.visible');

      cy.get(submitButton).click();

      cy.get(capacityWeightInput).should('be.visible');
    });
  });
});
