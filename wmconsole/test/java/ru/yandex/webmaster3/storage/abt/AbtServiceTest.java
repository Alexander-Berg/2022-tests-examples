package ru.yandex.webmaster3.storage.abt;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.abt.dao.AbtExperimentYDao;
import ru.yandex.webmaster3.storage.abt.dao.AbtHostExperimentYDao;
import ru.yandex.webmaster3.storage.abt.dao.AbtUserExperimentYDao;
import ru.yandex.webmaster3.storage.abt.hash.AbtHashExperimentYDao;
import ru.yandex.webmaster3.storage.abt.hash.HashExperimentRecord;
import ru.yandex.webmaster3.storage.abt.hash.HashField;
import ru.yandex.webmaster3.storage.abt.hash.HashFunction;
import ru.yandex.webmaster3.storage.abt.model.Experiment;
import ru.yandex.webmaster3.storage.abt.model.ExperimentInfo;
import ru.yandex.webmaster3.storage.abt.model.ExperimentScope;
import ru.yandex.webmaster3.storage.abt.model.SimpleExperiment;
import ru.yandex.webmaster3.storage.host.CommonDataState;
import ru.yandex.webmaster3.storage.host.CommonDataType;
import ru.yandex.webmaster3.storage.settings.SettingsService;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.webmaster3.core.data.WebmasterHostId.Schema.HTTP;
import static ru.yandex.webmaster3.core.data.WebmasterHostId.Schema.HTTPS;

/**
 * @author akhazhoyan 06/2018
 */
public class AbtServiceTest {
    private static final WebmasterHostId HOST_ID = IdUtils.urlToHostId("http://yandex.ru");
    private static final WebmasterHostId HOST_ID_2 = IdUtils.urlToHostId("http://www.yandex.com");
    private static final WebmasterHostId HOST_ID_3 = IdUtils.urlToHostId("https://yandex.com");
    private static final WebmasterHostId HOST_ID_2_DOMAIN = IdUtils.urlToHostId("http://yandex.com");
    private static final long USER_ID = 1;

    private SettingsService settingsService;
    private AbtService abtService;
    private ExperimentMapperService experimentMapperService;
    private AbtHashExperimentYDao abtHashExperimentYDao;
    private AbtHostExperimentYDao abtHostExperimentYDao;
    private AbtUserExperimentYDao abtUserExperimentYDao;
    private AbtExperimentYDao abtExperimentYDao;

    private Map<ExperimentScope, List<HashExperimentRecord>> hashExperimentRecords = Stream.of(
            new HashExperimentRecord(Experiment.TEST_USER_FIRST.getName(), "TEST_1", HashField.USER_ID, HashFunction.FNV64_MOD, 0.0, 0.5),
            new HashExperimentRecord(Experiment.TEST_USER_FIRST.getName(), "TEST_2", HashField.USER_ID, HashFunction.FNV64_MOD, 0.5, 1.0),
            new HashExperimentRecord(Experiment.TEST_USER_SECOND.getName(), "TEST_3", HashField.USER_ID, HashFunction.FNV64_MOD, 0.0, 0.0),
            new HashExperimentRecord(Experiment.TEST_USER_SECOND.getName(), "TEST_4", HashField.USER_ID, HashFunction.FNV64_MOD, 0.0, 1.0),
            new HashExperimentRecord(Experiment.TEST_HOST_FIRST.getName(), "HOST_1", HashField.DOMAIN_WITHOUT_WWW, HashFunction.FNV64_DIV, 0.0, 0.3),
            new HashExperimentRecord(Experiment.TEST_HOST_FIRST.getName(), "HOST_2", HashField.DOMAIN_WITHOUT_WWW, HashFunction.FNV64_DIV, 0.3, 0.7),
            new HashExperimentRecord(Experiment.TEST_HOST_FIRST.getName(), "HOST_3", HashField.DOMAIN_WITHOUT_WWW, HashFunction.FNV64_DIV, 0.7, 1.0)
    ).collect(Collectors.groupingBy(record -> Experiment.valueOf(record.getExperiment()).getScope()));

    @Before
    public void setUp() {
        experimentMapperService = mock(ExperimentMapperService.class);
        abtHashExperimentYDao = mock(AbtHashExperimentYDao.class);
        abtHostExperimentYDao = mock(AbtHostExperimentYDao.class);
        abtUserExperimentYDao = mock(AbtUserExperimentYDao.class);
        settingsService = mock(SettingsService.class);
        abtExperimentYDao = mock(AbtExperimentYDao.class);
        when(experimentMapperService.getExperiment(any())).thenAnswer(invocation -> {
            return Experiment.valueOf(invocation.getArgument(0));
        });
        abtService = new AbtService(experimentMapperService, abtHashExperimentYDao, abtHostExperimentYDao, abtUserExperimentYDao,
                () -> hashExperimentRecords, abtExperimentYDao, settingsService);
    }

    private List<ExperimentInfo> dataToExperimentInfos(Map<String, String> data) {
        return data.entrySet().stream()
                .map(entry -> new ExperimentInfo(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Test
    public void testListUserExperimentsSuccess1() {
        Map<String, String> data = Map.of(
                Experiment.TEST_USER_FIRST.getName(), "CONTROL",
                Experiment.TEST_USER_SECOND.getName(), "TEST");
        when(abtUserExperimentYDao.select(USER_ID)).thenReturn(dataToExperimentInfos(data));

        Map<String, String> experimentInfos = abtService.getUserExperiments(USER_ID);
        Assert.assertEquals(data, experimentInfos);
    }

    @Test
    public void testListUserExperimentsSuccess2() {
        Map<String, String> data = Map.of(
                Experiment.TEST_USER_FIRST.getName(), "CONTROL",
                Experiment.TEST_USER_SECOND.getName(), "TEST");
        when(abtUserExperimentYDao.select(USER_ID)).thenReturn(dataToExperimentInfos(data));
        when(experimentMapperService.getExperiment(Experiment.TEST_USER_FIRST.getName()))
                .thenReturn(new SimpleExperiment(Experiment.TEST_USER_FIRST.getName(), "TEST", Experiment.TEST_USER_FIRST.getScope(), false));
        when(experimentMapperService.getExperiment(Experiment.TEST_USER_SECOND.getName()))
                .thenReturn(new SimpleExperiment(Experiment.TEST_USER_SECOND.getName(), "TEST", Experiment.TEST_USER_SECOND.getScope(), true));

        Map<String, String> experimentInfos = abtService.getUserExperiments(USER_ID);
        Map<String, String> expected = Map.of(Experiment.TEST_USER_SECOND.getName(), "TEST");
        Assert.assertEquals(Experiment.TEST_USER_FIRST + " is banned", expected, experimentInfos);
    }

    @Test
    public void testListUserExperimentsSuccess4() {
        var hashExperimentRecordsSaved = hashExperimentRecords;
        hashExperimentRecords = Collections.emptyMap();

        DateTime lastImportDate = DateTime.parse("2019-08-05T20:27:05.232+03:00");
        when(settingsService.getSettingOrNull(CommonDataType.EXPERIMENTS_LAST_IMPORT_DATE)).thenReturn(
                new CommonDataState(CommonDataType.EXPERIMENTS_LAST_IMPORT_DATE, lastImportDate.toString(), lastImportDate));
        DateTime prevImportDate = lastImportDate.minusHours(1);
        List<ExperimentInfo> experimentInfos = List.of(
                new ExperimentInfo(Experiment.TEST_USER_FIRST.getName(), "TEST", prevImportDate),
                new ExperimentInfo(Experiment.TEST_USER_SECOND.getName(), "TEST", lastImportDate)
        );

        when(abtUserExperimentYDao.select(USER_ID)).thenReturn(experimentInfos);
        Map<String, String> returnedInfos = abtService.getUserExperiments(USER_ID);
        Map<String, String> expected = Map.of(Experiment.TEST_USER_SECOND.getName(), "TEST");
        Assert.assertEquals(expected, returnedInfos);

        hashExperimentRecords = hashExperimentRecordsSaved;
    }

    @Test
    public void testListHostExperimentsSuccess1() {
        Map<String, String> data = Map.of(
                Experiment.TEST_HOST_FIRST.getName(), "CONTROL",
                Experiment.TEST_HOST_SECOND.getName(), "TEST");
        when(abtHostExperimentYDao.getHostExperiments(HOST_ID)).thenReturn(dataToExperimentInfos(data));

        Map<String, String> experimentInfos = abtService.getHostExperiments(HOST_ID);
        Assert.assertEquals(data, experimentInfos);
    }

    @Test
    public void testListHostExperimentsSuccess3() {
        var hashExperimentRecordsSaved = hashExperimentRecords;
        hashExperimentRecords = Collections.emptyMap();

        DateTime lastImportDate = DateTime.parse("2019-08-05T20:27:05.232+03:00");
        when(settingsService.getSettingOrNull(CommonDataType.EXPERIMENTS_LAST_IMPORT_DATE)).thenReturn(
                new CommonDataState(CommonDataType.EXPERIMENTS_LAST_IMPORT_DATE, lastImportDate.toString(), lastImportDate));
        DateTime prevImportDate = lastImportDate.minusHours(1);
        List<ExperimentInfo> experimentInfos = List.of(
                new ExperimentInfo(Experiment.TEST_USER_FIRST.getName(), "TEST", prevImportDate),
                new ExperimentInfo(Experiment.TEST_USER_SECOND.getName(), "TEST", lastImportDate)
        );

        when(abtHostExperimentYDao.getHostExperiments(HOST_ID)).thenReturn(experimentInfos);
        Map<String, String> returnedInfos = abtService.getHostExperiments(HOST_ID);
        Map<String, String> expected = Map.of(Experiment.TEST_USER_SECOND.getName(), "TEST");
        Assert.assertEquals(expected, returnedInfos);

        hashExperimentRecords = hashExperimentRecordsSaved;
    }

    @Test
    public void testListHostExperimentsSuccess4() {
        Map<String, String> data1 = Map.of(
                Experiment.TEST_HOST_FIRST.getName(), "CONTROL",
                Experiment.TEST_HOST_SECOND.getName(), "TEST");
        when(abtHostExperimentYDao.getHostExperiments(HOST_ID_2)).thenReturn(dataToExperimentInfos(data1));

        Map<String, String> data2 = Map.of(
                Experiment.TEST_DOMAIN_FIRST.getName(), "CONTROL",
                Experiment.TEST_HOST_SECOND.getName(), "TEST");
        when(abtHostExperimentYDao.getHostExperiments(HOST_ID_2_DOMAIN)).thenReturn(dataToExperimentInfos(data2));

        Map<String, String> data2_3 = Map.of(
                Experiment.TEST_DOMAIN_FIRST.getName(), "CONTROL",
                Experiment.TEST_HOST_FIRST.getName(), "CONTROL",
                Experiment.TEST_HOST_SECOND.getName(), "TEST");

        Map<String, String> experimentInfos = abtService.getHostExperiments(HOST_ID_2);
        Assert.assertEquals(data2_3, experimentInfos);
    }

    @Test
    // Два хоста, без доменного эксперимента
    public void testListHostsExperimentsSuccess1() {
        List<WebmasterHostId> hostsIds = List.of(HOST_ID, HOST_ID_2);
        List<WebmasterHostId> allHostIds = List.of(HOST_ID_2, HOST_ID_2_DOMAIN, HOST_ID);

        Map<String, String> data1 = Map.of(
                Experiment.TEST_HOST_FIRST.getName(), "CONTROL",
                Experiment.TEST_HOST_SECOND.getName(), "TEST");
        Map<String, String> data2 = Map.of(
                Experiment.TEST_HOST_FIRST.getName(), "TEST",
                Experiment.TEST_HOST_SECOND.getName(), "CONTROL");
        when(abtHostExperimentYDao.getHostsExperiments(argThat(t -> t.containsAll(allHostIds))))
                .thenReturn(Map.of(HOST_ID, dataToExperimentInfos(data1), HOST_ID_2, dataToExperimentInfos(data2)));

        var experimentsInfos = abtService.getHostsExperiments(hostsIds);

        var experimentInfos = experimentsInfos.get(HOST_ID);
        Assert.assertNotNull(experimentInfos);
        Assert.assertEquals(data1, experimentInfos);

        experimentInfos = experimentsInfos.get(HOST_ID_2);
        Assert.assertNotNull(experimentInfos);
        Assert.assertEquals(data2, experimentInfos);
    }

    @Test
    // Два хоста, с доменным экспериментом
    public void testListHostsExperimentsSuccess2() {
        List<WebmasterHostId> hostsIds = List.of(HOST_ID, HOST_ID_2);
        List<WebmasterHostId> allHostIds = List.of(HOST_ID_2, HOST_ID_2_DOMAIN, HOST_ID); // тут важен порядок

        Map<String, String> data1 = Map.of(
                Experiment.TEST_HOST_FIRST.getName(), "CONTROL",
                Experiment.TEST_HOST_SECOND.getName(), "TEST");
        Map<String, String> data2 = Map.of(
                Experiment.TEST_HOST_FIRST.getName(), "TEST",
                Experiment.TEST_HOST_SECOND.getName(), "CONTROL");
        Map<String, String> data3 = Map.of(
                Experiment.TEST_DOMAIN_FIRST.getName(), "TEST");
        when(abtHostExperimentYDao.getHostsExperiments(argThat(t -> t.containsAll(allHostIds))))
                .thenReturn(Map.of(
                        HOST_ID, dataToExperimentInfos(data1),
                        HOST_ID_2, dataToExperimentInfos(data2),
                        HOST_ID_2_DOMAIN, dataToExperimentInfos(data3))
                );

        var experimentsInfos = abtService.getHostsExperiments(hostsIds);

        var experimentInfos = experimentsInfos.get(HOST_ID);
        Assert.assertNotNull(experimentInfos);
        Assert.assertEquals(data1, experimentInfos);

        experimentInfos = experimentsInfos.get(HOST_ID_2);
        Assert.assertNotNull(experimentInfos);
        var data2_3 = new HashMap<>(data2);
        data2_3.putAll(data3);
        Assert.assertEquals(data2_3, experimentInfos);
    }

    @Test
    public void testUserHashExperiment() {
        Map<String, String> experiments = abtService.getUserExperiments(1L);
        Assert.assertEquals("User 1 have TEST_1 experiment", "TEST_2", experiments.get(Experiment.TEST_USER_FIRST.getName()));
        Assert.assertEquals("User 1 have TEST_4 experiment", "TEST_4", experiments.get(Experiment.TEST_USER_SECOND.getName()));

        experiments = abtService.getUserExperiments(15L);
        Assert.assertEquals("User 2 have TEST_2 experiment", "TEST_1", experiments.get(Experiment.TEST_USER_FIRST.getName()));
        Assert.assertEquals("User 2 have TEST_4 experiment", "TEST_4", experiments.get(Experiment.TEST_USER_SECOND.getName()));
    }

    @Test
    public void testHostHashExperiment() {
        Map<String, String> experiments = abtService.getHostExperiments(new WebmasterHostId(HTTP, "drive.ru", 80));
        Assert.assertEquals("All drive.ru hosts have HOST_3 group", "HOST_3", experiments.get(Experiment.TEST_HOST_FIRST.getName()));
        experiments = abtService.getHostExperiments(new WebmasterHostId(HTTP, "www.drive.ru", 80));
        Assert.assertEquals("All drive.ru hosts have HOST_3 group", "HOST_3", experiments.get(Experiment.TEST_HOST_FIRST.getName()));
        experiments = abtService.getHostExperiments(new WebmasterHostId(HTTPS, "www.drive.ru", 443));
        Assert.assertEquals("All drive.ru hosts have HOST_3 group", "HOST_3", experiments.get(Experiment.TEST_HOST_FIRST.getName()));
    }

    @Test
    public void testGetHostExperimentDuplicationDomainName() {
        final Map<WebmasterHostId, Map<String, String>> hostsExperiments = abtService.getHostsExperiments(List.of(HOST_ID, HOST_ID_2, HOST_ID_3));
        Assert.assertEquals("Count of hosts with experiment", 3, hostsExperiments.size());
        Assert.assertEquals("http://yandex.ru have Experimtn TEST_HOST_FIRST=HOST1", "HOST_1", hostsExperiments.get(HOST_ID).get(Experiment.TEST_HOST_FIRST.getName()));
        Assert.assertEquals("http://www.yandex.ru have Experimtn TEST_HOST_FIRST=HOST3", "HOST_3", hostsExperiments.get(HOST_ID_2).get(Experiment.TEST_HOST_FIRST.getName()));
        Assert.assertEquals("https://yandex.ru have Experimtn TEST_HOST_FIRST=HOST3", "HOST_3", hostsExperiments.get(HOST_ID_3).get(Experiment.TEST_HOST_FIRST.getName()));
    }
}
