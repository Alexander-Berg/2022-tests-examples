package ru.yandex.partner.core.entity.user.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.entity.user.actions.factories.UserEditFactory;
import ru.yandex.partner.core.entity.user.model.BaseUser;
import ru.yandex.partner.core.entity.user.model.CommonUser;
import ru.yandex.partner.core.entity.user.model.User;
import ru.yandex.partner.core.entity.user.repository.UserTypedRepository;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.validation.defects.ids.PartnerCollectionDefectIds;
import ru.yandex.partner.libs.memcached.MemcachedService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.partner.core.entity.user.model.prop.CommonUserMidnamePropHolder.MIDNAME;
import static ru.yandex.partner.core.entity.user.model.prop.UserOptsInternalHasApprovedPropHolder.HAS_APPROVED;
import static ru.yandex.partner.core.entity.user.model.prop.UserOptsInternalPayoneerCurrencyPropHolder.PAYONEER_CURRENCY;
import static ru.yandex.partner.core.entity.user.model.prop.UserOptsInternalPayoneerPayeeIdPropHolder.PAYONEER_PAYEE_ID;
import static ru.yandex.partner.core.entity.user.model.prop.UserOptsInternalPayoneerStepPropHolder.PAYONEER_STEP;
import static ru.yandex.partner.dbschema.partner.Tables.USERS;

@ExtendWith(MySqlRefresher.class)
@CoreTest
class UserUpdateServiceTest {
    @Autowired
    UserUpdateOperationFactory userUpdateOperationFactory;

    @Autowired
    ActionPerformer actionPerformer;

    @Autowired
    UserTypedRepository repository;

    @Autowired
    MemcachedService memcachedService;

    @Autowired
    DSLContext dslContext;

    @Autowired
    UserEditFactory userEditFactory;

    @Test
    void createPartialUpdateOperation() {
        List<User> users = repository.getSafely(List.of(1009L), User.class);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0).getMidname()).isEmpty();

        List<ModelChanges<CommonUser>> modelChanges =
                List.of(ModelChanges.build(1009L, CommonUser.class, MIDNAME, "new_value"));

        List<ModelChanges<BaseUser>> modelChangesList =
                modelChanges.stream().map(mc -> mc.castModel(BaseUser.class)).collect(Collectors.toList());

        MassResult<Long> result = userUpdateOperationFactory.createUpdateOperation(
                Applicability.PARTIAL,
                modelChangesList
        ).prepareAndApply();

        users = repository.getSafely(List.of(1009L), User.class);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0).getMidname()).isEqualTo("new_value");
    }

    @Test
    void createPartialUpdateOperationOpts() {
        List<User> users = repository.getSafely(List.of(1009L), User.class);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0).getHasApproved()).isFalse();

        String opts = dslContext.select(USERS.OPTS)
                .from(USERS)
                .where(USERS.ID.eq(1009L))
                .fetchOne()
                .get(USERS.OPTS);
        assertThat(opts).isEqualTo("{\"inn\": \"111222333444\", \"has_rsya\": 1, \"payoneer\": 1, " +
                "\"has_approved\": 0, \"payoneer_url\": \"https://payouts.sandbox.payoneer.com/partners/" +
                "lp.aspx?token=3329262e318344f2a6be86924bf0a6cdF91C67EB4A\", " +
                "\"payoneer_step\": 3, \"self_employed\": 1, \"cooperation_form\": \"ph\", \"has_common_offer\": 0, " +
                "\"payoneer_currency\": \"USD\", \"has_tutby_agreement\": 0, " +
                "\"has_mobile_mediation\": 0, \"allowed_design_auction_native_only\": 1}"
        );

        List<ModelChanges<CommonUser>> modelChanges =
                List.of(ModelChanges.build(1009L, CommonUser.class, HAS_APPROVED, true));

        List<ModelChanges<BaseUser>> modelChangesList =
                modelChanges.stream().map(mc -> mc.castModel(BaseUser.class)).collect(Collectors.toList());

        MassResult<Long> result = userUpdateOperationFactory.createUpdateOperation(
                Applicability.PARTIAL,
                modelChangesList
        ).prepareAndApply();

        users = repository.getSafely(List.of(1009L), User.class);
        assertThat(users.size()).isEqualTo(1);
        assertThat(users.get(0).getHasApproved()).isTrue();

        opts = dslContext.select(USERS.OPTS)
                .from(USERS)
                .where(USERS.ID.eq(1009L))
                .fetchOne()
                .get(USERS.OPTS);
        assertThat(opts).isEqualTo("{\"inn\": \"111222333444\", \"has_rsya\": 1, \"payoneer\": 1, " +
                "\"has_approved\": 1, \"payoneer_url\": \"https://payouts.sandbox.payoneer.com/partners/" +
                "lp.aspx?token=3329262e318344f2a6be86924bf0a6cdF91C67EB4A\", " +
                "\"payoneer_step\": 3, \"self_employed\": 1, \"cooperation_form\": \"ph\", \"has_common_offer\": 0, " +
                "\"payoneer_currency\": \"USD\", \"has_tutby_agreement\": 0, " +
                "\"has_mobile_mediation\": 0, \"allowed_design_auction_native_only\": 1}"
        );
    }

    @Test
    void failPayoneerUpdateOperation() {
        List<ModelChanges<User>> modelChanges =
                List.of(ModelChanges.build(1009L, User.class, PAYONEER_CURRENCY, "CURRENCY")
                        .process(-1, PAYONEER_STEP)
                        .process("123456789012345678901234567890+", PAYONEER_PAYEE_ID));

        List<ModelChanges<BaseUser>> modelChangesList =
                modelChanges.stream().map(mc -> mc.castModel(BaseUser.class)).collect(Collectors.toList());

        MassResult<Long> result = userUpdateOperationFactory.createUpdateOperation(
                Applicability.PARTIAL,
                modelChangesList
        ).prepareAndApply();

        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();

        List<DefectInfo<Defect>> defectInfos = result.getValidationResult().flattenErrors();
        assertThat(defectInfos.size()).isEqualTo(3);

        Set<DefectId> defectIdSet = defectInfos.stream()
                .map(DefectInfo::getDefect)
                .map(Defect::defectId)
                .collect(Collectors.toSet());
        assertThat(defectIdSet.contains(NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN))
                .isTrue();
        assertThat(defectIdSet.contains(PartnerCollectionDefectIds.Size.MUST_BE_IN_COLLECTION_WITH_PRESENTATION))
                .isTrue();

    }

    @Test
    public void testCacheInvalidationNotCalledOnSuccess() {
        var action = userEditFactory.edit(List.of(new ModelChanges<>(1009L, User.class)
                .process("some@valid.email", User.EMAIL)));

        actionPerformer.doActions(action);

        verify(memcachedService, times(0)).delete(eq("available_resources"), anyString());
    }

    @Test
    void updateForbiddenPropertiesFail() {
        // Пытаемся обновить запрещённое для обновления поле
        ModelChanges<User> modelChanges = ModelChanges.build(1009L, User.class,
                CommonUser.ID, 100L
        );
        List<ModelChanges<BaseUser>> modelChangesList =
                List.of(modelChanges).stream().map(mc -> mc.castModel(BaseUser.class)).collect(Collectors.toList());
        MassResult<Long> result = userUpdateOperationFactory.createUpdateOperation(
                Applicability.PARTIAL,
                modelChangesList
        ).prepareAndApply();
        // Ошибка валидации
        assertThat(result.getErrorCount()).isOne();
        assertThat(result.getSuccessfulCount()).isZero();

    }

}
