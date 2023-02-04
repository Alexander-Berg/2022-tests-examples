package ru.auto.ara.presentation

import ru.auto.ara.presentation.view.ViewModelView

class ViewModelViewStub<ViewModel : Any> : ViewModelView<ViewModel> {

    lateinit var state: ViewModel

    override fun update(newState: ViewModel) {
        state = newState
    }

    override fun showToast(message: String) {
        // stub
    }

    override fun showToast(message: Int) {
        // stub
    }

    override fun showSnackWithAction(msg: String, action: () -> Unit, actionName: String) {
        // stub
    }

    override fun showSnackWithAction(msg: Int, action: () -> Unit, actionName: Int) {
        // stub
    }

    override fun dismissSnack() {
        // stub
    }

}
