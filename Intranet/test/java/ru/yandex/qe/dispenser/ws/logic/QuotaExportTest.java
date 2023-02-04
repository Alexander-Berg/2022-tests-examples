package ru.yandex.qe.dispenser.ws.logic;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.opencsv.CSVReader;
import com.opencsv.bean.BeanFieldSplit;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.QuotaSpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Segment;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.domain.dao.person.PersonDao;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.segment.SegmentDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.v2.QuotaExportService;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuotaExportTest extends BusinessLogicTestBase {

    @Autowired
    private PersonDao personDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;
    @Autowired
    private SegmentDao segmentDao;

    @Test
    public void csvExportResponseShouldHaveCorrectFormat() throws IOException {
        final Response response = createLocalClient().path("/v2/quotas/export/csv").get();
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.valueOf("text/csv;charset=utf-8"), response.getMediaType());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));

        final String entity = response.readEntity(String.class);
        final CSVReader reader = new CSVReader(new StringReader(entity));
        final List<String[]> rows = reader.readAll();

        assertArrayEquals(rows.get(0), QuotaExportService.QuotaCSVBean.getColumnOrder());

        final Set<String> serviceKeys = serviceDao.getAll().stream().map(Service::getKey).collect(Collectors.toSet());
        final Set<String> resourceKeys = resourceDao.getAll().stream().map(Resource::getPublicKey).collect(Collectors.toSet());
        final Set<String> quotaSpecKeys = quotaSpecDao.getAll().stream().map(QuotaSpec::getPublicKey).collect(Collectors.toSet());
        final Set<String> projectKeys = projectDao.getAll().stream().map(Project::getPublicKey).collect(Collectors.toSet());
        final Set<String> segmentKeys = segmentDao.getAll().stream().map(Segment::getPublicKey).collect(Collectors.toSet());
        final Set<String> logins = personDao.getAll().stream().map(Person::getLogin).collect(Collectors.toSet());

        rows.stream().skip(1).forEach(row -> {
            assertTrue(serviceKeys.contains(row[0]));
            assertTrue(resourceKeys.contains(row[1]));
            assertTrue(quotaSpecKeys.contains(row[2]));
            assertTrue(projectKeys.contains(row[4]));
            assertTrue(allSpaceSplitPartsMatches(row[3], segmentKeys::contains));
            assertTrue(allSpaceSplitPartsMatches(row[5], logins::contains));
            assertTrue(hasQuotaFormat(row[6]));
            assertTrue(hasQuotaFormat(row[7]));
        });
    }

    private static boolean allSpaceSplitPartsMatches(final String string, final Predicate<String> predicate) {
        return Arrays.stream(StringUtils.split(string, " ")).allMatch(predicate);
    }

    private static boolean hasQuotaFormat(final String string) {
        final String[] parts = StringUtils.split(string, " ");
        if (parts.length != 2) {
            return false;
        }
        final Set<String> unitAbbreviations = Arrays.stream(DiUnit.values()).map(DiUnit::getAbbreviation).collect(Collectors.toSet());
        return NumberUtils.isNumber(parts[0]) && unitAbbreviations.contains(parts[1]);
    }

    @Test
    public void csvExportShouldReturnCorrectData() {
        final Response response = createLocalClient().path("/v2/quotas/export/csv").get();

        final String entity = response.readEntity(String.class);
        final CsvToBean<QuotaExportService.QuotaCSVBean> csvToBean = new CsvToBeanBuilder<QuotaExportService.QuotaCSVBean>(new StringReader(entity))
                .withType(QuotaExportService.QuotaCSVBean.class)
                .build();
        final List<QuotaExportService.QuotaCSVBean> beans = csvToBean.parse();
        beans.forEach(beanPostprocessingConsumer());

        final DiQuotaGetResponse quotas = dispenser().quotas().get().perform();
        assertEquals(beans.size(), quotas.size());

        for (final DiQuota quota : quotas) {
            final QuotaExportService.QuotaCSVBean bean = beans.stream()
                    .filter(quotaMatchingPredicate(quota))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No csv record in response for quota " + quota));
            assertEquals(bean.getMax(), quota.getMax().humanize());
            assertEquals(bean.getActual(), quota.getActual().humanize());
            final Set<String> responsibles = bean.getProjectResponsibles().stream()
                    .map(Person::getLogin)
                    .collect(Collectors.toSet());
            assertEquals(responsibles, quota.getProject().getResponsibles().getPersons());
        }
    }

    private static Predicate<QuotaExportService.QuotaCSVBean> quotaMatchingPredicate(final DiQuota quota) {
        return bean -> bean.getProjectKey().equals(quota.getProject().getKey()) &&
                bean.getServiceKey().equals(quota.getSpecification().getResource().getService().getKey()) &&
                bean.getResourceKey().equals(quota.getSpecification().getResource().getKey()) &&
                bean.getQuotaSpecKey().equals(quota.getSpecification().getKey()) &&
                bean.getSegmentKeys().equals(quota.getSegmentKeys());
    }

    /**
     * When reading empty column value OpenCSV creates collection with one empty string instead of empty collection.
     * See {@link BeanFieldSplit#convert(java.lang.String)}. Didn't find out how to fix that, so additional postprocessing is required.
     */
    private static Consumer<QuotaExportService.QuotaCSVBean> beanPostprocessingConsumer() {
        return bean -> {
            bean.getSegmentKeys().removeIf(String::isEmpty);
            bean.getProjectResponsibles().removeIf(person -> person == null || person.getLogin().isEmpty());
        };
    }
}
