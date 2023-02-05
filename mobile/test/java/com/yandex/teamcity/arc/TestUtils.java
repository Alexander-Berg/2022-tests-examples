package com.yandex.teamcity.arc;

import jetbrains.buildServer.vcs.VcsChangeInfo;
import org.junit.Test;

import ru.yandex.teamcity.arc.client.ChangeType;

import static com.yandex.teamcity.arc.Utils.getTypeFromGrpcChangeType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestUtils {

    @Test
    public void getTypeByStatusTest() {
        assertEquals(VcsChangeInfo.Type.ADDED, getTypeFromGrpcChangeType(ChangeType.add));
        assertEquals(VcsChangeInfo.Type.ADDED, getTypeFromGrpcChangeType(ChangeType.copy));
        assertEquals(VcsChangeInfo.Type.ADDED, getTypeFromGrpcChangeType(ChangeType.move));
        assertEquals(VcsChangeInfo.Type.CHANGED, getTypeFromGrpcChangeType(ChangeType.modify));
        assertEquals(VcsChangeInfo.Type.REMOVED, getTypeFromGrpcChangeType(ChangeType.delete));
        assertEquals(VcsChangeInfo.Type.NOT_CHANGED, getTypeFromGrpcChangeType(ChangeType.none));
    }

    @Test
    public void testSpecialBranch() {
        assertTrue(Utils.isSpecialBranch("arcadia/users/robot-stark/1760535/rebase"));
        assertTrue(Utils.isSpecialBranch("arcadia/users/robot-stark/mail/android/1662997/head"));
        assertTrue(Utils.isSpecialBranch("arcadia/users/robot-stark/tv-music-app/1761102/merge_head"));
        assertTrue(Utils.isSpecialBranch("arcadia/users/robot-stark/navi/1705497/merge_pin"));
        assertFalse(Utils.isSpecialBranch("arcadia/users/robot-stark/navi/1705497/not_merge_pin"));
        assertFalse(Utils.isSpecialBranch("arcadia/users/robot-stark/navi/1705497not_merge_pin"));
        assertFalse(Utils.isSpecialBranch("arcadia/trunk"));
    }
}
