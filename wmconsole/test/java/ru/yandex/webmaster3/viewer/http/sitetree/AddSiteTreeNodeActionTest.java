package ru.yandex.webmaster3.viewer.http.sitetree;

import java.util.Collections;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.sitestructure.SiteTreeNode;
import ru.yandex.webmaster3.storage.util.ydb.exception.WebmasterYdbException;
import ru.yandex.webmaster3.storage.user.dao.UserSiteTreeYDao;

/**
 * User: azakharov
 * Date: 28.08.14
 * Time: 18:28
 */
public class AddSiteTreeNodeActionTest {
    @Test
    public void testAddSiteTreeNodeActionValidRegexp1()  {
        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/pled_pokr/pled_wool/140\\*200/");
        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());
        EasyMock.expect(mockUserSiteTreeYDao.addUserNode(request.getHostId(), request.getNodeName(),
                        request.getNodeName()))
                .andReturn(new SiteTreeNode(SiteTreeNode.createNodeId("/pled_pokr/pled_wool/140\\*200/", true),
                        SiteTreeNode.ROOT_NODE_ID, "/pled_pokr/pled_wool/140\\*200/", "/pled_pokr/pled_wool/140\\*200" +
                        "/", null, null, null, true, null));

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.NormalResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionValidRegexp2()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/pled_pokr/pled_wool/140*200/");
        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());
        EasyMock.expect(mockUserSiteTreeYDao.addUserNode(request.getHostId(), request.getNodeName(),
                        request.getNodeName()))
                .andReturn(new SiteTreeNode(SiteTreeNode.createNodeId("/pled_pokr/pled_wool/140*200/", true),
                        SiteTreeNode.ROOT_NODE_ID, "/pled_pokr/pled_wool/140*200/", "/pled_pokr/pled_wool/140*200/",
                        null, null, null, true, null));

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.NormalResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionValidRegexp3()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/section?id=123#frag");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());
        EasyMock.expect(mockUserSiteTreeYDao.addUserNode(request.getHostId(), "/section?id=123", "/section?id=123"))
                .andReturn(new SiteTreeNode(SiteTreeNode.createNodeId("/section?id=123", true),
                        SiteTreeNode.ROOT_NODE_ID, "/section?id=123", "/section?id=123", null, null, null, true, null));

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.NormalResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionInvalidRegexp1()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/\\");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.InvalidNodeNameResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionInvalidRegexp2()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("\\");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.InvalidNodeNameResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionInvalidRegexp3()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/\\1");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.InvalidNodeNameResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionEmptyNodeName()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.InvalidNodeNameResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionSingleSlashNodeName()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.InvalidNodeNameResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionSlashAndFragmentNodeName()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/#fragment");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.InvalidNodeNameResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionSlashAndQueryNodeName()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/?id=100500");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.<SiteTreeNode>emptyList());
        EasyMock.expect(mockUserSiteTreeYDao.addUserNode(request.getHostId(), request.getNodeName(),
                        request.getNodeName()))
                .andReturn(new SiteTreeNode(SiteTreeNode.createNodeId("/?id=100500", true),
                        SiteTreeNode.ROOT_NODE_ID, "/?id=100500", "/?id=100500", null, null, null, true, null));

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.NormalResponse);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeActionDuplicateNode()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/foo/bar");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.singletonList(new SiteTreeNode(SiteTreeNode.createNodeId("/foo/bar", true),
                        SiteTreeNode.ROOT_NODE_ID, "/foo/bar", "/foo/bar", null, null, null, true, null)));

        EasyMock.replay(mockUserSiteTreeYDao);
        AddSiteTreeNodeResponse response = action.process(request);
        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.DuplicationUserNodeError);
        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeCyrillicUrl()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("/_тест\\*кириллического_урла");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.emptyList());
        EasyMock.expect(mockUserSiteTreeYDao.addUserNode(request.getHostId(), "/_%D1%82%D0%B5%D1%81%D1%82\\*%D0%BA%D0" +
                        "%B8%D1%80%D0%B8%D0%BB%D0%BB%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%BE%D0%B3%D0%BE_%D1%83%D1%80%D0" +
                        "%BB%D0%B0", request.getNodeName()))
                .andReturn(new SiteTreeNode(SiteTreeNode.createNodeId("/_%D1%82%D0%B5%D1%81%D1%82\\*%D0%BA%D0%B8%D1" +
                        "%80%D0%B8%D0%BB%D0%BB%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%BE%D0%B3%D0%BE_%D1%83%D1%80%D0%BB%D0" +
                        "%B0", true),
                        SiteTreeNode.ROOT_NODE_ID, "/_%D1%82%D0%B5%D1%81%D1%82\\*%D0%BA%D0%B8%D1%80%D0%B8%D0%BB%D0%BB" +
                        "%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%BE%D0%B3%D0%BE_%D1%83%D1%80%D0%BB%D0%B0", "/_" +
                        "тест\\*кириллического_урла", null, null, null, true, null));

        EasyMock.replay(mockUserSiteTreeYDao);

        AddSiteTreeNodeResponse response = action.process(request);

        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.NormalResponse);
        AddSiteTreeNodeResponse.NormalResponse nr = (AddSiteTreeNodeResponse.NormalResponse) response;

        Assert.assertNotNull(nr.getNode());
        Assert.assertEquals("/_тест\\*кириллического_урла", nr.getNode().getName());
        Assert.assertNull(nr.getNode().getOriginalName());

        EasyMock.verify(mockUserSiteTreeYDao);
    }

    @Test
    public void testAddSiteTreeNodeNoLeadingSlash()  {

        UserSiteTreeYDao mockUserSiteTreeYDao = EasyMock.createMock(UserSiteTreeYDao.class);

        AddSiteTreeNodeAction action = new AddSiteTreeNodeAction(mockUserSiteTreeYDao);
        AddSiteTreeNodeRequest request = new AddSiteTreeNodeRequest();
        request.setUserId(139551309);
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "lenta.ru", 80);
        request.setHostId(hostId.toStringId());
        request.setConvertedHostId(hostId);
        request.setNodeName("some/node");

        EasyMock.expect(mockUserSiteTreeYDao.getUserTreeNodes(EasyMock.anyObject(WebmasterHostId.class)))
                .andReturn(Collections.emptyList());
        EasyMock.expect(mockUserSiteTreeYDao.addUserNode(request.getHostId(), "/some/node", request.getNodeName()))
                .andReturn(new SiteTreeNode(SiteTreeNode.createNodeId("/some/node", true),
                        SiteTreeNode.ROOT_NODE_ID, "/some/node", request.getNodeName(), null, null, null, true, null));

        EasyMock.replay(mockUserSiteTreeYDao);

        AddSiteTreeNodeResponse response = action.process(request);

        Assert.assertTrue(response instanceof AddSiteTreeNodeResponse.NormalResponse);
        AddSiteTreeNodeResponse.NormalResponse nr = (AddSiteTreeNodeResponse.NormalResponse) response;

        Assert.assertNotNull(nr.getNode());
        Assert.assertEquals("some/node", nr.getNode().getName());
        Assert.assertNull(nr.getNode().getOriginalName());

        EasyMock.verify(mockUserSiteTreeYDao);
    }

}
