package ru.yandex.qe.dispenser.standalone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import ru.yandex.qe.dispenser.domain.staff.StaffApi;
import ru.yandex.qe.dispenser.domain.staff.StaffGroup;
import ru.yandex.qe.dispenser.domain.staff.StaffPage;
import ru.yandex.qe.dispenser.domain.staff.StaffPerson;

public class MockStaff implements StaffApi {

    private List<StaffGroup> groups = new ArrayList<>();
    private List<StaffPerson> persons = new ArrayList<>();

    @Override
    public StaffPage<StaffGroup> getGroups(final String fields, final String sort, final int limit, final String query) {
        Stream<StaffGroup> filteredGroupsStream = groups.stream();
        if (StringUtils.defaultString(query).equals("is_deleted == True")) {
            filteredGroupsStream = filteredGroupsStream.filter(StaffGroup::isDeleted);
        } else if (StringUtils.defaultString(query).startsWith("id > ")) {
            final long id = Long.parseLong(query.substring(5));
            filteredGroupsStream = filteredGroupsStream.filter(g -> g.getId() > id);
        }
        final List<StaffGroup> filteredGroups = filteredGroupsStream.collect(Collectors.toList());
        final List<StaffGroup> result = filteredGroups.stream().sorted(Comparator.comparing(StaffGroup::getId)).limit(limit).collect(Collectors.toList());
        return new StaffPage<>(result, filteredGroups.size(), filteredGroups.size() / limit);
    }

    @Override
    public StaffPage<StaffGroup> getGroups(final String fields, final String sort, final int limit, final String query, final long page) {
        Stream<StaffGroup> filteredGroupsStream = groups.stream();
        if (StringUtils.defaultString(query).equals("is_deleted == True")) {
            filteredGroupsStream = filteredGroupsStream.filter(StaffGroup::isDeleted);
        } else if (StringUtils.defaultString(query).startsWith("id > ")) {
            final long id = Long.parseLong(query.substring(5));
            filteredGroupsStream = filteredGroupsStream.filter(g -> g.getId() > id);
        }
        final List<StaffGroup> filteredGroups = filteredGroupsStream.collect(Collectors.toList());
        final long skip = (page - 1) * limit;
        final List<StaffGroup> result = filteredGroups.stream().sorted(Comparator.comparing(StaffGroup::getId)).skip(skip).limit(limit).collect(Collectors.toList());
        return new StaffPage<>(result, filteredGroups.size(), filteredGroups.size() / limit);
    }

    @Override
    public StaffPage<StaffGroup> getGroups(final String fields, final String sort, final int limit) {
        final Stream<StaffGroup> filteredGroups = groups.stream();
        final List<StaffGroup> result = filteredGroups.sorted(Comparator.comparing(StaffGroup::getId)).limit(limit).collect(Collectors.toList());
        return new StaffPage<>(result, groups.size(), groups.size() / limit);
    }

    @Override
    public StaffPage<StaffGroup> getGroups(final String fields, final String sort, final int limit, final long page) {
        final Stream<StaffGroup> filteredGroups = groups.stream();
        final long skip = (page - 1) * limit;
        final List<StaffGroup> result = filteredGroups.sorted(Comparator.comparing(StaffGroup::getId)).skip(skip).limit(limit).collect(Collectors.toList());
        return new StaffPage<>(result, groups.size(), groups.size() / limit);
    }

    @Override
    public StaffPage<StaffPerson> getPersons(final String fields, final String sort, final int limit, final String query) {
        Stream<StaffPerson> filteredPersonsStream = persons.stream();
        if (StringUtils.defaultString(query).equals("is_deleted == True")) {
            filteredPersonsStream = filteredPersonsStream.filter(StaffPerson::isDeleted);
        } else if (StringUtils.defaultString(query).startsWith("id > ")) {
            final long id = Long.parseLong(query.substring(5));
            filteredPersonsStream = filteredPersonsStream.filter(g -> g.getId() > id);
        }
        final List<StaffPerson> filteredPersons = filteredPersonsStream.collect(Collectors.toList());
        final List<StaffPerson> result = filteredPersons.stream().sorted(Comparator.comparing(StaffPerson::getId)).limit(limit).collect(Collectors.toList());
        return new StaffPage<>(result, filteredPersons.size(), filteredPersons.size() / limit);
    }

    @Override
    public StaffPage<StaffPerson> getPersons(final String fields, final String sort, final int limit) {
        final Stream<StaffPerson> filteredPersons = persons.stream();
        final List<StaffPerson> result = filteredPersons.sorted(Comparator.comparing(StaffPerson::getId)).limit(limit).collect(Collectors.toList());
        return new StaffPage<>(result, persons.size(), persons.size() / limit);
    }

    @Override
    public StaffPage<StaffPerson> getPersons(final String fields, final String sort, final int limit, final String query, final long page) {
        Stream<StaffPerson> filteredPersonsStream = persons.stream();
        if (StringUtils.defaultString(query).equals("is_deleted == True")) {
            filteredPersonsStream = filteredPersonsStream.filter(StaffPerson::isDeleted);
        } else if (StringUtils.defaultString(query).startsWith("id > ")) {
            final long id = Long.parseLong(query.substring(5));
            filteredPersonsStream = filteredPersonsStream.filter(g -> g.getId() > id);
        }
        final List<StaffPerson> filteredPersons = filteredPersonsStream.collect(Collectors.toList());
        final long skip = (page - 1) * limit;
        final List<StaffPerson> result = filteredPersons.stream().sorted(Comparator.comparing(StaffPerson::getId)).skip(skip).limit(limit).collect(Collectors.toList());
        return new StaffPage<>(result, filteredPersons.size(), filteredPersons.size() / limit);
    }

    @Override
    public StaffPage<StaffPerson> getPersons(final String fields, final String sort, final int limit, final long page) {
        final Stream<StaffPerson> filteredPersons = persons.stream();
        final long skip = (page - 1) * limit;
        final List<StaffPerson> result = filteredPersons.sorted(Comparator.comparing(StaffPerson::getId)).skip(skip).limit(limit).collect(Collectors.toList());
        return new StaffPage<>(result, persons.size(), persons.size() / limit);
    }

    public void setGroups(final List<StaffGroup> groups) {
        this.groups = groups;
    }

    public void setPersons(final List<StaffPerson> persons) {
        this.persons = persons;
    }

}
