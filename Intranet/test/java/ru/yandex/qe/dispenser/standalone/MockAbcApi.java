package ru.yandex.qe.dispenser.standalone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import ru.yandex.qe.dispenser.domain.abc.AbcApi;
import ru.yandex.qe.dispenser.domain.abc.AbcRole;
import ru.yandex.qe.dispenser.domain.abc.AbcService;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceGradient;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceMember;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceReference;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceResponsible;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceState;
import ru.yandex.qe.dispenser.domain.abc.Page;
import ru.yandex.qe.dispenser.domain.abc.TranslatedString;

import static ru.yandex.qe.dispenser.ws.AcceptanceTestBase.TEST_ABC_SERVICE_ID;

@ParametersAreNonnullByDefault
public class MockAbcApi implements AbcApi {

    @Nonnull
    private List<AbcService> services = new ArrayList<>();
    @Nonnull
    private List<AbcServiceResponsible> responsibles = new ArrayList<>();
    @Nonnull
    private List<AbcServiceMember> members = new ArrayList<>();
    @Nonnull
    private List<AbcRole> roles = new ArrayList<>();

    public static final TranslatedString EMPTY_TRANSLATE_STRING = new TranslatedString("", "");

    private static final List<AbcService> DEFAULT_ABC_SERVICES = ImmutableList.of(
            new AbcService(TEST_ABC_SERVICE_ID, Collections.emptyList(), null, "123", EMPTY_TRANSLATE_STRING, EMPTY_TRANSLATE_STRING, AbcServiceState.DEVELOP),
            new AbcService(1234, Collections.emptyList(), null, "1234", EMPTY_TRANSLATE_STRING, EMPTY_TRANSLATE_STRING, AbcServiceState.DEVELOP)
    );

    private final List<AbcServiceGradient> gradients = new ArrayList<>();

    public MockAbcApi() {
        this.services.addAll(DEFAULT_ABC_SERVICES);
    }

    @Override
    public Page<AbcService> getServices(@Nullable final String ids,
                                        @Nullable final Integer smallestAcceptableServiceId,
                                        @Nullable final String cursor,
                                        @Nullable final String fields,
                                        @Nullable final Integer page,
                                        @Nullable final Integer pageSize,
                                        @Nullable final Integer parentIdWithDescendants,
                                        @Nullable final String statesString) {
        Stream<AbcService> serviceStream = services.stream();

        final int smallestId = smallestAcceptableServiceId == null ? -1 : smallestAcceptableServiceId;
        serviceStream = serviceStream
                .filter(s -> s.getId() > smallestId)
                .sorted(Comparator.comparing(AbcServiceReference::getId));

        if (StringUtils.isNotEmpty(ids)) {
            final Set<Integer> idList = getIntegers(ids);

            serviceStream = serviceStream.filter(s -> idList.contains(s.getId()));
        } else if (parentIdWithDescendants != null) {
            serviceStream = serviceStream.filter(s -> s.getAncestors().stream().anyMatch(ar -> parentIdWithDescendants.equals(ar.getId())));
        }
        if (statesString != null) {
            final Set<AbcServiceState> states = Arrays.stream(statesString.split(","))
                    .map(AbcServiceState::fromKey)
                    .collect(Collectors.toSet());

            serviceStream = serviceStream.filter(s -> s.getState() != null && states.contains(s.getState()));
        }

        final List<AbcService> filteredServices = serviceStream.collect(Collectors.toList());

        return getPage(pageSize, page, filteredServices);
    }

    @NotNull
    private <T> Page<T> getPage(@Nullable final Integer pageSize,
                                @Nullable final Integer page,
                                final List<T> entities) {
        final int realPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        final int realPage = page == null ? 0 : page;

        final boolean hasNextService = entities.size() > (realPage == 0 ? 1 : realPage) * realPageSize;

        final List<T> pageEntities = entities.stream()
                .skip(Math.max((realPage - 1) * realPageSize, 0))
                .limit(realPageSize).collect(Collectors.toList());

        return new Page<>(pageEntities, hasNextService ? "next-url" : null);
    }

    @NotNull
    private Set<Integer> getIntegers(@NotNull final String ids) {
        return Arrays.stream(ids.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    @Override
    public Page<AbcServiceResponsible> getServiceResponsibles(@Nullable final String serviceIds,
                                                              @Nullable final String fields,
                                                              @Nullable final Integer page,
                                                              @Nullable final Integer pageSize) {

        Stream<AbcServiceResponsible> stream = responsibles.stream();

        if (StringUtils.isNotEmpty(serviceIds)) {
            final Set<Integer> ids = getIntegers(serviceIds);
            stream = stream.filter(e -> ids.contains(e.getServiceReference().getId()));
        }

        return getPage(pageSize, page, stream.collect(Collectors.toList()));
    }

    @Override
    public Page<AbcServiceMember> getServiceMembers(@Nullable final Integer smallestAcceptableServiceId,
                                                    @Nullable final String serviceIds,
                                                    @Nullable final String cursor,
                                                    @Nullable final String roleIds,
                                                    @Nullable final String roleScopeIds,
                                                    @Nullable final String fields,
                                                    @Nullable final Integer page,
                                                    @Nullable final Integer pageSize) {
        Stream<AbcServiceMember> stream = members.stream();

        final int smallestId = smallestAcceptableServiceId == null ? -1 : smallestAcceptableServiceId;
        stream = stream
                .filter(s -> s.getId() > smallestId)
                .sorted(Comparator.comparing(AbcServiceMember::getId));

        if (StringUtils.isNotEmpty(serviceIds)) {
            final Set<Integer> ids = getIntegers(serviceIds);
            stream = stream.filter(e -> ids.contains(e.getServiceReference().getId()));
        }

        if (StringUtils.isNotEmpty(roleIds)) {
            final Set<Integer> ids = getIntegers(roleIds);
            stream = stream.filter(e -> ids.contains(e.getRole().getId()));
        }

        if (StringUtils.isNotEmpty(roleScopeIds)) {
            stream = stream.filter(e -> false);
        }

        return getPage(pageSize, page, stream.collect(Collectors.toList()));
    }

    @Override
    public Page<AbcRole> getRoles(@Nullable final String serviceIdsString, @Nullable final String idsString,
                                  @Nullable final String fields,
                                  @Nullable final Integer page,
                                  @Nullable final Integer pageSize) {
        Stream<AbcRole> stream = roles.stream();
        if (StringUtils.isNotEmpty(serviceIdsString)) {
            final Set<Integer> serviceIds = getIntegers(serviceIdsString);
            stream = stream.filter(e -> serviceIds.contains(e.getServiceReference().getId()));
        }
        if (StringUtils.isNotEmpty(idsString)) {
            final Set<Integer> ids = getIntegers(idsString);
            stream = stream.filter(e -> ids.contains(e.getId()));
        }

        return getPage(pageSize, page, stream.collect(Collectors.toList()));
    }

    @Override
    public Page<AbcServiceMember> getServiceMembersByUsers(@Nullable final String logins, @Nullable final String states, @Nullable final Long role,
                                                           @Nullable final String fields, @Nullable final Integer page, @Nullable final Integer pageSize) {
        final Set<String> loginsSet = new HashSet<>(Arrays.asList(StringUtils.split(StringUtils.defaultString(logins), ',')));
        final Set<AbcServiceState> statesSet = Stream.of(StringUtils.split(StringUtils.defaultString(states), ','))
                .map(AbcServiceState::fromKey).collect(Collectors.toSet());
        final Map<Integer, AbcServiceState> currentStates = services.stream().collect(Collectors.toMap(AbcServiceReference::getId, AbcService::getState));
        return getPage(pageSize, page, members.stream()
                .filter(m -> loginsSet.contains(m.getPerson().getLogin())
                        && (role == null || (m.getRole() != null && m.getRole().getId() != null && Objects.equals(m.getRole().getId().longValue(), role)))
                        && statesSet.contains(currentStates.getOrDefault(m.getServiceReference().getId(), AbcServiceState.DEVELOP))).collect(Collectors.toList()));
    }

    @Override
    public Page<AbcServiceGradient> getServicesGradient(@Nullable final String cursor) {
        return new Page<>(gradients,null);
    }

    @TestOnly
    public AbcService addService(final AbcService service) {
        services.add(service);
        return service;
    }

    @TestOnly
    public AbcService addService(final Integer id, final List<AbcServiceReference> ancestors) {
        final AbcService service = new AbcService(id, ancestors, null, null, null, null, null);
        services.add(service);
        return service;
    }

    @TestOnly
    public AbcServiceMember addMember(final AbcServiceMember member) {
        members.add(member);
        return member;
    }
    @TestOnly
    public AbcServiceMember removeMember(final AbcServiceMember member) {
        members.remove(member);
        return member;
    }

    @TestOnly
    public AbcServiceResponsible addResponsible(final AbcServiceResponsible responsible) {
        responsibles.add(responsible);
        return responsible;
    }

    @TestOnly
    public AbcServiceResponsible removeResponsible(final AbcServiceResponsible responsible) {
        responsibles.remove(responsible);
        return responsible;
    }

    @TestOnly
    public AbcRole addRole(final AbcRole role) {
        roles.add(role);
        return role;
    }

    @TestOnly
    public void setGradients(final Collection<AbcServiceGradient> gradients) {
        this.gradients.clear();
        this.gradients.addAll(gradients);
    }

    @TestOnly
    public void reset() {
        clear();
        services.addAll(DEFAULT_ABC_SERVICES);
    }

    @TestOnly
    public void clear() {
        roles.clear();
        members.clear();
        responsibles.clear();
        services.clear();
    }
}
