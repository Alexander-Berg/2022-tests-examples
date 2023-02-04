package ru.yandex.wmconsole.servantlet.support;

import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.wmconsole.servantlet.poll.AddPollServantlet;
import ru.yandex.wmconsole.servantlet.poll.DeletePollServantlet;
import ru.yandex.wmconsole.servantlet.poll.EditPollServantlet;
import ru.yandex.wmconsole.service.HostInfoService;
import ru.yandex.wmconsole.service.PollService;
import ru.yandex.wmconsole.service.UsersHostsService;
import ru.yandex.wmconsole.service.WMCUserInfoService;
import ru.yandex.wmtools.common.error.InternalException;
import ru.yandex.wmtools.common.error.UserException;
import ru.yandex.wmtools.common.service.UserService;

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.verify;

/**
 * User: azakharov
 * Date: 27.03.12
 * Time: 13:46
 */
public class EditPollServantletTest {
    private static final String PARAM_URL = "url";

    private ServRequest mockReq;
    private ServResponse mockRes;
    private PollService mockPollService;
    private HostInfoService mockHostInfoService;
    private WMCUserInfoService mockWMCUserInfoService;
    private UsersHostsService mockUsersHostsService;
    private UserService mockUserService;

    private AddPollServantlet  addPollServantlet;
    private EditPollServantlet editPollServantlet;
    private DeletePollServantlet deletePollServantlet;
    
    private IMocksControl control;

    @Before
    public void setUp() {
        control = createControl();

        mockReq = control.createMock(ServRequest.class);
        mockRes = control.createMock(ServResponse.class);
        mockPollService = control.createMock(PollService.class);
        mockHostInfoService = control.createMock(HostInfoService.class);
        mockWMCUserInfoService = control.createMock(WMCUserInfoService.class);
        mockUsersHostsService = control.createMock(UsersHostsService.class);

        editPollServantlet = new EditPollServantlet();
        editPollServantlet.setPollService(mockPollService);
        editPollServantlet.setHostInfoService(mockHostInfoService);
        editPollServantlet.setUserInfoService(mockWMCUserInfoService);
        editPollServantlet.setUsersHostsService(mockUsersHostsService);
    }

    @Ignore
    @Test
    public void testEditPoll() throws UserException, InternalException {
        expect(mockWMCUserInfoService.getPassportUserInfo((long) 1)).andReturn(null);
        long userId = 1;
        control.replay();
        //editPollServantlet.doProcess(mockReq, mockRes, userId);
        //verify(mockWMCUserInfoService);
    }
}
