package ru.yandex.disk.qa_tools.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.BaseFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.yandex.disk.qa_tools.databinding.FTestUrlsBinding
import ru.yandex.disk.qa_tools.ui.adapter.UrlsAdapter
import ru.yandex.disk.qa_tools.ui.adapter.UrlsGroupItem
import ru.yandex.disk.qa_tools.ui.adapter.UrlsItem

interface OnClickPositionUrl {
    fun openUrl(url: String)
}

private const val TAG_SAVE_EXPANDABLE = "expandable_position"

private val urlGroups = listOf(
    UrlsGroupItem("Видео", listOf(
        UrlsItem("Конвертер форматы", "https://yadi.sk/d/iBUAUA1Zl5bgog"),
        UrlsItem("Проблемные форматы", "https://yadi.sk/d/Pxm_zKIwF__P8A"),
        UrlsItem("Длинные видео (фильмы)", "https://yadi.sk/d/7EIvAVitS_7ZKA"),
        UrlsItem("Папка с разными форматами", "https://yadi.sk/d/jnbObDs93E6nkm")
    )),
    UrlsGroupItem("Изображения", listOf(
        UrlsItem("Гифки", "https://yadi.sk/d/YOeRzYTNOM7prA"),
        UrlsItem("Разные соотношения сторон", "https://yadi.sk/d/qIq51vw624ZCGw"),
        UrlsItem("Разные форматы", "https://yadi.sk/d/5L9IpCXTU6PmzQ"),
        UrlsItem("Векторы", "https://yadi.sk/d/y8909FxBaQK8OA"),
        UrlsItem("Вертикальные панорамы", "https://yadi.sk/d/0tGKkg-K1XotJQ"),
        UrlsItem("Конвертер форматы", "https://yadi.sk/d/yiuvnBWArpSbhQ"),
        UrlsItem("Белая картинка", "https://yadi.sk/i/b2w1e-orhejbuA")
    )),
    UrlsGroupItem("Документы", listOf(
        UrlsItem("doc", "https://yadi.sk/d/9rouH8Pc885cKw"),
        UrlsItem("ods", "https://yadi.sk/d/b6TUF6T6J0XkhQ"),
        UrlsItem("odt", "https://yadi.sk/d/tBZtCUUPMU71IQ"),
        UrlsItem("pdf", "https://yadi.sk/d/oKKSa60ck1aLrw"),
        UrlsItem("ppt", "https://yadi.sk/d/C3OBP-YAJ-IqoQ"),
        UrlsItem("rtf", "https://yadi.sk/d/Sq81mH71reOLAg"),
        UrlsItem("txt", "https://yadi.sk/d/6GmaCbJIqwWVAQ"),
        UrlsItem("xls", "https://yadi.sk/d/q4c3yjXCl6amSQ"),
        UrlsItem("Архивы", "https://yadi.sk/d/TegfeheKwwIhSA"),
        UrlsItem("Книги", "https://yadi.sk/d/mAZnsV9rKF9_Xw"),
        UrlsItem("Куча разных форматов", "https://yadi.sk/d/twGe_qg0W1QKmg")
    )),
    UrlsGroupItem("Аудио", listOf(
        UrlsItem("5 аудиофайлов", "https://yadi.sk/d/gnPfSwQGv01QRg"),
        UrlsItem("Аудиокнига", "https://yadi.sk/d/qEDXthcSlTENxQ"),
        UrlsItem("Еще одна аудиокнига", "https://yadi.sk/d/BSJhJwAs1IbPmw"),
        UrlsItem("Длинное аудио", "https://yadi.sk/d/y-ztCXAqbkwSdw"),
        UrlsItem("Разные форматы", "https://yadi.sk/d/S1SW9hpiNoomhg")
    )),
    UrlsGroupItem("Тестирование Ленты", listOf(
        UrlsItem("10 видео", "https://yadi.sk/d/5HatpByu3Nrf4M"),
        UrlsItem("10 картинок", "https://yadi.sk/d/Xlt4GtGu3NrfAb"),
        UrlsItem("10 книг", "https://yadi.sk/d/28DvJKVu3NrfG6"),
        UrlsItem("3 блока (фото видео книги без показать все) ", "https://yadi.sk/d/qA14MiI13Nrevq"),
        UrlsItem("4 блока (фото видео книги пдф с показать все)", "https://yadi.sk/d/MKIA-38r3Nres8"),
        UrlsItem("4 видео ", "https://yadi.sk/d/VzErcuE_3Nrf6r"),
        UrlsItem("4 картинки", "https://yadi.sk/d/pVg0a1nG3NrfBb"),
        UrlsItem("4 книги", "https://yadi.sk/d/3ezmmvTf3NrfKC"),
        UrlsItem("Контентые блоки с кнопкой Показать все", "https://yadi.sk/d/OyirG0lb-j1sjw"),
        UrlsItem("Контентые блоки без кнопки показать все", "https://yadi.sk/d/Sq-IiVwbMbZonw"),
        UrlsItem("Папка для фолдерблока", "https://yadi.sk/d/u8f0enQr3NrgKA"),
        UrlsItem("Фотоформаты (портрет и ландшафт)", "https://yadi.sk/d/z9ORKHdg3NrgRZ")
    )),
    UrlsGroupItem("Остальное", listOf(
        UrlsItem("Кривые имена", "https://yadi.sk/d/LbpU_QwL3KHdZw"),
        UrlsItem("Карабченные файлы", "https://yadi.sk/d/I9ezf3Cb3Gq3D3"),
        UrlsItem("Сортировка", "https://yadi.sk/d/ofece9TuyDfB8")
    ))
)

class TestUrlsFragment : BaseFragment() {

    private lateinit var urlsAdapter: UrlsAdapter

    private var _binding: FTestUrlsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        _binding = FTestUrlsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        urlsAdapter = UrlsAdapter(layoutInflater, object : OnClickPositionUrl {
            override fun openUrl(url: String) {
                startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
            }
        })
        with(binding.recyclerViewUrls) {
            layoutManager = LinearLayoutManager(context)
            adapter = urlsAdapter
            if (savedInstanceState != null)
                (adapter as UrlsAdapter).expandedPosition = savedInstanceState.getInt(TAG_SAVE_EXPANDABLE)
            (adapter as UrlsAdapter).urls = urlGroups
            addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(TAG_SAVE_EXPANDABLE, urlsAdapter.expandedPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): TestUrlsFragment {
            return TestUrlsFragment()
        }
    }
}
