package ru.yandex.intranet.d.web.controllers.front;

import java.util.Map;
import java.util.Set;

import ru.yandex.intranet.d.web.model.AmountDto;
import ru.yandex.intranet.d.web.model.ValidationMessageDto;
import ru.yandex.intranet.d.web.model.ValidationMessagesDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunAnswerDto;

import static ru.yandex.intranet.d.web.model.ValidationMessageLevelDto.INFO;

/**
 * Builder.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 26-11-2021
 */
public class UpdateProvisionDryRunAnswerDtoBuilder extends UpdateProvisionDryRunAnswerDto.Builder {

    public UpdateProvisionDryRunAnswerDtoBuilder setBalance(String balance) {
        super.setBalance(balance);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setProvidedAbsolute(String providedAbsolute) {
        super.setProvidedAbsolute(providedAbsolute);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setProvidedRatio(String providedRatio) {
        super.setProvidedRatio(providedRatio);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setAllocated(String allocated) {
        super.setAllocated(allocated);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setAllocatedRatio(String allocatedRatio) {
        super.setAllocatedRatio(allocatedRatio);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setForEditUnitId(String forEditUnitId) {
        super.setForEditUnitId(forEditUnitId);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setProvidedDelta(String providedDelta) {
        super.setProvidedDelta(providedDelta);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setProvidedAbsoluteInMinAllowedUnit(
            String providedAbsoluteInMinAllowedUnit) {
        super.setProvidedAbsoluteInMinAllowedUnit(providedAbsoluteInMinAllowedUnit);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setMinAllowedUnitId(String minAllowedUnitId) {
        super.setMinAllowedUnitId(minAllowedUnitId);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setBalanceAmount(AmountDto balanceAmount) {
        super.setBalanceAmount(balanceAmount);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setProvidedAmount(AmountDto providedAmount) {
        super.setProvidedAmount(providedAmount);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setDeltaAmount(AmountDto deltaAmount) {
        super.setDeltaAmount(deltaAmount);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setRelatedResources(Map<String,
            UpdateProvisionDryRunAnswerDto> relatedResources) {
        super.setRelatedResources(relatedResources);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder addRelatedResource(
            String resourceId,
            UpdateProvisionDryRunAnswerDto relatedResource) {
        super.addRelatedResource(resourceId, relatedResource);
        return this;
    }

    public UpdateProvisionDryRunAnswerDto build() {
        return super.build();
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setValidationInfo() {
        setValidationMessages(null);
        return this;
    }

    public UpdateProvisionDryRunAnswerDtoBuilder setValidationInfo(String key, String value) {
        setValidationMessages(new ValidationMessagesDto(Map.of(key, Set.of(new ValidationMessageDto(INFO, value)))));
        return this;
    }
}
