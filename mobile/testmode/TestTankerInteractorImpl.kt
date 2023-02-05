package ru.yandex.yandexmaps.tools.tanker.sync.impl.testmode

import ru.yandex.yandexmaps.tools.tanker.sync.impl.TankerInteractor
import ru.yandex.yandexmaps.tools.tanker.sync.impl.di.ImplType
import java.io.File
import javax.inject.Inject

class TestTankerInteractorImpl @Inject constructor(
    @ImplType(isTest = false) private val prodImpl: TankerInteractor,
) : TankerInteractor {

    override fun addStrings(file: File): TankerInteractor.UploadResult {
        println("attempt to upload file: ${file.path}\n content:\n${file.readText()}")
        return TankerInteractor.UploadResult.Success
    }

    override fun downloadStrings(): TankerInteractor.DownloadResult {
        return prodImpl.downloadStrings()
    }
}
