package ru.yandex.partner.core.entity.user.repository;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.common.editablefields.EditableFieldsService;
import ru.yandex.partner.core.entity.user.model.BaseUser;
import ru.yandex.partner.core.entity.user.model.User;
import ru.yandex.partner.core.entity.user.service.UserValidationService;
import ru.yandex.partner.core.entity.user.type.common.CommonUserConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.core.entity.user.defect.UserDefectIds.CommonUser.INCORRECT_EMAIL;

@CoreTest
class UserTypedRepositoryTest {

    @Autowired
    UserValidationService readValidationService;

    @Autowired
    UserTypedRepository repository;

    @Autowired
    EditableFieldsService<BaseUser> editableFieldsService;

    @Test
    void getUsersByIds() {
        List<User> users = repository.getSafely(List.of(1009L), User.class);
        assertThat(users.size()).isEqualTo(1);

        User user = users.get(0);
        assertThat(user.getId()).isEqualTo(1009);
        assertThat(user.getLogin()).isEqualToIgnoringCase("mocked-yan-partner");
        assertThat(user.getRoles().size()).isEqualTo(1);
        assertThat(user.getFeatures())
                .containsExactlyInAnyOrder("business_rules",
                        "design_auction_native",
                        "turbo_desktop_available",
                        "mobile_floorad_available",
                        "simple_inapp");


        assertThat(user.getHasApproved()).isFalse();
        assertThat(user.getHasTutbyAgreement()).isFalse();
        assertThat(user.getHasCommonOffer()).isFalse();
        assertThat(user.getHasMobileMediation()).isFalse();
        assertThat(user.getHasRsya()).isTrue();
        assertThat(user.getAdfoxOffer()).isNull();
        assertThat(user.getPaidOffer()).isNull();
        assertThat(user.getAllowedDesignAuctionNativeOnly()).isTrue();
        assertThat(user.getContentBlockEditTemplateAllowed()).isNull();
        assertThat(user.getInn()).isEqualTo("111222333444");
        assertThat(user.getCooperationForm()).isEqualTo("ph");
        assertThat(user.getSelfEmployed()).isEqualTo(1);
        assertThat(user.getSelfEmployedRequestId()).isNull();
        assertThat(user.getIsDmLite()).isFalse();
        assertThat(user.getIsEfirBlogger()).isFalse();
        assertThat(user.getPayoneer()).isTrue();
        assertThat(user.getPayoneerCurrency()).isEqualTo("USD");
        assertThat(user.getPayoneerPayeeId()).isNull();
        assertThat(user.getPayoneerStep()).isEqualTo(3);
        assertThat(user.getPayoneerUrl()).isEqualTo("https://payouts.sandbox.payoneer.com" +
                "/partners/lp.aspx?token=3329262e318344f2a6be86924bf0a6cdF91C67EB4A");
    }

    @Test
    void getUsersByIdsWithNullValues() {
        List<User> users = repository.getSafely(List.of(668991881L), User.class);
        assertThat(users.size()).isEqualTo(1);

        User user = users.get(0);
        assertThat(user.getId()).isEqualTo(668991881L);
        assertThat(user.getLogin()).isEqualToIgnoringCase("mocked-yndx-adfox");
        assertThat(user.getRoles().size()).isEqualTo(2);
        assertThat(user.getFeatures()).containsExactlyInAnyOrder("simple_inapp");

        assertThat(user.getHasApproved()).isFalse();
        assertThat(user.getHasTutbyAgreement()).isFalse();
        assertThat(user.getHasCommonOffer()).isFalse();
        assertThat(user.getHasMobileMediation()).isFalse();
        assertThat(user.getHasRsya()).isTrue();
        assertThat(user.getAdfoxOffer()).isNull();
        assertThat(user.getPaidOffer()).isNull();
        assertThat(user.getAllowedDesignAuctionNativeOnly()).isNull();
        assertThat(user.getContentBlockEditTemplateAllowed()).isNull();
        assertThat(user.getInn()).isNull();
        assertThat(user.getCooperationForm()).isNull();
        assertThat(user.getSelfEmployed()).isNull();
        assertThat(user.getSelfEmployedRequestId()).isNull();
        assertThat(user.getIsDmLite()).isFalse();
        assertThat(user.getIsEfirBlogger()).isFalse();
        assertThat(user.getPayoneer()).isNull();
        assertThat(user.getPayoneerCurrency()).isNull();
        assertThat(user.getPayoneerPayeeId()).isNull();
        assertThat(user.getPayoneerStep()).isNull();
        assertThat(user.getPayoneerUrl()).isNull();


    }

    @Test
    void getEditableModelProperties() {
        List<User> users = repository.getSafely(List.of(668991881L), User.class);

        assertThat(editableFieldsService.calculateEditableModelPropertiesHolder(users.get(0), null)
                .containsAnyPath(new HashSet<>(CommonUserConstants.EDIT_FORBIDDEN_MODEL_PROPERTIES))
        ).isFalse();
    }

    @Test
    void validateUserWithIncorrectEmail() {
        List<User> users = repository.getSafely(List.of(668991881L), User.class);
        users.get(0).setEmail("incorrect@email@ru");
        ValidationResult<List<? extends BaseUser>, Defect> vr =
                readValidationService.validate(users);

        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isOne();
        assertThat(defectInfos.get(0).getDefect().defectId()).isEqualTo(INCORRECT_EMAIL);
    }

    @Test
    void validateValidUser() {
        List<User> users = repository.getSafely(List.of(668991881L), User.class);
        ValidationResult<List<? extends BaseUser>, Defect> vr =
                readValidationService.validate(users);

        List<DefectInfo<Defect>> defectInfos = vr.flattenErrors();
        assertThat(defectInfos.size()).isZero();
    }
}
