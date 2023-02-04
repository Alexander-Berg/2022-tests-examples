package ru.yandex.intranet.d.web.controllers.front;

import java.util.Map;

import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunAmounts;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunFolderQuotaDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunRequestDto;

import static ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunRequestDto.EditedField.ABSOLUTE;

@SuppressWarnings("UnusedReturnValue") // API
public class UpdateProvisionDryRunRequestDtoBuilder {
    private String resourceId;
    private UpdateProvisionDryRunAmounts oldAmounts;
    private UpdateProvisionDryRunRequestDto.OldEditFormFields oldFormFields;
    private UpdateProvisionDryRunRequestDto.ChangedEditFormField newFormFields;
    private UpdateProvisionDryRunRequestDto.EditedField editedField;
    private Map<String, UpdateProvisionDryRunFolderQuotaDto> relatedResources = Map.of();

    public UpdateProvisionDryRunRequestDtoBuilder setResourceId(
            String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public UpdateProvisionDryRunRequestDtoBuilder setOldAmounts(
            UpdateProvisionDryRunAmounts oldAmounts) {
        this.oldAmounts = oldAmounts;
        return this;
    }

    public UpdateProvisionDryRunRequestDtoBuilder setOldFormFields(
            UpdateProvisionDryRunRequestDto.OldEditFormFields oldFormFields) {
        this.oldFormFields = oldFormFields;
        return this;
    }

    public UpdateProvisionDryRunRequestDtoBuilder setNewFormFields(
            UpdateProvisionDryRunRequestDto.ChangedEditFormField newFormFields) {
        this.newFormFields = newFormFields;
        return this;
    }

    public UpdateProvisionDryRunRequestDtoBuilder setEditedField(
            UpdateProvisionDryRunRequestDto.EditedField editedField) {
        this.editedField = editedField;
        return this;
    }

    public UpdateProvisionDryRunRequestDtoBuilder setRelatedResources(
            Map<String, UpdateProvisionDryRunFolderQuotaDto> relatedResources) {
        this.relatedResources = relatedResources;
        return this;
    }

    public UpdateProvisionDryRunRequestDtoBuilder setNewProvidedAbsolute(
            String newProvidedAbsolute
    ) {
        setEditedField(ABSOLUTE);
        setNewFormFields(new UpdateProvisionDryRunRequestDto.ChangedEditFormField(
                newProvidedAbsolute, // providedAbsolute
                null, // providedDelta
                null // forEditUnitId
        ));
        return this;
    }

    public UpdateProvisionDryRunRequestDtoBuilder setNewProvidedDelta(
            String newProvidedDelta
    ) {
        setEditedField(UpdateProvisionDryRunRequestDto.EditedField.DELTA);
        setNewFormFields(new UpdateProvisionDryRunRequestDto.ChangedEditFormField(
                null, // providedAbsolute
                newProvidedDelta, // providedDelta
                null // forEditUnitId
        ));
        return this;
    }

    public UpdateProvisionDryRunRequestDtoBuilder setNewEditUnitId(
            String newUnitId
    ) {
        setEditedField(UpdateProvisionDryRunRequestDto.EditedField.UNIT);
        setNewFormFields(new UpdateProvisionDryRunRequestDto.ChangedEditFormField(
                null, // providedAbsolute
                null, // providedDelta
                newUnitId // forEditUnitId
        ));
        return this;
    }

    public UpdateProvisionDryRunRequestDto build() {
        return new UpdateProvisionDryRunRequestDto(
                resourceId, oldAmounts, oldFormFields, newFormFields, editedField, relatedResources
        );
    }
}
