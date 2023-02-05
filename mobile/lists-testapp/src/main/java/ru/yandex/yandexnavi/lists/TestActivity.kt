package ru.yandex.yandexnavi.lists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_test.*
import ru.yandex.yandexnavi.lists.api.ItemPresenter
import ru.yandex.yandexnavi.lists.api.ViewFactory
import ru.yandex.yandexnavi.lists.items.SimpleListPresenter
import ru.yandex.yandexnavi.lists.items.analytics.CompositeLifecycleRegisterObserver
import ru.yandex.yandexnavi.lists.items.analytics.LifecycleMeta
import ru.yandex.yandexnavi.lists.items.analytics.LifecycleRegisterRepo
import ru.yandex.yandexnavi.lists.items.item_a.ItemAPresenter
import ru.yandex.yandexnavi.lists.items.item_a.ItemAView
import ru.yandex.yandexnavi.lists.items.item_b.ItemBPresenter
import ru.yandex.yandexnavi.lists.items.item_b.ItemBView
import ru.yandex.yandexnavi.lists.testapp.R
import kotlin.reflect.KClass

private const val ITEMS_VIEWS_INFO_FORMAT = "==> Item views" +
    "\ncreated count=%d" +
    "\nbinds count=%d" +
    "\nattached count=%d"

private const val ITEMS_PRESENTERS_INFO_FORMAT = "==> Item presenters" +
    "\ncreated count=%d" +
    "\nbinds count=%d" +
    "\nattached count=%d"

class TestActivity : AppCompatActivity(
    R.layout.activity_test
) {
    // === Lifecycle logging
    // > A
    private val aPresentersRegister = LifecycleRegisterRepo()
    private val aViewsRegister = LifecycleRegisterRepo()
    private val aPresenterFactory = { ItemAPresenter(aPresentersRegister) }
    private val aViewFactory = { root: ViewGroup ->
        ItemAView(root.context).apply { lifecycleRegister = aViewsRegister }
    }

    // > B
    private val bPresentersRegister = LifecycleRegisterRepo()
    private val bViewsRegister = LifecycleRegisterRepo()
    private val bPresenterFactory = { ItemBPresenter(bPresentersRegister) }
    private val bViewFactory = { root: ViewGroup ->
        (LayoutInflater.from(root.context).inflate(R.layout.item_b, root, false) as ItemBView)
            .apply { lifecycleRegister = bViewsRegister }
    }

    // > Composite
    private val presentersLifecycle = CompositeLifecycleRegisterObserver(
        aPresentersRegister,
        bPresentersRegister
    )
    private val viewsLifecycle = CompositeLifecycleRegisterObserver(
        aViewsRegister,
        bViewsRegister
    )

    // === Creation
    private val presentersGenerator: Sequence<ItemPresenter> = generateSequence {
        listOf(
            aPresenterFactory,
            bPresenterFactory
        )
            .random()
            .invoke()
    }
    private val listPresenter = SimpleListPresenter(presentersGenerator.take(500).toList())
    private val viewMappings: Map<KClass<out ItemPresenter>, ViewFactory> = mapOf(
        ItemAPresenter::class to aViewFactory,
        ItemBPresenter::class to bViewFactory
    )

    // === Activity variables
    private val disposable = CompositeDisposable()
    private lateinit var presentersLifecycleMeta: LifecycleMeta
    private lateinit var viewsLifecycleMeta: LifecycleMeta

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view_items.apply {
            layoutManager = LinearLayoutManager(this@TestActivity, RecyclerView.VERTICAL, false)
            attachListPresenter(listPresenter, viewMappings)
        }
        view_refresh.setOnClickListener {
            view_items.adapter!!.notifyDataSetChanged()
        }

        disposable += presentersLifecycle.meta()
            .subscribe {
                presentersLifecycleMeta = it
                updateRightInfo()
            }
        disposable += viewsLifecycle.meta()
            .subscribe {
                viewsLifecycleMeta = it
                updateRightInfo()
            }
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    private fun updateRightInfo() {
        if (!this::presentersLifecycleMeta.isInitialized || !this::viewsLifecycleMeta.isInitialized) {
            return
        }

        val presentersInfo = presentersLifecycleMeta.run {
            ITEMS_PRESENTERS_INFO_FORMAT.format(
                created,
                binded,
                attached
            )
        }

        val viewsInfo = viewsLifecycleMeta.run {
            ITEMS_VIEWS_INFO_FORMAT.format(
                created,
                binded,
                attached
            )
        }

        view_info.text = viewsInfo + "\n" + presentersInfo
    }
}
