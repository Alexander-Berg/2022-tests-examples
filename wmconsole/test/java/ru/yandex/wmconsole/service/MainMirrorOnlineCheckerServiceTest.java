package ru.yandex.wmconsole.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.webmaster.common.host.dao.TblHostsMainDao;
import ru.yandex.wmconsole.data.info.BriefHostInfo;
import ru.yandex.wmconsole.data.mirror.MirrorGroupActionEnum;
import ru.yandex.wmconsole.data.mirror.MirrorGroupChangeRequest;
import ru.yandex.wmconsole.data.mirror.MirrorGroupChangeStateEnum;
import ru.yandex.wmconsole.data.partition.WMCPartition;
import ru.yandex.wmtools.common.error.InternalException;
import ru.yandex.wmtools.common.error.UserException;
import ru.yandex.wmtools.common.service.JdbcConnectionWrapperService;
import ru.yandex.wmtools.common.sita.*;
import ru.yandex.wmtools.common.util.ServiceTransactionCallback;
import ru.yandex.wmtools.common.util.ServiceTransactionTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import static org.easymock.EasyMock.*;

/**
 * User: azakharov
 * Date: 04.04.14
 * Time: 11:58
 */
public class MainMirrorOnlineCheckerServiceTest {

    private static final long USER_ID = 777;
    private static final long HOST_ID = 888;

    private static final String CURRENT_MAIN_MIRROR = "lenta.ru";
    private static final String CURRENT_NOT_MAIN_MIRROR = "www.lenta.ru";
    private static final String NEW_MAIN_MIRROR = CURRENT_NOT_MAIN_MIRROR;

    private MainMirrorOnlineCheckerService mainMirrorOnlineCheckerService;
    private MirrorGroupsChangeService mockMirrorGroupsChangeService;
    private SitaService mockSitaService;
    private TblHostsMainDao mockTblHostsMainDao;
    private JdbcConnectionWrapperService mockJdbcConnectionWrapperService;

    @Before
    public void setUp() {
        mainMirrorOnlineCheckerService = new MainMirrorOnlineCheckerService();

        mockMirrorGroupsChangeService = createMock(MirrorGroupsChangeService.class);
        mainMirrorOnlineCheckerService.setMirrorGroupsChangeService(mockMirrorGroupsChangeService);

        mockSitaService = createMock(SitaService.class);
        mainMirrorOnlineCheckerService.setNewSitaService(mockSitaService);
        mainMirrorOnlineCheckerService.setProdSitaService(mockSitaService);

        mockTblHostsMainDao = createMock(TblHostsMainDao.class);
        mainMirrorOnlineCheckerService.setTblHostsMainDao(mockTblHostsMainDao);

        mockJdbcConnectionWrapperService = createMock(JdbcConnectionWrapperService.class);
        mainMirrorOnlineCheckerService.setJdbcConnectionWrapperService(mockJdbcConnectionWrapperService);

        mainMirrorOnlineCheckerService.init();
    }

    @Test
    public void testRerankNewToChecked() throws InternalException, URISyntaxException, InterruptedException, UserException {

        MirrorGroupChangeRequest newRequest = new MirrorGroupChangeRequest(
                HOST_ID, USER_ID, new Date(), null, NEW_MAIN_MIRROR, MirrorGroupActionEnum.RERANGE, MirrorGroupChangeStateEnum.NEW, CURRENT_MAIN_MIRROR, null, null, null, null, null, MirrorGroupChangeRequest.DEFAULT_ATTEMPTS, false
        );

        ServiceTransactionTemplate mockTransactionTemplate = createMock(ServiceTransactionTemplate.class);
        expect(mockJdbcConnectionWrapperService.getServiceTransactionTemplate(anyObject(WMCPartition.class))).andReturn(
                mockTransactionTemplate
        ).times(2);
        expect(mockTransactionTemplate.executeInService(anyObject(ServiceTransactionCallback.class))).andReturn(newRequest).once();
        expect(mockTransactionTemplate.executeInService(anyObject(ServiceTransactionCallback.class))).andReturn(null).once();

        BriefHostInfo hostInfo = new BriefHostInfo(HOST_ID, CURRENT_MAIN_MIRROR, null);
        expect(mockTblHostsMainDao.getBriefHostInfoByHostId(HOST_ID)).andReturn(hostInfo);

        URI uri = new URI("http://" + CURRENT_MAIN_MIRROR);
        expect(mockSitaService.getMainMirrorCurrentBase(uri)).andReturn(uri);

        URI currentNotMainUri = new URI("http://" + CURRENT_NOT_MAIN_MIRROR);
        URI newMainUri = new URI("http://" + NEW_MAIN_MIRROR);
        SitaMirroringRequest sitaRequest = SitaMirroringRequest.createRerankRequest(uri, newMainUri);
        SitaMirroringResponse sitaResponse = new SitaMirroringResponse(SitaMirroringActionStatusEnum.OK,
                CollectionFactory.list(
                        new SitaMirroringResponse.THostResult(SitaMirroringHostStatusEnum.OK, currentNotMainUri, null),
                        new SitaMirroringResponse.THostResult(SitaMirroringHostStatusEnum.OK, uri, newMainUri)
                ));
        expect(mockSitaService.request(sitaRequest)).andReturn(sitaResponse);

        mockMirrorGroupsChangeService.updateState(new MirrorGroupChangeRequest(newRequest, MirrorGroupChangeStateEnum.CHECKED, newRequest.getAttempts()));
        expectLastCall().once();

        replay(mockJdbcConnectionWrapperService, mockTransactionTemplate, mockMirrorGroupsChangeService, mockTblHostsMainDao, mockSitaService);

        mainMirrorOnlineCheckerService.checkDatabase(8);

        verify(mockJdbcConnectionWrapperService, mockTransactionTemplate, mockMirrorGroupsChangeService, mockTblHostsMainDao, mockSitaService);
    }

    @Test
    public void testRerankNeedRecheckToChecked() throws InternalException, URISyntaxException, InterruptedException, UserException {

        MirrorGroupChangeRequest newRequest = new MirrorGroupChangeRequest(
                HOST_ID, USER_ID, new Date(), null, NEW_MAIN_MIRROR, MirrorGroupActionEnum.RERANGE, MirrorGroupChangeStateEnum.NEED_RECHECK, CURRENT_MAIN_MIRROR, null, null, null, null, null, MirrorGroupChangeRequest.DEFAULT_ATTEMPTS, false
        );

        ServiceTransactionTemplate mockTransactionTemplate = createMock(ServiceTransactionTemplate.class);
        expect(mockJdbcConnectionWrapperService.getServiceTransactionTemplate(anyObject(WMCPartition.class))).andReturn(
                mockTransactionTemplate
        ).times(2);
        expect(mockTransactionTemplate.executeInService(anyObject(ServiceTransactionCallback.class))).andReturn(newRequest).once();
        expect(mockTransactionTemplate.executeInService(anyObject(ServiceTransactionCallback.class))).andReturn(null).once();

        BriefHostInfo hostInfo = new BriefHostInfo(HOST_ID, CURRENT_MAIN_MIRROR, null);
        expect(mockTblHostsMainDao.getBriefHostInfoByHostId(HOST_ID)).andReturn(hostInfo);

        URI uri = new URI("http://" + CURRENT_MAIN_MIRROR);
        expect(mockSitaService.getMainMirrorCurrentBase(uri)).andReturn(uri);

        URI currentNotMainUri = new URI("http://" + CURRENT_NOT_MAIN_MIRROR);
        URI newMainUri = new URI("http://" + NEW_MAIN_MIRROR);
        SitaMirroringRequest sitaRequest = SitaMirroringRequest.createRerankRequest(uri, newMainUri);
        SitaMirroringResponse sitaResponse = new SitaMirroringResponse(SitaMirroringActionStatusEnum.OK,
                CollectionFactory.list(
                        new SitaMirroringResponse.THostResult(SitaMirroringHostStatusEnum.OK, currentNotMainUri, null),
                        new SitaMirroringResponse.THostResult(SitaMirroringHostStatusEnum.OK, uri, newMainUri)
                ));
        expect(mockSitaService.request(sitaRequest)).andReturn(sitaResponse);

        mockMirrorGroupsChangeService.updateState(new MirrorGroupChangeRequest(newRequest, MirrorGroupChangeStateEnum.CHECKED, newRequest.getAttempts()));
        expectLastCall().once();

        replay(mockJdbcConnectionWrapperService, mockTransactionTemplate, mockMirrorGroupsChangeService, mockTblHostsMainDao, mockSitaService);

        mainMirrorOnlineCheckerService.checkDatabase(8);

        verify(mockJdbcConnectionWrapperService, mockTransactionTemplate, mockMirrorGroupsChangeService, mockTblHostsMainDao, mockSitaService);
    }

    @Test
    public void testRerankNewToDeclined() throws InternalException, URISyntaxException, UserException {

        MirrorGroupChangeRequest newRequest = new MirrorGroupChangeRequest(
                HOST_ID, USER_ID, new Date(), null, NEW_MAIN_MIRROR, MirrorGroupActionEnum.RERANGE, MirrorGroupChangeStateEnum.NEW, CURRENT_MAIN_MIRROR, null, null, null, null, null, MirrorGroupChangeRequest.DEFAULT_ATTEMPTS, false
        );

        ServiceTransactionTemplate mockTransactionTemplate = createMock(ServiceTransactionTemplate.class);
        expect(mockJdbcConnectionWrapperService.getServiceTransactionTemplate(anyObject(WMCPartition.class))).andReturn(
                mockTransactionTemplate
        ).times(2);
        expect(mockTransactionTemplate.executeInService(anyObject(ServiceTransactionCallback.class))).andReturn(newRequest).once();
        expect(mockTransactionTemplate.executeInService(anyObject(ServiceTransactionCallback.class))).andReturn(null).once();

        BriefHostInfo hostInfo = new BriefHostInfo(HOST_ID, CURRENT_MAIN_MIRROR, null);
        expect(mockTblHostsMainDao.getBriefHostInfoByHostId(HOST_ID)).andReturn(hostInfo);

        URI uri = new URI("http://" + CURRENT_MAIN_MIRROR);
        expect(mockSitaService.getMainMirrorCurrentBase(uri)).andReturn(uri);

        URI currentNotMainUri = new URI("http://" + CURRENT_NOT_MAIN_MIRROR);
        URI newMainUri = new URI("http://" + NEW_MAIN_MIRROR);
        SitaMirroringRequest sitaRequest = SitaMirroringRequest.createRerankRequest(uri, newMainUri);
        SitaMirroringResponse sitaResponse = new SitaMirroringResponse(SitaMirroringActionStatusEnum.ERROR_TIMEOUT,
                CollectionFactory.list(
                        new SitaMirroringResponse.THostResult(SitaMirroringHostStatusEnum.ERROR_HOST_NOT_PROCESSED, currentNotMainUri, null),
                        new SitaMirroringResponse.THostResult(SitaMirroringHostStatusEnum.ERROR_HOST_NOT_PROCESSED, uri, null)
                ));
        expect(mockSitaService.request(sitaRequest)).andReturn(sitaResponse);

        mockMirrorGroupsChangeService.updateState(new MirrorGroupChangeRequest(newRequest, MirrorGroupChangeStateEnum.DECLINED, newRequest.getAttempts()));
        expectLastCall().once();

        replay(mockJdbcConnectionWrapperService, mockTransactionTemplate, mockMirrorGroupsChangeService, mockTblHostsMainDao, mockSitaService);

        mainMirrorOnlineCheckerService.checkDatabase(8);

        verify(mockJdbcConnectionWrapperService, mockTransactionTemplate, mockMirrorGroupsChangeService, mockTblHostsMainDao, mockSitaService);
    }

    @Test
    public void testRerankNeedRecheckToDeclined() throws InternalException, URISyntaxException, UserException {

        MirrorGroupChangeRequest newRequest = new MirrorGroupChangeRequest(
                HOST_ID, USER_ID, new Date(), null, NEW_MAIN_MIRROR, MirrorGroupActionEnum.RERANGE, MirrorGroupChangeStateEnum.NEED_RECHECK, CURRENT_MAIN_MIRROR, null, null, null, null, null, MirrorGroupChangeRequest.DEFAULT_ATTEMPTS, false);

        ServiceTransactionTemplate mockTransactionTemplate = createMock(ServiceTransactionTemplate.class);
        expect(mockJdbcConnectionWrapperService.getServiceTransactionTemplate(anyObject(WMCPartition.class))).andReturn(
                mockTransactionTemplate
        ).times(2);
        expect(mockTransactionTemplate.executeInService(anyObject(ServiceTransactionCallback.class))).andReturn(newRequest).once();
        expect(mockTransactionTemplate.executeInService(anyObject(ServiceTransactionCallback.class))).andReturn(null).once();

        BriefHostInfo hostInfo = new BriefHostInfo(HOST_ID, CURRENT_MAIN_MIRROR, null);
        expect(mockTblHostsMainDao.getBriefHostInfoByHostId(HOST_ID)).andReturn(hostInfo);

        URI uri = new URI("http://" + CURRENT_MAIN_MIRROR);
        expect(mockSitaService.getMainMirrorCurrentBase(uri)).andReturn(uri);

        URI currentNotMainUri = new URI("http://" + CURRENT_NOT_MAIN_MIRROR);
        URI newMainUri = new URI("http://" + NEW_MAIN_MIRROR);
        SitaMirroringRequest sitaRequest = SitaMirroringRequest.createRerankRequest(uri, newMainUri);
        SitaMirroringResponse sitaResponse = new SitaMirroringResponse(SitaMirroringActionStatusEnum.ERROR_TIMEOUT,
                CollectionFactory.list(
                        new SitaMirroringResponse.THostResult(SitaMirroringHostStatusEnum.ERROR_HOST_NOT_PROCESSED, currentNotMainUri, null),
                        new SitaMirroringResponse.THostResult(SitaMirroringHostStatusEnum.ERROR_HOST_NOT_PROCESSED, uri, null)
                ));
        expect(mockSitaService.request(sitaRequest)).andReturn(sitaResponse);

        mockMirrorGroupsChangeService.updateState(new MirrorGroupChangeRequest(newRequest, MirrorGroupChangeStateEnum.RECHECK_DECLINED, newRequest.getAttempts()));
        expectLastCall().once();

        replay(mockJdbcConnectionWrapperService, mockTransactionTemplate, mockMirrorGroupsChangeService, mockTblHostsMainDao, mockSitaService);

        mainMirrorOnlineCheckerService.checkDatabase(8);

        verify(mockJdbcConnectionWrapperService, mockTransactionTemplate, mockMirrorGroupsChangeService, mockTblHostsMainDao, mockSitaService);
    }

    @After
    public void tearDown() {
    }
}
