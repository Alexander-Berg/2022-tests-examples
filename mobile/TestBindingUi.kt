package com.edadeal.android.ui.common.dev

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.edadeal.android.R
import com.edadeal.android.databinding.HomeBinding
import com.edadeal.android.model.BasePresenter
import com.edadeal.android.model.navigation.RouterStackEntry
import com.edadeal.android.model.util.SchedulerProvider
import com.edadeal.android.ui.common.MainOffsetDecoration
import com.edadeal.android.ui.common.base.BaseRecyclerAdapter
import com.edadeal.android.ui.common.base.ChildUi
import com.edadeal.android.ui.common.base.ParentUi
import com.edadeal.android.ui.common.base.RecyclerStateController
import com.edadeal.android.ui.common.bindings.WalletBalanceBinding
import com.edadeal.android.ui.common.bottomnav.BottomNavMode
import com.edadeal.android.ui.getVectorDrawable
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

class TestBindingUi(
    override val controller: TestBindingController,
    stackEntry: RouterStackEntry,
    parentUi: ParentUi,
    inflater: LayoutInflater
) : ChildUi(parentUi, stackEntry, inflater) {

    override val viewBinding = HomeBinding.inflate(inflater)
    override val presenter = object : BasePresenter<Unit>(Unit, SchedulerProvider.ImmediateScheduler) {
        override fun doQuery(query: Unit) {}
    }
    override val bottomNavMode = BottomNavMode.Hidden
    private val adapterTestBinding = BaseRecyclerAdapter().apply {
        bindings = listOf(WalletBalanceBinding({}))
    }
    private val offsetDecoration = object : MainOffsetDecoration(res) {
        override fun setOffsets(rect: Rect, item: Any?, topItem: Any?, bottomItem: Any?, leftItem: Any?, rightItem: Any?) {
            super.setOffsets(rect, item, topItem, bottomItem, leftItem, rightItem)
            if (item is WalletBalanceBinding.Item) rect.setDp(8, 10, 10, 8)
        }
    }

    init {
        with(viewBinding.recycler.root) {
            layoutManager = LinearLayoutManager(ctx)
            adapter = adapterTestBinding
            addItemDecoration(offsetDecoration)
            setPadding(0, 0, 0, res.getDimensionPixelSize(R.dimen.bottomNavHeight))
        }
        controller.createStateRestoringDataObserver(viewBinding.recycler.root)

        controller.items?.let {
            val items = mutableListOf<Any>()
            for ((cls, packedItem) in it) {
                val aux = findAux(cls) ?: continue
                items.add(aux.unpack(packedItem))
            }
            adapterTestBinding.items = items
        }
        controller.items = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val always = MenuItemCompat.SHOW_AS_ACTION_ALWAYS
        val subMenu = menu.addSubMenu(0, Menu.NONE, 0, R.string.commonEdit)
        subMenu.setIcon(res.getVectorDrawable(R.drawable.ic_add_black_24dp, R.color.appbarIconColor))
        MenuItemCompat.setShowAsAction(subMenu.item, always)

        val add = { title: String, action: () -> Unit ->
            val item = subMenu.add(0, Menu.NONE, 0, title)
            item.setOnMenuItemClickListener { action(); true }
            MenuItemCompat.setShowAsAction(item, always)
        }
        for (v in BindingAux.values()) {
            add(v.cls.simpleName) {
                v.create?.invoke(ctx)?.let(this::updateAdapter)
                    ?: v.createThruDialog?.invoke(ctx, this::updateAdapter)?.show()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        controller.items = adapterTestBinding.items.mapNotNull { item ->
            adapterTestBinding.bindings.find { it.getItemId(item) != null }?.let { findAux(it.javaClass) }?.let {
                it.cls to it.pack(item)
            }
        }
    }

    private fun findAux(cls: Class<*>) = BindingAux.values().find { it.cls == cls }
    private fun updateAdapter(item: Any) { adapterTestBinding.items = adapterTestBinding.items + listOf(item) }

    private enum class BindingAux(
        val cls: Class<*>,
        val pack: (Any) -> Parcelable,
        val unpack: (Parcelable) -> Any,
        val create: ((Context) -> Any)? = null,
        val createThruDialog: ((Context, (Any) -> Unit) -> AlertDialog)? = null
    ) {

        WALLET_BALANCE(
            cls = WalletBalanceBinding::class.java,
            pack = { Bundle().apply { putInt("v", (it as WalletBalanceBinding.Item).amount) } },
            unpack = { WalletBalanceBinding.Item((it as Bundle).getInt("v")) },
            createThruDialog = { ctx, onDone ->
                val editText = EditText(ctx).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                AlertDialog.Builder(ctx)
                    .setTitle("Amount")
                    .setView(editText)
                    .setPositiveButton("OK") { _, _ ->
                        val amount = editText.text.toString().toDoubleOrNull() ?: .0
                        onDone(WalletBalanceBinding.Item((amount * 100).roundToInt()))
                    }
                    .create()
            }
        )
    }
}

typealias TestBindingItems = List<Pair<Class<*>, Parcelable>>

class TestBindingController @JvmOverloads constructor(
    bundle: Bundle = Bundle()
) : RecyclerStateController(bundle) {

    override val uiClass = TestBindingUi::class.java
    var items: TestBindingItems? by parcelableProxyBundle(
        { p: TestBindingItemsParcelable -> p.v },
        { TestBindingItemsParcelable(it) }
    )

    override fun createUi(
        parentUi: ParentUi,
        inflater: LayoutInflater,
        stackEntry: RouterStackEntry,
        navigationResult: Any?
    ) = TestBindingUi(this, stackEntry, parentUi, inflater)

    @Parcelize
    private class TestBindingItemsParcelable(var v: TestBindingItems) : Parcelable
}
