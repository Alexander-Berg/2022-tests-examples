package ru.yandex.io.audio

import org.junit.Assert
import org.junit.Test
import ru.yandex.io.sdk.audio.ChannelData
import ru.yandex.quasar.protobuf.YandexIO

class ChannelDataTest {
    @Test
    fun given_channelDataType_when_convertingToProto_then_shouldConvertCorrectly() {
        Assert.assertEquals(YandexIO.IOAudioChannel.Type.values().size, ChannelData.Type.values().size)
        Assert.assertEquals(YandexIO.IOAudioChannel.Type.RAW_VALUE, ChannelData.Type.RAW.ordinal)
        Assert.assertEquals(YandexIO.IOAudioChannel.Type.VQE_VALUE, ChannelData.Type.VQE.ordinal)
        Assert.assertEquals(YandexIO.IOAudioChannel.Type.FEEDBACK_VALUE, ChannelData.Type.FEEDBACK.ordinal)
        Assert.assertEquals(YandexIO.IOAudioChannel.Type.BEAMFORMING_VALUE, ChannelData.Type.BEAMFORMING.ordinal)
        Assert.assertEquals(YandexIO.IOAudioChannel.Type.BACKGROUND_NOISE_REDUCER_VALUE, ChannelData.Type.BACKGROUND_NOISE_REDUCER.ordinal)
        Assert.assertEquals(YandexIO.IOAudioChannel.Type.MAIN_MIC_SYNC_VALUE, ChannelData.Type.MAIN_MIC_SYNC.ordinal)
        Assert.assertEquals(YandexIO.IOAudioChannel.Type.AUXILIARY_MIC_SYNC_VALUE, ChannelData.Type.AUXILIARY_MIC_SYNC.ordinal)
        Assert.assertEquals(YandexIO.IOAudioChannel.Type.FEEDBACK_SYNC_VALUE, ChannelData.Type.FEEDBACK_SYNC.ordinal)
    }
}
