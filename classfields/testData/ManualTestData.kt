package ru.auto.ara.core.testdata

import ru.auto.ara.R
import ru.auto.ara.core.utils.getResourceStringWithoutNonbreakingSpace

class ManualParam(
    val title: String,
    val subtitle: String,
    val image: Int
)

val MANUAL_PARAMS: Array<ManualParam> = arrayOf(
    ManualParam(
        title = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_01_title),
        subtitle = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_01_text),
        image = R.drawable.uchebnik_01
    ),
    ManualParam(
        title = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_02_title),
        subtitle = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_02_text),
        image = R.drawable.uchebnik_02
    ),
    ManualParam(
        title = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_03_title),
        subtitle = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_03_text),
        image = R.drawable.uchebnik_03
    ),
    ManualParam(
        title = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_04_title),
        subtitle = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_04_text),
        image = R.drawable.uchebnik_04
    ),
    ManualParam(
        title = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_05_title),
        subtitle = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_05_text),
        image = R.drawable.uchebnik_05
    ),
    ManualParam(
        title = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_06_title),
        subtitle = getResourceStringWithoutNonbreakingSpace(R.string.manual_page_06_text),
        image = R.drawable.uchebnik_06
    )
)
