package ru.yandex.partner.core.entity.user.multistate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.partner.core.action.Action;
import ru.yandex.partner.core.entity.user.actions.UserActionsEnum;
import ru.yandex.partner.core.entity.user.model.User;
import ru.yandex.partner.core.messages.TextTemplateMsg;
import ru.yandex.partner.core.messages.UserActionMsg;
import ru.yandex.partner.core.multistate.user.UserMultistate;
import ru.yandex.partner.core.multistate.user.UserStateFlag;
import ru.yandex.partner.libs.i18n.GettextMsg;
import ru.yandex.partner.libs.multistate.action.ActionNameHolder;

import static java.util.function.Predicate.not;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.any;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.empty;
import static ru.yandex.partner.libs.multistate.MultistatePredicates.has;

public class UserMultistateGraphTest {
    public static final Map<String, GettextMsg> ALL_ACTIONS = new HashMap<>();
    public static final Map<Long, Set<String>> ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE = new HashMap<>();

    private UserMultistateGraph userMultistateGraph;

    @BeforeAll
    static void beforeAll() {
        fillAllActions();
        fillAllowedActions();
    }

    @BeforeEach
    public void init() {
        CanChangeRequisitesCheck canChangeRequisitesCheck = mock(CanChangeRequisitesCheck.class);

        when(canChangeRequisitesCheck.check(anyList()))
                .thenReturn(ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.keySet().stream().map(it -> true).toList());

        userMultistateGraph =
                new UserMultistateGraph(new UserActionChecksService(canChangeRequisitesCheck));
    }

    @Test
    public void testGetAllowedActions() {

        Long userId = 123L;

        List<User> models = new ArrayList<>(ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.size());
        List<Set<Action>> expectedResult = new ArrayList<>(ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.size());

        ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.forEach((multistateValue, actions) -> {
            models.add(mockUser(userId, multistateValue));
            expectedResult.add(actions.stream()
                    .map(action -> new Action(action, ALL_ACTIONS.get(action))).collect(Collectors.toSet()));
        });

        Assertions.assertEquals(expectedResult, userMultistateGraph.getAllowedActions(models));

        for (int i = 0; i < models.size(); i++) {
            Assertions.assertEquals(expectedResult.get(i), userMultistateGraph.getAllowedActions(models.get(i)));
        }

    }

    @Test
    public void testCheckActionAllowed() {

        Long userId = 123L;

        Set<String> actionNames = new HashSet<>(ALL_ACTIONS.keySet());
        actionNames.add("nonexistent_action");

        List<User> models = new ArrayList<>(ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.size());
        Map<String, List<Boolean>> expectedResult = actionNames.stream()
                .collect(Collectors.toMap(a -> a, a -> new ArrayList<>(ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.size())));
        List<Map<String, Boolean>> expectedByModel = new ArrayList<>(ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.size());

        ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.forEach((multistateValue, actions) -> {
            models.add(mockUser(userId, multistateValue));
            expectedResult.forEach((actionName, result) -> result.add(actions.contains(actionName)));
            expectedByModel.add(actionNames.stream().collect(Collectors.toMap(a -> a, actions::contains)));
        });

        Assertions.assertEquals(expectedResult, userMultistateGraph.checkActionsAllowed(actionNames, models));

        actionNames.forEach(actionName -> {
            Assertions.assertEquals(expectedResult.get(actionName),
                    userMultistateGraph.checkActionAllowed(actionName, models));
            for (int i = 0; i < models.size(); i++) {
                Assertions.assertEquals(expectedResult.get(actionName).get(i),
                        userMultistateGraph.checkActionAllowed(actionName, models.get(i)));
            }
        });

        for (int i = 0; i < models.size(); i++) {
            Assertions.assertEquals(expectedByModel.get(i), userMultistateGraph.checkActionsAllowed(actionNames,
                    models.get(i)));
        }

    }

    @Test
    public void getReachableMultistatesTest() {
        Set<UserMultistate> expected = ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.keySet().stream()
                .map(UserMultistate::new).collect(Collectors.toSet());
        Assertions.assertEquals(expected, userMultistateGraph.getReachableMultistates());
    }

    @Test
    public void getMultsitatesForPredicateTest() {
        Set<UserMultistate> allMultistates = ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.keySet().stream()
                .map(UserMultistate::new).collect(Collectors.toSet());
        Assertions.assertEquals(allMultistates, userMultistateGraph.getMultistatesForPredicate(any()));

        Set<UserMultistate> emptyMultistates = Set.of(new UserMultistate(0L));
        Assertions.assertEquals(emptyMultistates, userMultistateGraph.getMultistatesForPredicate(empty()));

        Set<UserMultistate> nonEmptyMultistates = new HashSet<>(allMultistates);
        nonEmptyMultistates.removeAll(emptyMultistates);
        Assertions.assertEquals(nonEmptyMultistates, userMultistateGraph.getMultistatesForPredicate(not(empty())));

        Set<UserMultistate> haveFlagMultistates = List.of(1L, 3L, 9L, 11L, 17L, 19L, 25L, 27L).stream()
                .map(UserMultistate::new).collect(Collectors.toSet());
        Assertions.assertEquals(haveFlagMultistates,
                userMultistateGraph.getMultistatesForPredicate(has(UserStateFlag.CONTACTS_PROVIDED)));

        Set<UserMultistate> notHaveFlagMultustates = new HashSet<>(allMultistates);
        notHaveFlagMultustates.removeAll(haveFlagMultistates);
        Assertions.assertEquals(notHaveFlagMultustates,
                userMultistateGraph.getMultistatesForPredicate(not(has(UserStateFlag.CONTACTS_PROVIDED))));

        Set<UserMultistate> haveTwoFlagsMultistates = List.of(3L, 11L, 19L, 27L).stream()
                .map(UserMultistate::new).collect(Collectors.toSet());
        Assertions.assertEquals(haveTwoFlagsMultistates, userMultistateGraph.getMultistatesForPredicate(
                has(UserStateFlag.CONTACTS_PROVIDED).and(has(UserStateFlag.NEED_CREATE_IN_BANNER_STORE))
        ));

    }

    protected User mockUser(Long userId, Long multistateValue) {
        UserMultistate multistate = new UserMultistate(multistateValue);
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getMultistate()).thenReturn(multistate);
        return user;
    }

    private static void fillAllowedActions() {
        Map<Long, Set<ActionNameHolder>> restricted = new HashMap<>();
        restricted.put(0L,
                Set.of(UserActionsEnum.CREATED_PARTNER_IN_BANNER_STORE,
                        UserActionsEnum.YAN_CONTRACT_READY,
                        UserActionsEnum.RESET_BLOCKED));
        restricted.put(1L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.PROVIDE_CONTACTS,
                        UserActionsEnum.CREATED_PARTNER_IN_BANNER_STORE,
                        UserActionsEnum.YAN_CONTRACT_READY,
                        UserActionsEnum.RESET_BLOCKED));
        restricted.put(2L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.REQUEST_CREATE_IN_BANNER_STORE,
                        UserActionsEnum.YAN_CONTRACT_READY,
                        UserActionsEnum.RESET_BLOCKED));
        restricted.put(3L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.PROVIDE_CONTACTS,
                        UserActionsEnum.REQUEST_CREATE_IN_BANNER_STORE,
                        UserActionsEnum.YAN_CONTRACT_READY,
                        UserActionsEnum.RESET_BLOCKED));
        restricted.put(8L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.CREATED_PARTNER_IN_BANNER_STORE,
                        UserActionsEnum.REQUEST_YAN_CONTRACT,
                        UserActionsEnum.RESET_BLOCKED));
        restricted.put(9L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.PROVIDE_CONTACTS,
                        UserActionsEnum.CREATED_PARTNER_IN_BANNER_STORE,
                        UserActionsEnum.REQUEST_YAN_CONTRACT,
                        UserActionsEnum.RESET_BLOCKED
                ));
        restricted.put(10L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.REQUEST_CREATE_IN_BANNER_STORE,
                        UserActionsEnum.REQUEST_YAN_CONTRACT,
                        UserActionsEnum.RESET_BLOCKED));
        restricted.put(11L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.PROVIDE_CONTACTS,
                        UserActionsEnum.REQUEST_CREATE_IN_BANNER_STORE,
                        UserActionsEnum.REQUEST_YAN_CONTRACT,
                        UserActionsEnum.RESET_BLOCKED
                ));
        restricted.put(16L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.CREATED_PARTNER_IN_BANNER_STORE,
                        UserActionsEnum.YAN_CONTRACT_READY,
                        UserActionsEnum.SET_USER_ROLE,
                        UserActionsEnum.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS,
                        UserActionsEnum.EDIT,
                        UserActionsEnum.SET_EXCLUDED_DOMAINS,
                        UserActionsEnum.SET_EXCLUDED_PHONES,
                        UserActionsEnum.SET_BLOCKED,
                        UserActionsEnum.CHANGE_CONTRACT));
        restricted.put(17L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.PROVIDE_CONTACTS,
                        UserActionsEnum.CREATED_PARTNER_IN_BANNER_STORE,
                        UserActionsEnum.YAN_CONTRACT_READY,
                        UserActionsEnum.SET_USER_ROLE,
                        UserActionsEnum.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS,
                        UserActionsEnum.EDIT,
                        UserActionsEnum.SET_EXCLUDED_DOMAINS,
                        UserActionsEnum.SET_EXCLUDED_PHONES,
                        UserActionsEnum.SET_BLOCKED,
                        UserActionsEnum.CHANGE_CONTRACT));
        restricted.put(18L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.REQUEST_CREATE_IN_BANNER_STORE,
                        UserActionsEnum.YAN_CONTRACT_READY,
                        UserActionsEnum.SET_USER_ROLE,
                        UserActionsEnum.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS,
                        UserActionsEnum.EDIT,
                        UserActionsEnum.SET_EXCLUDED_DOMAINS,
                        UserActionsEnum.SET_EXCLUDED_PHONES,
                        UserActionsEnum.SET_BLOCKED,
                        UserActionsEnum.CHANGE_CONTRACT));
        restricted.put(19L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.PROVIDE_CONTACTS,
                        UserActionsEnum.REQUEST_CREATE_IN_BANNER_STORE,
                        UserActionsEnum.YAN_CONTRACT_READY,
                        UserActionsEnum.SET_USER_ROLE,
                        UserActionsEnum.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS,
                        UserActionsEnum.EDIT,
                        UserActionsEnum.SET_EXCLUDED_DOMAINS,
                        UserActionsEnum.SET_EXCLUDED_PHONES,
                        UserActionsEnum.SET_BLOCKED,
                        UserActionsEnum.CHANGE_CONTRACT));
        restricted.put(24L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.CREATED_PARTNER_IN_BANNER_STORE,
                        UserActionsEnum.REQUEST_YAN_CONTRACT,
                        UserActionsEnum.SET_USER_ROLE,
                        UserActionsEnum.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS,
                        UserActionsEnum.EDIT,
                        UserActionsEnum.SET_EXCLUDED_DOMAINS,
                        UserActionsEnum.SET_EXCLUDED_PHONES,
                        UserActionsEnum.SET_BLOCKED,
                        UserActionsEnum.CHANGE_CONTRACT));
        restricted.put(25L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.PROVIDE_CONTACTS,
                        UserActionsEnum.CREATED_PARTNER_IN_BANNER_STORE,
                        UserActionsEnum.REQUEST_YAN_CONTRACT,
                        UserActionsEnum.SET_USER_ROLE,
                        UserActionsEnum.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS,
                        UserActionsEnum.EDIT,
                        UserActionsEnum.SET_EXCLUDED_DOMAINS,
                        UserActionsEnum.SET_EXCLUDED_PHONES,
                        UserActionsEnum.SET_BLOCKED,
                        UserActionsEnum.CHANGE_CONTRACT
                ));
        restricted.put(26L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.REQUEST_CREATE_IN_BANNER_STORE,
                        UserActionsEnum.REQUEST_YAN_CONTRACT,
                        UserActionsEnum.SET_USER_ROLE,
                        UserActionsEnum.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS,
                        UserActionsEnum.EDIT,
                        UserActionsEnum.SET_EXCLUDED_DOMAINS,
                        UserActionsEnum.SET_EXCLUDED_PHONES,
                        UserActionsEnum.SET_BLOCKED,
                        UserActionsEnum.CHANGE_CONTRACT));
        restricted.put(27L,
                Set.of(UserActionsEnum.ADD,
                        UserActionsEnum.PROVIDE_CONTACTS,
                        UserActionsEnum.REQUEST_CREATE_IN_BANNER_STORE,
                        UserActionsEnum.REQUEST_YAN_CONTRACT,
                        UserActionsEnum.SET_USER_ROLE,
                        UserActionsEnum.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS,
                        UserActionsEnum.EDIT,
                        UserActionsEnum.SET_EXCLUDED_DOMAINS,
                        UserActionsEnum.SET_EXCLUDED_PHONES,
                        UserActionsEnum.SET_BLOCKED,
                        UserActionsEnum.CHANGE_CONTRACT
                ));
        ALLOWED_ACTIONS_BY_MULTUSTATE_VALUE.putAll(restricted.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> {
                    Set<String> actions = new HashSet<>(ALL_ACTIONS.keySet());
                    actions.removeAll(
                            entry.getValue().stream().map(ActionNameHolder::getActionName).toList()
                    );
                    return actions;
                })));
    }

    private static void fillAllActions() {
        ALL_ACTIONS.put(UserActionsEnum.ADD.getActionName(),
                TextTemplateMsg.ADD);
        ALL_ACTIONS.put(UserActionsEnum.PROVIDE_CONTACTS.getActionName(),
                UserActionMsg.PROVIDE_CONTACTS);
        ALL_ACTIONS.put(UserActionsEnum.REQUEST_CREATE_IN_BANNER_STORE.getActionName(),
                UserActionMsg.REQUEST_CREATE_IN_BANNER_STORE);
        ALL_ACTIONS.put(UserActionsEnum.CREATED_PARTNER_IN_BANNER_STORE.getActionName(),
                UserActionMsg.CREATED_PARTNER_IN_BANNER_STORE);
        ALL_ACTIONS.put(UserActionsEnum.SET_USER_ROLE.getActionName(),
                UserActionMsg.SET_USER_ROLE);
        ALL_ACTIONS.put(UserActionsEnum.REVOKE_ROLES.getActionName(),
                UserActionMsg.REVOKE_ROLES);
        ALL_ACTIONS.put(UserActionsEnum.REQUEST_YAN_CONTRACT.getActionName(),
                UserActionMsg.REQUEST_YAN_CONTRACT);
        ALL_ACTIONS.put(UserActionsEnum.YAN_CONTRACT_READY.getActionName(),
                UserActionMsg.YAN_CONTRACT_READY);
        ALL_ACTIONS.put(UserActionsEnum.LINK_ADFOX_USER.getActionName(),
                UserActionMsg.LINK_ADFOX_USER);
        ALL_ACTIONS.put(UserActionsEnum.UNLINK_ADFOX_USER.getActionName(),
                UserActionMsg.UNLINK_ADFOX_USER);
        ALL_ACTIONS.put(UserActionsEnum.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS.getActionName(),
                UserActionMsg.UNSUBSCRIBE_FROM_STAT_MONITORING_EMAILS);
        ALL_ACTIONS.put(UserActionsEnum.EDIT.getActionName(),
                TextTemplateMsg.EDIT);
        ALL_ACTIONS.put(UserActionsEnum.SET_EXCLUDED_DOMAINS.getActionName(),
                UserActionMsg.SET_EXCLUDED_DOMAINS);
        ALL_ACTIONS.put(UserActionsEnum.SET_EXCLUDED_PHONES.getActionName(),
                UserActionMsg.SET_EXCLUDED_PHONES);
        ALL_ACTIONS.put(UserActionsEnum.SET_BLOCKED.getActionName(),
                UserActionMsg.SET_BLOCKED);
        ALL_ACTIONS.put(UserActionsEnum.RESET_BLOCKED.getActionName(),
                UserActionMsg.RESET_BLOCKED);
        ALL_ACTIONS.put(UserActionsEnum.CHANGE_CONTRACT.getActionName(),
                UserActionMsg.EDIT_REQUISITES);
    }

}
