package ru.yandex.partner.core.entity.user.actions;

import java.util.List;
import java.util.Set;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.action.ActionContextFacade;
import ru.yandex.partner.core.action.ActionPerformer;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.user.actions.factories.UserBlockFactory;
import ru.yandex.partner.core.entity.user.actions.factories.UserEditFactory;
import ru.yandex.partner.core.entity.user.filter.UserFilters;
import ru.yandex.partner.core.entity.user.model.User;
import ru.yandex.partner.core.entity.user.service.UserService;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.core.multistate.user.UserStateFlag;
import ru.yandex.partner.dbschema.partner.Tables;
import ru.yandex.partner.libs.annotation.PartnerTransactional;
import ru.yandex.partner.libs.multistate.graph.MultistateGraph;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.has;

@ExtendWith(MySqlRefresher.class)
@CoreTest
class UserDoActionTest {
    public static final Set<ModelProperty<?, ?>> BASE_USER_FIELDS = Set.of(
            User.ID,
            User.LOGIN,
            User.MULTISTATE,
            User.NAME,
            User.LASTNAME
    );

    @Autowired
    ActionPerformer actionPerformer;

    @Autowired
    MultistateGraph<User, UserStateFlag> multistateGraph;

    @Autowired
    UserEditFactory userEditFactory;

    @Autowired
    UserBlockFactory userBlockFactory;

    @Autowired
    UserService userService;

    @Autowired
    ActionContextFacade actionContextFacade;

    @Autowired
    DSLContext dsl;

    @Test
    void simpleDoAction() {
        List<Long> ids = List.of(0L, 1008L);

        UserActionEdit userActionEdit = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class).process("cron-test", User.LOGIN),
                new ModelChanges<>(1008L, User.class).process("moderator-test", User.LOGIN)
        ));

        UserActionEdit userActionEdit2 = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class).process("cron-name-test", User.NAME),
                new ModelChanges<>(1008L, User.class).process("moderator-name-test", User.NAME)
        ));

        int actionsCountBefore = userActionsCount();

        var result = actionPerformer.doActions(userActionEdit, userActionEdit2);


        assertThat(result.isCommitted()).isTrue();
        int actionsCountAfter = userActionsCount();

        assertThat(actionsCountAfter)
                .describedAs("edit actions write to action logs too")
                .isGreaterThan(actionsCountBefore);

        var users = userService.findAll(QueryOpts.forClass(User.class)
                .withFilter(CoreFilterNode.in(UserFilters.ID, ids))
                .withProps(BASE_USER_FIELDS)
        );

        assertThat(users.get(0).getLogin()).isEqualTo("cron-test");
        assertThat(users.get(0).getName()).isEqualTo("cron-name-test");
        assertThat(users.get(1).getLogin()).isEqualTo("moderator-test");
        assertThat(users.get(1).getName()).isEqualTo("moderator-name-test");
    }

    @Test
    void simpleDoActionValidationFail() {
        UserActionEdit validEditAction = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class).process("cron-test", User.LOGIN),
                new ModelChanges<>(1008L, User.class).process("moderator-test", User.LOGIN)
        ));

        UserActionEdit brokenEditAction = userEditFactory.edit(List.of(
                new ModelChanges<>(1008L, User.class).process("invalid-email", User.EMAIL)
        ));

        int actionsCountBefore = userActionsCount();
        // should complete with validation errors
        actionPerformer.doActions(validEditAction, brokenEditAction);
        int actionsCountAfter = userActionsCount();

        assertThat(actionsCountAfter)
                .describedAs("broken actions should not write to action log")
                .isEqualTo(actionsCountBefore);
    }

    @Test
    void sequentialDoAction() {
        List<Long> ids = List.of(0L, 1008L);

        UserActionEdit userActionEdit = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class).process("cron-test", User.LOGIN),
                new ModelChanges<>(1008L, User.class).process("moderator-test", User.LOGIN)
        ));

        UserActionEdit userActionEdit2 = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class).process("cron-name-test", User.NAME),
                new ModelChanges<>(1008L, User.class).process("moderator-name-test", User.NAME)
        ));

        assertThat(actionContextFacade.geThreadInsideDoAction()).isEqualTo(0L);
        actionPerformer.doActions(userActionEdit);
        assertThat(actionContextFacade.geThreadInsideDoAction()).isEqualTo(0L);
        actionPerformer.doActions(userActionEdit2);
        assertThat(actionContextFacade.geThreadInsideDoAction()).isEqualTo(0L);

        var users = userService.findAll(QueryOpts.forClass(User.class)
                .withFilter(CoreFilterNode.in(UserFilters.ID, ids))
                .withProps(BASE_USER_FIELDS)
        );

        assertThat(users.get(0).getLogin()).isEqualTo("cron-test");
        assertThat(users.get(0).getName()).isEqualTo("cron-name-test");
        assertThat(users.get(1).getLogin()).isEqualTo("moderator-test");
        assertThat(users.get(1).getName()).isEqualTo("moderator-name-test");
    }

    @Test
    void blockTransitionAction() {
        List<Long> ids = List.of(1008L);

        int actionsCountBefore = userActionsCount();

        actionPerformer.doActions(userBlockFactory.block(ids));

        int actionsCountAfter = userActionsCount();

        assertThat(actionsCountAfter).isGreaterThan(actionsCountBefore);

        var users = userService.findAll(QueryOpts.forClass(User.class)
                .withFilter(CoreFilterNode.in(UserFilters.ID, ids))
                .withProps(BASE_USER_FIELDS)
        );

        assertThat(users.get(0).getMultistate())
                .matches(has(UserStateFlag.BLOCKED), "user is blocked");
    }

    @Test
    void blockTransitionActionFailed() {
        List<Long> ids = List.of(1008L, 0L);

        int actionsCountBefore = userActionsCount();

        var edit = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class)
                        .process("cron-name-test", User.NAME)
        ));

        assertThat(actionContextFacade.geThreadInsideDoAction()).isEqualTo(0L);
        var result =  actionPerformer.doActions(userBlockFactory.block(ids), edit);
        assertThat(result.isCommitted()).isEqualTo(false);
        assertThat(actionContextFacade.geThreadInsideDoAction()).isEqualTo(0L);

        int actionsCountAfter = userActionsCount();

        assertThat(actionsCountAfter).isEqualTo(actionsCountBefore);

        var users = userService.findAll(QueryOpts.forClass(User.class)
                .withFilter(CoreFilterNode.in(UserFilters.ID, ids))
                .withProps(BASE_USER_FIELDS)
        );

        assertThat(users.get(0).getMultistate())
                .matches(has(UserStateFlag.BLOCKED).negate(), "user is not blocked");
    }

    @Test
    void blockTransitionActionNestedFailed() {
        List<Long> ids = List.of(1008L, 0L);

        int actionsCountBefore = userActionsCount();

        UserActionBlock block = userBlockFactory.block(ids, true);

        assertThat(actionContextFacade.geThreadInsideDoAction()).isEqualTo(0L);
        var result = actionPerformer.doActions(block);
        assertThat(result.isCommitted()).isEqualTo(false);
        assertThat(actionContextFacade.geThreadInsideDoAction()).isEqualTo(0L);

        int actionsCountAfter = userActionsCount();

        assertThat(actionsCountAfter).isEqualTo(actionsCountBefore);

        var users = userService.findAll(QueryOpts.forClass(User.class)
                .withFilter(CoreFilterNode.in(UserFilters.ID, ids))
                .withProps(BASE_USER_FIELDS)
        );

        assertThat(users)
                .allMatch(user -> !user.getMultistate().test(UserStateFlag.BLOCKED),
                        "no user is blocked");
    }

    private int userActionsCount() {
        return dsl.selectCount().from(Tables.USERS_ACTION_LOG)
                .fetchOptional(0, Integer.class).orElse(0);
    }

    @Test
    @PartnerTransactional
    public void testNonErasingUpdatedData() {

        List<Long> ids = List.of(0L);

        UserActionEdit userActionEdit = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class).process("cron_lastName", User.LASTNAME)
        ));

        var context =
                actionContextFacade.getActionContext(User.class, UserActionContext.class);
        context.init();
        var containers =
                context.getContainers(ids, Set.of(), true);

        userActionEdit.onAction(context, containers);
        var container = context.getContainers(ids,
                Set.of(User.ID, User.LOGIN, User.MULTISTATE, User.NAME, User.LASTNAME), false).get(0);
        var notChanged = container.getNonChangedItem();
        var changed = container.getItem();
        assertThat(changed.getLastname()).isNotEqualTo(notChanged.getLastname());
    }

    @Test
    void validationExceptionTest() {
        List<Long> ids = List.of(0L, 1008L);

        UserActionEdit userActionEdit = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class).process("good@email.ru", User.EMAIL),
                new ModelChanges<>(1008L, User.class).process("incorrect_email", User.EMAIL)
        ));

        var result = actionPerformer.doActions(userActionEdit);
        assertThat(result.isCommitted()).isFalse();
        var err = result.getErrors();
        assertThat(err.keySet().contains(User.class)).isTrue();
        assertThat(err.get(User.class).keySet().contains(1008L)).isTrue();
        var users = userService.findAll(QueryOpts.forClass(User.class)
                .withFilter(CoreFilterNode.in(UserFilters.ID, ids))
                .withProps(BASE_USER_FIELDS)
        );
        assertThat(users.get(0).getEmail()).isNotEqualTo("good@email.ru");
        assertThat(users.get(1).getEmail()).isNotEqualTo("incorrect_email");
    }

    @Test
    void partlyValidTest() {
        List<Long> ids = List.of(0L, 1008L);

        UserActionEdit userActionEdit = userEditFactory.edit(List.of(
                new ModelChanges<>(0L, User.class).process("good@email.ru", User.EMAIL),
                new ModelChanges<>(1008L, User.class).process("incorrect_email", User.EMAIL)
        ));

        var result = actionPerformer.doActions(false, userActionEdit);

        assertThat(result.isCommitted()).isTrue();
        var users = userService.findAll(QueryOpts.forClass(User.class)
                .withFilter(CoreFilterNode.in(UserFilters.ID, ids))
                .withProps(BASE_USER_FIELDS)
        );
        assertThat(users.get(0).getEmail()).isEqualTo("good@email.ru");
        assertThat(users.get(1).getEmail()).isNotEqualTo("incorrect_email");
    }

}
