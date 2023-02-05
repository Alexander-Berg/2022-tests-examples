package ru.yandex.market.processor.testinstance

/**
 * @param methodNameSuffix этот суффикс будет добавляться к сгенерированному процессором фабричному методу. Например,
 * если класс называется "Example", а суффикс "testInstance", то процессор сгенерирует фабричный метод с названием
 * exampleTestInstance.
 *
 * @param handWrittenMethodName для тех классов, для которых невозможно сгенерировать методы с помощью процессора,
 * нужно написать их руками. В этом случае процессор для генерации тестовых данных будет искать в таких классах метод
 * с имененм, заданном в этом параметре.
 *
 * @param fileNameSuffix этот суффикс будет добавлен к сгенерированному процессором файлу. Логика очень похожа на
 * параметр [methodNameSuffix].
 */
data class ProcessorConfiguration(
    val methodNameSuffix: String,
    val handWrittenMethodName: String,
    val fileNameSuffix: String,
    val fileNameJvmSuffix: String,
    val jvmFactoryMethodName: String
) {
    init {
        require(methodNameSuffix.isNotBlank()) {
            "Require not-blank suffix for generated method names!"
        }
        require(handWrittenMethodName.isNotBlank()) {
            "Require not-blank name for hand-written test factory methods!"
        }
        require(fileNameSuffix.isNotBlank()) {
            "Require not-blank suffix for generated test factory files!"
        }
    }
}