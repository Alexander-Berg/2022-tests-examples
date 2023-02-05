package com.bluelinelabs.conductor.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.NonNull
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType

class TestController : Controller() {

    var currentCallState = CallState(false)
    var changeHandlerHistory = ChangeHandlerHistory()

    @NonNull
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        currentCallState.createViewCalls++
        val view: FrameLayout = AttachFakingFrameLayout(inflater.context)
        view.id = VIEW_ID
        val childContainer1: FrameLayout = AttachFakingFrameLayout(inflater.context)
        childContainer1.id = CHILD_VIEW_ID_1
        view.addView(childContainer1)
        val childContainer2: FrameLayout = AttachFakingFrameLayout(inflater.context)
        childContainer2.id = CHILD_VIEW_ID_2
        view.addView(childContainer2)
        return view
    }

    override fun onChangeStarted(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeStarted(changeHandler, changeType)
        currentCallState.changeStartCalls++
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        currentCallState.changeEndCalls++
        if (changeHandler is MockChangeHandler) {
            changeHandlerHistory.addEntry(changeHandler.from, changeHandler.to, changeType.isPush, changeHandler)
        } else {
            changeHandlerHistory.isValidHistory = false
        }
    }

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        currentCallState.contextAvailableCalls++
    }

    override fun onContextUnavailable() {
        super.onContextUnavailable()
        currentCallState.contextUnavailableCalls++
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        currentCallState.attachCalls++
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        currentCallState.detachCalls++
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        currentCallState.destroyViewCalls++
    }

    override fun onDestroy() {
        super.onDestroy()
        currentCallState.destroyCalls++
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        currentCallState.saveViewStateCalls++
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        currentCallState.restoreViewStateCalls++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        currentCallState.saveInstanceStateCalls++
        outState.putParcelable(KEY_CALL_STATE, currentCallState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentCallState = savedInstanceState.getParcelable(KEY_CALL_STATE)!!
        currentCallState.restoreInstanceStateCalls++
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        currentCallState.onActivityResultCalls++
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        currentCallState.onRequestPermissionsResultCalls++
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        currentCallState.createOptionsMenuCalls++
    }

    companion object {

        @IdRes
        const val VIEW_ID = 2342

        @IdRes
        const val CHILD_VIEW_ID_1 = 2343

        @IdRes
        const val CHILD_VIEW_ID_2 = 2344
        private const val KEY_CALL_STATE = "TestController.currentCallState"
    }
}
