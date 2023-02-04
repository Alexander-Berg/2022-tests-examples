package ru.yandex.webmaster3.viewer.http.sitetree;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.storage.util.ydb.exception.WebmasterYdbException;
import ru.yandex.webmaster3.storage.user.dao.UserSiteTreeYDao;

/**
 * User: azakharov
 * Date: 03.09.14
 * Time: 15:30
 */
public class DeleteSiteTreeNodeActionTest {
    @Test
    public void testDeleteSite()  {
        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);
        DeleteSiteTreeNodeAction action = new DeleteSiteTreeNodeAction(mockUserSiteTreeYDao);

        long nodeId = 777l;

        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        mockUserSiteTreeYDao.deleteUserNode(hostId, nodeId);
        EasyMock.expectLastCall();

        EasyMock.replay(mockUserSiteTreeYDao);

        DeleteSiteTreeNodeRequest request = new DeleteSiteTreeNodeRequest();
        request.setNodeId(nodeId);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);

        DeleteSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof DeleteSiteTreeNodeResponse.OrdinaryResponse);

        EasyMock.verify(mockUserSiteTreeYDao);
    }
}
