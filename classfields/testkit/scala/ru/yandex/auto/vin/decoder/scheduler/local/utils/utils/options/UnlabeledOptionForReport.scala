package ru.yandex.auto.vin.decoder.scheduler.local.utils.options

case class UnlabeledOptionForReport(
    vin: String,
    mark: String,
    model: String,
    code: String,
    partner: String,
    description: String)

case class LabeledOption(mark: String, code: String)
