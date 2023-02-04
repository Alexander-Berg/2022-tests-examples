package ru.yandex.qe.dispenser.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.abc.AbcPerson;
import ru.yandex.qe.dispenser.domain.abc.AbcService;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceMember;
import ru.yandex.qe.dispenser.domain.abc.AbcServiceResponsible;
import ru.yandex.qe.dispenser.domain.dao.group.GroupDao;
import ru.yandex.qe.dispenser.domain.dao.project.ProjectDao;
import ru.yandex.qe.dispenser.domain.hierarchy.Hierarchy;
import ru.yandex.qe.dispenser.ws.abc.AbcApiHelper;
import ru.yandex.qe.dispenser.ws.abc.MembersRequestBuilder;
import ru.yandex.qe.dispenser.ws.abc.ProjectTreeSync;
import ru.yandex.qe.dispenser.ws.abc.UpdateProjectMembers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.qe.dispenser.domain.abc.AbcApi.MAX_PAGE_SIZE;

public class AbcIntegrationTest extends BaseExternalApiTest {

    @Inject
    private AbcApiHelper abcHelper;

    @Inject
    private ProjectDao projectDao;

    @Inject
    private GroupDao groupDao;

    @Inject
    private UpdateProjectMembers updateProjectMembers;

    @Inject
    private ProjectTreeSync projectTreeSync;

    @Test
    public void pagedResultCanBeFetched() {
        final Stream<AbcServiceResponsible> services = abcHelper.createResponsiblesRequestBuilder()
                .fields("id")
                .pageSize(200)
                .stream();

        assertTrue(services.count() > 200);
    }

    @Test
    public void pagedResultCanBeFetchedByCursor() {
        final Stream<AbcService> services = abcHelper.createServiceRequestBuilder()
                .fields("id")
                .pageSize(100)
                .stream();

        assertTrue(services.count() > 100);

        final Stream<AbcServiceMember> members = abcHelper.createMembersRequestBuilder()
                .fields("id")
                .pageSize(MAX_PAGE_SIZE)
                .stream();

        assertTrue(members.count() > MAX_PAGE_SIZE);
    }

    @Test
    public void pagedResultCanBeFetchedByCursorFromLargeAmountOfService() {

        final Set<Integer> serverIds = Sets.newHashSet(1, 2, 3, 4, 5, 7, 9, 10, 11, 12, 13, 14, 15, 16, 18, 19, 21, 22, 23, 24, 27, 30, 34, 35, 36, 43, 46, 48, 49, 51, 52, 58, 60, 63, 67, 70, 72, 75, 78, 79, 86, 88, 89, 92, 94, 96, 97, 98, 99, 101, 107, 108, 109, 114, 115, 116, 117, 120, 121, 127, 130, 133, 136, 137, 142, 143, 144, 145, 146, 147, 148, 149, 150, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 171, 174, 175, 176, 178, 181, 184, 185, 186, 187, 188, 189, 190, 192, 195, 198, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 226, 228, 230, 231, 232, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 283, 284, 286, 288, 289, 290, 292, 293, 295, 296, 297, 298, 299, 300, 301, 304, 305, 306, 307, 308, 309, 311, 319, 323, 324, 329, 330, 331, 332, 334, 335, 336, 337, 338, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 353, 354, 355, 356, 357, 359, 360, 361, 363, 365, 366, 368, 370, 375, 376, 377, 379, 381, 382, 384, 385, 386, 387, 388, 390, 393, 394, 395, 397, 398, 399, 400, 403, 405, 406, 408, 412, 418, 419, 421, 422, 424, 426, 427, 428, 429, 433, 435, 437, 438, 441, 442, 444, 446, 448, 449, 450, 451, 454, 455, 456, 457, 460, 465, 466, 467, 468, 470, 471, 472, 473, 476, 478, 479, 480, 481, 483, 484, 485, 487, 488, 492, 493, 494, 498, 499, 500, 502, 504, 505, 506, 508, 510, 511, 513, 521, 523, 524, 525, 526, 527, 529, 530, 531, 532, 534, 535, 536, 537, 541, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 556, 560, 561, 562, 563, 564, 565, 566, 567, 568, 571, 572, 574, 576, 578, 580, 581, 582, 583, 584, 585, 589, 590, 594, 597, 598, 600, 601, 603, 604, 606, 608, 610, 611, 612, 615, 616, 622, 626, 631, 635, 642, 645, 646, 648, 649, 650, 652, 661, 663, 665, 666, 670, 674, 675, 676, 679, 680, 684, 688, 690, 692, 694, 695, 696);

        //same logic as @see AbcApiHelper::collectPersonsByService
        final MembersRequestBuilder builder = abcHelper.createMembersRequestBuilder();
        for (final List<Integer> ids : Lists.partition(Lists.newArrayList(serverIds), 100)) {
            final Map<Integer, List<AbcServiceMember>> members = builder
                    .serviceId(ids)
                    .pageSize(10)
                    .fields("person.login", "service.id")
                    .stream().collect(Collectors.groupingBy(personHolder -> personHolder.getServiceReference().getId()));

            assertFalse(members.isEmpty());
        }
    }

    @Test
    public void abcServicesAncestorsCanBeFetched() {
        final List<AbcService> services = abcHelper.getServicesWithAncestors(Arrays.asList(1357, 989)).collect(Collectors.toList());

        assertEquals(2, services.size());

        assertTrue(services.stream()
                .allMatch(s -> s.getId() != null && s.getAncestors() != null));
    }

    @Test
    public void abcServicesDescendantsCanBeFetched() {
        final List<AbcService> descendants = abcHelper.getServicesByParentWithDescendants(851).collect(Collectors.toList());

        assertFalse(descendants.isEmpty());

        assertTrue(descendants.stream()
                .anyMatch(s -> Objects.equals(s.getId(), 3079))
        );
    }

    @Test
    public void abcServiceMembersCanBeFetched() {

        assertTrue(abcHelper.getServiceMembers(1357)
                .map(AbcPerson::getLogin)
                .anyMatch(login -> login.equals("vinnie-grapes")));

        assertTrue(abcHelper.getServiceResponsibles(1357)
                .map(AbcPerson::getLogin)
                .anyMatch(login -> login.equals("denblo")));

        assertTrue(abcHelper.getServiceMembers(1357, 25)
                .map(AbcPerson::getLogin)
                .anyMatch(login -> login.equals("den")));
    }

    @Test
    public void abcServiceQuotaManagerCanBeFetched() {
        assertTrue(abcHelper.createMembersRequestBuilder()
                .roleScope(121)
                .serviceId(1357)
                .fields("person.login")
                .stream()
                .anyMatch(p -> p.getPerson().getLogin().equals("ndolganov")));
    }


    @Test
    public void abcServiceProjectSynced() {
        projectTreeSync.perform(new UpdateProjectMembers.RoleChangesHolder());
        updateHierarchy();

        final Set<Project> projects = projectDao.getAll();

        assertTrue(projects.size() > 100);

        assertFalse(projects.stream().anyMatch(p -> p.getAbcServiceId() != null && p.getAbcServiceId().equals(842)),
                "Deleted services can't be synced"
        );

        final Project projectWithQuotaManager = projects.stream()
                .filter(p -> Objects.equals(2793, p.getAbcServiceId()))
                .findFirst()
                .get();

        final Set<Person> responsibles = Hierarchy.get().getProjectReader().getLinkedResponsibles(projectWithQuotaManager);

        final Set<String> responsibleLogins = responsibles.stream()
                .map(Person::getLogin)
                .collect(Collectors.toSet());

        assertTrue(responsibleLogins.containsAll(ImmutableSet.of("starlight")));
    }

    @Test
    public void abcServiceMembersWithRoles() {
        final int dispenserId = 1357;
        final Set<String> roleIds = Collections.singleton("1");

        final List<AbcPerson> collect = abcHelper.getServiceMembersWithRoles(dispenserId, roleIds).collect(Collectors.toList());
        assertEquals(1, collect.size());
        assertEquals("vinnie-grapes", collect.get(0).getLogin());
    }

    @Test
    public void abcServiceMembersByUser() {
        final Integer dispenserId = 1357;
        final Set<String> logins = Collections.singleton("vinnie-grapes");

        final List<AbcServiceMember> collect = abcHelper.getServiceMembersByUser(logins).collect(Collectors.toList());

        assertTrue(collect.stream()
                .anyMatch(e -> dispenserId.equals(e.getServiceReference() != null ? e.getServiceReference().getId() : null)));
    }
}
