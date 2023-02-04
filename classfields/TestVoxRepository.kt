package ru.auto.ara.core.feature.calls

import com.voximplant.sdk.call.ILocalVideoStream
import com.voximplant.sdk.call.IRemoteVideoStream
import ru.auto.data.model.Currency
import ru.auto.feature.calls.data.AudioSource
import ru.auto.feature.calls.data.CallAction
import ru.auto.feature.calls.data.CallData
import ru.auto.feature.calls.data.CallEvent
import ru.auto.feature.calls.data.CallPayload
import ru.auto.feature.calls.data.ConnectionEvent
import ru.auto.feature.calls.data.IVoxRepository
import ru.auto.feature.calls.data.SensorsOrientation
import ru.auto.feature.calls.data.SupportedFeature
import ru.auto.feature.calls.data.VideoCamera
import rx.Completable
import rx.Observable
import rx.Single

const val OUTGOING_TEST_VOX_CALL_ID = "OutgoingTestCallId"
const val INCOMING_TEST_VOX_CALL_ID = "IncomingTestCallId"

const val OFFER_ID_FOR_VOX_TEST = "1092718938-48ec2434"
const val OFFER_CARD_PATH_FOR_VOX_TEST = "https://auto.ru/cars/used/sale/"

private const val OFFER_PRICE = 1000000F
private const val OFFER_NAME = "Test Car"
private const val USER_NAME = "Test Car"

val INCOMING_TEST_VOX_CALL = CallData.Incoming(
    callId = INCOMING_TEST_VOX_CALL_ID,
    payload = CallPayload(
        offerPic = null,
        offerLink = OFFER_CARD_PATH_FOR_VOX_TEST + OFFER_ID_FOR_VOX_TEST,
        offerPrice = OFFER_PRICE,
        offerPriceCurrency = Currency.RUR,
        offerName = OFFER_NAME,
        userPic = null,
        userAlias = USER_NAME,
        userPhone = null,
        line1 = OFFER_NAME,
        line2 = "1 000 000 ₽"
    ),
    rawVoxData = emptyMap(),
    supportedFeatures = emptySet()
)

val INCOMING_TEST_VOX_CALL_VIDEO_SUPPORT =
    INCOMING_TEST_VOX_CALL.copy(
        supportedFeatures = setOf(SupportedFeature.VIDEO)
    )

class TestVoxRepository(
    private val args: VoxRepoArgs,
) : IVoxRepository {

    override val isReadyForCalling: Boolean = true

    @Volatile
    private var activeAudioDevice = AudioSource.EARPIECE

    override fun connect(): Completable = Completable.complete()

    override fun callTo(payload: Map<String, String>, destination: String?, enableVideoImmediately: Boolean): Single<String> =
        Single.just(OUTGOING_TEST_VOX_CALL_ID)

    override fun disconnect(): Completable = Completable.complete()

    override fun observeIncomingCalls(): Observable<CallData.Incoming> = args.incomingCallObservable

    override fun observeCall(id: String): Observable<CallEvent> = args.callObservableFactory(id)
        .doOnNext { event ->
            if (event is CallEvent.ActiveAudioDeviceChanged) {
                activeAudioDevice = event.device
            }
        }

    override fun makeActionOnCall(id: String, action: CallAction): Completable = Completable.complete()

    override fun registerForPushNotifications(firebaseOrHmsToken: String): Completable = Completable.complete()

    override fun unregisterFromPushNotification(): Completable = Completable.complete()

    override fun handlePushNotification(notification: Map<String, String>): Completable =
        Completable.fromCallable {
            args.onPushNotification(notification)
        }

    override fun observeConnectionEvents(): Observable<ConnectionEvent> = Observable.empty()

    override fun getAudioDevices(): Single<Set<AudioSource>> = Single.just(setOf(AudioSource.EARPIECE, AudioSource.SPEAKER))

    override fun getActiveAudioDevice(): Single<AudioSource> = Single.just(activeAudioDevice)

    override fun setAudioSource(chosen: AudioSource): Completable = Completable.complete()

    override fun sendVideo(callId: String, value: Boolean): Completable = Completable.complete()

    override fun setLocalVideoSource(chosen: VideoCamera): Completable = Completable.complete()

    override fun getCurrentLocalVideoSource(): Single<VideoCamera> = Single.just(VideoCamera.FRONT)

    override fun setCustomCameraResolution(width: Int, height: Int, selfRotation: SensorsOrientation): Completable =
        Completable.complete()

    override fun getVideoStreams(callId: String): Single<Pair<ILocalVideoStream?, IRemoteVideoStream?>> =
        Single.just(null to null)

    override fun observeSecondsTicking(callId: String): Observable<Long> = Observable.just(0)

    override fun hasValidClient(): Single<Boolean> = Single.just(true)

    data class VoxRepoArgs(
        val incomingCallObservable: Observable<CallData.Incoming>,
        val callObservableFactory: (String) -> Observable<CallEvent>,
        val onPushNotification: (Map<String, String>) -> Unit,
    )
}
