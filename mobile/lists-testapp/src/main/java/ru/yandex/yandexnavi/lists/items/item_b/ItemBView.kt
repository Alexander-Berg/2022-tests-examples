package ru.yandex.yandexnavi.lists.items.item_b

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.layout_rectangle.view.*
import ru.yandex.yandexnavi.lists.api.ItemView
import ru.yandex.yandexnavi.lists.items.analytics.LifecycleRegister
import ru.yandex.yandexnavi.lists.items.item_a.TEXT_FORMAT
import ru.yandex.yandexnavi.lists.testapp.R

class ItemBView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ItemView<ItemBPresenter> {

    lateinit var lifecycleRegister: LifecycleRegister
    private val viewId: Int by lazy { lifecycleRegister.createId() }
    private lateinit var presenter: ItemBPresenter
    private var bindCounter = 0

    init {
        View.inflate(context, R.layout.layout_rectangle, this)
        view_content.setBackgroundColor(ContextCompat.getColor(context, R.color.color_green))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegister.registerAttach()
        presenter.onAttach()
    }

    override fun onDetachedFromWindow() {
        lifecycleRegister.registerDetach()
        presenter.onDetach()
        super.onDetachedFromWindow()
    }

    override fun setPresenter(presenter: ItemBPresenter) {
        bindCounter = maxOf(0, bindCounter + 1)
        lifecycleRegister.registerBind()
        this.presenter = presenter
        presenter.bind()

        view_text.text = TEXT_FORMAT.format(
            "B",
            presenter.presenterId,
            presenter.bindCounter,
            viewId,
            bindCounter
        )
    }
}
