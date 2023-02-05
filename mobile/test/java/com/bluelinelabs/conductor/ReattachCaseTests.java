package com.bluelinelabs.conductor;

import android.os.Bundle;

import com.bluelinelabs.conductor.internal.LifecycleHandler;
import com.bluelinelabs.conductor.utils.ActivityProxy;
import com.bluelinelabs.conductor.utils.AttachFakingFrameLayout;
import com.bluelinelabs.conductor.utils.TestController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ReattachCaseTests {

    private ActivityProxy activityProxy;
    private Router router;

    public void createActivityController(Bundle savedInstanceState) {
        activityProxy = new ActivityProxy().create(savedInstanceState).start().resume();
        router = Conductor.attachRouter(activityProxy.getActivity(), activityProxy.getView(), savedInstanceState);
        if (!router.hasRootController()) {
            router.setRoot(RouterTransaction.with(new TestController()));
        }
    }

    @Before
    public void setup() {
        createActivityController(null);
    }

    @Test
    public void testPendingChangesAfterRotation() {
        Controller controller1 = new TestController();
        Controller controller2 = new TestController();

        // first activity
        ActivityProxy activityProxy = new ActivityProxy().create(null);
        AttachFakingFrameLayout container1 = new AttachFakingFrameLayout(activityProxy.getActivity());

        container1.setNeedDelayPost(true); // delay forever as view will be removed
        activityProxy.setView(container1);

        // first attachRouter: Conductor.attachRouter(activityProxy.getActivity(), container1, null)
        LifecycleHandler lifecycleHandler = LifecycleHandler.install(activityProxy.getActivity());
        Router router = lifecycleHandler.getRouter(container1, null);
        router.setRoot(RouterTransaction.with(controller1));

        // setup controllers
        router.pushController(RouterTransaction.with(controller2));

        // simulate setRequestedOrientation in activity onCreate
        activityProxy.start().resume();
        Bundle savedState = new Bundle();
        activityProxy.saveInstanceState(savedState).pause().stop(true);

        // recreate activity and view
        activityProxy = new ActivityProxy().create(savedState);
        AttachFakingFrameLayout container2 = new AttachFakingFrameLayout(activityProxy.getActivity());
        activityProxy.setView(container2);

        // second attach router with the same lifecycleHandler (do manually as Roboelectric recreates retained fragments)
        // Conductor.attachRouter(activityProxy.getActivity(), container2, savedState);
        router = lifecycleHandler.getRouter(container2, savedState);
        router.rebindIfNeeded();

        activityProxy.start().resume();

        assertTrue(controller2.isAttached());
    }
}