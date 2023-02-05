package ru.yandex.yandexnavi.lists.items.item_a

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.layout_rectangle.view.*
import ru.yandex.yandexnavi.lists.api.ItemView
import ru.yandex.yandexnavi.lists.items.analytics.LifecycleRegister
import ru.yandex.yandexnavi.lists.testapp.R

const val TEXT_FORMAT = "ITEM_%s" +
    "\npresenter={id:%d, bindCount:%d}" +
    "\nview={id:%d, bindCount:%d}"

class ItemAView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ItemView<ItemAPresenter> {

    lateinit var lifecycleRegister: LifecycleRegister
    private val viewId: Int by lazy { lifecycleRegister.createId() }
    private lateinit var presenter: ItemAPresenter
    private var bindCounter = 0

    init {
        View.inflate(context, R.layout.item_a, this)
        view_content.setBackgroundColor(ContextCompat.getColor(context, R.color.color_blue))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegister.registerAttach()
        presenter.onAttach()
    }

    override fun onDetachedFromWindow() {
        presenter.onDetach()
        lifecycleRegister.registerDetach()
        super.onDetachedFromWindow()
    }

    override fun setPresenter(presenter: ItemAPresenter) {
        bindCounter = maxOf(0, bindCounter + 1)
        lifecycleRegister.registerBind()
        this.presenter = presenter
        presenter.bind()

        view_text.text = TEXT_FORMAT.format(
            "A",
            presenter.presenterId,
            presenter.bindCounter,
            viewId,
            bindCounter
        )
    }
}
