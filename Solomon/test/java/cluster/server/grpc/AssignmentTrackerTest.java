package ru.yandex.solomon.alert.cluster.server.grpc;

import java.util.concurrent.ArrayBlockingQueue;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.alert.cluster.server.grpc.AssignmentTracker.ObsoleteListener;
import ru.yandex.solomon.balancer.AssignmentSeqNo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author Vladimir Gordiychuk
 */
public class AssignmentTrackerTest {

    private AssignmentTracker tracker;

    @Before
    public void setUp() throws Exception {
        tracker = new AssignmentTracker();
    }

    @Test
    public void valid() {
        assertTrue(tracker.isValid("junk", new AssignmentSeqNo(1, 42)));
        assertTrue(tracker.isValid("junk", new AssignmentSeqNo(1, 42)));
    }

    @Test
    public void invalid() {
        assertTrue(tracker.isValid("junk", new AssignmentSeqNo(1, 42)));
        assertFalse(tracker.isValid("junk", new AssignmentSeqNo(1, 41)));
    }

    @Test
    public void validNew() {
        assertTrue(tracker.isValid("junk", new AssignmentSeqNo(1, 42)));
        assertTrue(tracker.isValid("junk", new AssignmentSeqNo(1, 43)));
        assertTrue(tracker.isValid("junk", new AssignmentSeqNo(2, 44)));
    }

    @Test
    public void validDifferentProject() {
        assertTrue(tracker.isValid("one", new AssignmentSeqNo(1, 42)));
        assertTrue(tracker.isValid("two", new AssignmentSeqNo(1, 41)));
    }

    @Test
    public void notifyInvalid() throws InterruptedException {
        var stub = new ObsoleteListenerStub();
        tracker.subscribe(stub);

        assertTrue(tracker.isValid("junk", new AssignmentSeqNo(1, 41)));
        assertTrue(tracker.isValid("junk", new AssignmentSeqNo(1, 42)));

        var obsolete = stub.obsoletes.take();
        assertEquals(new Obsolete("junk", new AssignmentSeqNo(1, 41)), obsolete);
    }

    private static class ObsoleteListenerStub implements ObsoleteListener {
        private final ArrayBlockingQueue<Obsolete> obsoletes = new ArrayBlockingQueue<Obsolete>(100);

        @Override
        public void obsolete(String projectId, AssignmentSeqNo seqNo) {
            obsoletes.add(new Obsolete(projectId, seqNo));
        }
    }

    private record Obsolete(String projectId, AssignmentSeqNo seqNo) {
    }
}
