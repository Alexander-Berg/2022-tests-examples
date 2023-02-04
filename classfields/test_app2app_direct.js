require(Modules.PushService);
require(Modules.Player);

//config
const DEFAULT_CALLER_ID = "+79584227509" // caller-id for pstn calls
const CALL_TIMEOUT = 20000; //in ms
const ANALYTICS_LABEL_FOR_FIREBASE_PUSHES = "VoxImplantPushLabel";
const TURN_ON_MIC_IN_APP_MESSAGE = "Включите микрофон в приложении, чтобы вас слышали"
const TURN_ON_MIC_IN_SETTINGS_MESSAGE = "Включите микрофон в настройках, чтобы вас слышали"

//constants
const CONNECTED_MSG_TYPE = "connected"
const CALL_FAILED_MSG_TYPE = "callFailed"
const MUTED_MSG_TYPE = "muted"
const VIEWPORT_MSG_TYPE = "viewport"
const EVENT_TYPE_TO_ID = {
    START_APP_SCENARIO_EVENT: 1,
    OUT_APP_CALL_INIT_EVENT: 2,
    OUT_APP_CALL_CONNECTED_EVENT: 3,
    OUT_APP_CALL_FAILED_EVENT: 4,
    OUT_APP_CALL_TIMEOUT_EVENT: 5,
    OUT_PHONE_CALL_INIT_EVENT: 6,
    OUT_PHONE_CALL_CONNECTED_EVENT: 7,
    OUT_PHONE_CALL_FAILED_EVENT: 8,
    IN_CALL_DISCONNECTED_EVENT: 9,
    OUT_APP_CALL_DISCONNECTED_EVENT: 10,
    OUT_PHONE_CALL_DISCONNECTED_EVENT: 11,
    APP_SCENARIO_TERMINATED_EVENT: 12,
    NO_REDIRECT_APP_CALL_EVENT: 13
};

let fallbackStarted = false;
/** @type [String: String] */
let headers;
/** @type Call */
let inCall;
/** @type Call */
let outCall;
let sessionId;
let startTalkTime;
let endTalkTime;

const customData = {
    events: [],
    talkDuration: 0,
    sourceUsername: undefined,
    targetUsername: undefined,
    callerId: undefined,
    sourcePhone: undefined, //todo would be set from incomingHeaders
    targetPhone: undefined,
    redirectId: undefined,
    recordUrl: undefined
};

const allowedHeaders = [
    "X-offer_pic",
    "X-offer_link",
    "X-offer_price",
    "X-offer_price_currency",
    "X-offer_year",
    "X-offer_mark",
    "X-offer_model",
    "X-offer_generation",
    "X-user_pic",
    "X-alias",
    "X-alias_and_subject",
    "X-image",
    "X-url",
    "X-line1",
    "X-line2",
    "X-handle",
    "X-subjectType",
    "X-features"
]

function filterHeaders(inputHeaders) {
    let resultHeaders = {};
    for (let name in inputHeaders) {
        if (inputHeaders.hasOwnProperty(name) && allowedHeaders.indexOf(name) >= 0) {
            resultHeaders[name] = inputHeaders[name];
        }
    }
    return resultHeaders;
}

function processMessage(e, anotherCall) {
    log("Received message " + e.text + " from callId=" + e.call.id());
    const message = JSON.parse(e.text);
    switch (message.event) {
        case MUTED_MSG_TYPE:
            const outMsg = buildMutedMessage(message.data.isMuted)
            anotherCall.sendMessage(outMsg);
            break;

        case VIEWPORT_MSG_TYPE:
            anotherCall.sendMessage(e.text);
            break;

        default:
            log("Error: unexpected message type!");
    }
}

function handleInCallMessage(event) {
    processMessage(event, outCall);
}

function handleOutCallMessage(event) {
    processMessage(event, inCall);
}

function buildMessage(msgType, msgData) {
    return JSON.stringify({
        "event": msgType,
        "data": msgData
    });
}

function buildConnectedMessage(features) {
    return buildMessage(CONNECTED_MSG_TYPE, { "features": features ? features.split(',') : [] });
}

function buildCallFailedMessage(reason) {
    return buildMessage(CALL_FAILED_MSG_TYPE, {"callFailedReason": reason});
}

function buildMutedMessage(isMuted) {
    return buildMessage(MUTED_MSG_TYPE, {"isMuted": isMuted});
}

function initForwardCall(callerId, displayName, scheme) {
    forwardCall(callerId, displayName, scheme);
}

function forwardCall(callerId, displayName, scheme) {
    inCall.record({stereo: true});
    inCall.answer();
    inCall.playProgressTone("RU");

    if (customData.targetUsername) {
        initCallingVoxUser(callerId, displayName, scheme);
    } else {
        addEventAndSend(EVENT_TYPE_TO_ID.NO_REDIRECT_APP_CALL_EVENT);
        callNumber();
    }
}

function initCallingVoxUser(callerId, displayName, scheme) {
    log("Call to vox user");
    addEventAndSend(EVENT_TYPE_TO_ID.OUT_APP_CALL_INIT_EVENT);
    callVoxUser(callerId, displayName, scheme);
}

function callVoxUser(callerId, displayName, scheme) {
    outCall = VoxEngine.callUser({
        username: customData.targetUsername,
        callerid: callerId,
        displayName: displayName,
        extraHeaders: headers,
        video: true,
        scheme: scheme,
        analyticsLabel: ANALYTICS_LABEL_FOR_FIREBASE_PUSHES
    });

    let timer;
    let disconnectionExpected = false;

    outCall.addEventListener(CallEvents.Connected, function (e) {
        clearTimeout(timer);

        const incomingDeviceFeatures = e.headers["X-features-in"];

        outCall.addEventListener(CallEvents.Disconnected, (event) => {
            if (disconnectionExpected && event.headers["X-endReason"] == null) {
                disconnectionExpected = false;
                callVoxUser(callerId, displayName, scheme);
            } else {
                addEventAndSend(EVENT_TYPE_TO_ID.OUT_APP_CALL_DISCONNECTED_EVENT);
                log("Scenario was terminated due to disconnected target")
                terminateScenario();
            }
        });

        function connectCalls() {
            disconnectionExpected = false;
            startTalkTime = Date.now();
            addEventAndSend(EVENT_TYPE_TO_ID.OUT_APP_CALL_CONNECTED_EVENT);
            VoxEngine.sendMediaBetween(inCall, outCall);
            inCall.sendMessage(buildConnectedMessage(incomingDeviceFeatures));
            outCall.sendMessage(buildConnectedMessage());
        }

        //TODO добавить ссылку на вики, где описано зачем это нужно
        if (e.headers["X-mic"] === "denied") {
            log("Incoming call microphone permission denied");
            disconnectionExpected = true;
            recursivePlayback(TURN_ON_MIC_IN_SETTINGS_MESSAGE, outCall);
        } else if (e.headers["X-mic"] === "notDetermined") {
            log("Incoming call microphone permission was not asked");
            disconnectionExpected = false;

            const cancellationToken = recursivePlayback(TURN_ON_MIC_IN_APP_MESSAGE, outCall);

            outCall.addEventListener(CallEvents.MessageReceived, (e) => {
                const message = JSON.parse(e.text);
                if (message.event === "micPermission") {
                    log("Incoming call microphone permission with data=" + message.data);

                    if (message.data === "authorized") {
                        cancellationToken.cancel();
                        connectCalls();
                    } else if (message.data === "denied") {
                        cancellationToken.cancel();
                        disconnectionExpected = true;
                        recursivePlayback(TURN_ON_MIC_IN_SETTINGS_MESSAGE, outCall);
                    }
                }
            });
        } else {
            connectCalls();
        }
    });

    outCall.addEventListener(CallEvents.Failed, function (e) {
        addEventAndSend(EVENT_TYPE_TO_ID.OUT_APP_CALL_FAILED_EVENT, e.code);
        if (fallbackStarted)
            return;
        if (e.code === 486 || e.code === 603) {
            const isBusy = e.headers["X-endReason"] === "busyCallee" || e.code === 486;
            const reason = isBusy ? "busyCallee" : "rejected";
            inCall.sendMessage(buildCallFailedMessage(reason));
            inCall.hangup({[`X-callFailed`]: reason});
            log("Scenario was terminated due to [" + reason + "]")
            terminateScenario();
            return;
        }
        fallbackStarted = true;
        clearTimeout(timer);
        callNumber()
    });

    inCall.addEventListener(CallEvents.MessageReceived, event => handleInCallMessage(event));
    outCall.addEventListener(CallEvents.MessageReceived, event => handleOutCallMessage(event));

    addReinviteListeners(outCall, inCall);

    timer = setTimeout(function () {
        addEventAndSend(EVENT_TYPE_TO_ID.OUT_APP_CALL_TIMEOUT_EVENT);
        if (fallbackStarted)
            return;
        fallbackStarted = true;
        outCall.hangup();
        callNumber()
    }, CALL_TIMEOUT);
}

function recursivePlayback(text, call) {
    log("Start play phrase=" + text);
    let isCancelled = false;
    let player;

    function play() {
        if (isCancelled) {
            return;
        }

        player = VoxEngine.createTTSPlayer(text, Language.RU_RUSSIAN_FEMALE);

        player.addEventListener(PlayerEvents.PlaybackFinished, () => {
            setTimeout(play, 3000);
        });
        player.sendMediaTo(call);
    }

    play();

    return {
        cancel: () => {
            log("Cancel playing phrase");
            isCancelled = true;
            player.stop();
        }
    }
}

function callNumber() {
    if (!customData.targetPhone) {
        log("Not call to target, because is undefined")
        return;
    }

    customData.callerId = DEFAULT_CALLER_ID;
    log("Call to target: target=[" + customData.targetPhone + "], caller-id=[" + customData.callerId + "]")
    addEventAndSend(EVENT_TYPE_TO_ID.OUT_PHONE_CALL_INIT_EVENT);
    outCall = VoxEngine.callPSTN(customData.targetPhone, customData.callerId);

    outCall.addEventListener(CallEvents.Disconnected, () => {
        addEventAndSend(EVENT_TYPE_TO_ID.OUT_PHONE_CALL_DISCONNECTED_EVENT);
        log("Scenario was terminated due to disconnected target")
        terminateScenario();
    });

    outCall.addEventListener(CallEvents.Failed, function (e) {
        if (e.code === 408) {
            inCall.sendMessage(buildCallFailedMessage("noAnswer"));
            inCall.hangup({[`X-callFailed`]: "noAnswer"});
        } else {
            inCall.sendMessage(buildCallFailedMessage("unknown"));
        }

        addEventAndSend(EVENT_TYPE_TO_ID.OUT_PHONE_CALL_FAILED_EVENT, e.code);
        log("Scenario was terminated due to failed phone call")
        terminateScenario();
    });

    outCall.addEventListener(CallEvents.Connected, () => {
        inCall.sendMessage(buildConnectedMessage());
        startTalkTime = Date.now();
        addEventAndSend(EVENT_TYPE_TO_ID.OUT_PHONE_CALL_CONNECTED_EVENT);
        VoxEngine.sendMediaBetween(inCall, outCall);
    });
}

VoxEngine.addEventListener(AppEvents.Started, (e) => {
    sessionId = e.sessionId.toString();
    log("Got Started event, sessionId=" + sessionId);
});

function buildDisplayName() {
    let displayName = headers["X-alias_and_subject"];

    // todo: remove this block after AUTORUBACK-1695 implemented
    if (!displayName) {
        displayName = headers["X-alias"] ?? customData.sourceUsername;
        const subject = ["offer_mark", "offer_model", "offer_generation"]
            .map(name => headers["X-" + name])
            .filter(v => (v ?? "") != "")
            .join(" ");

        if (subject) {
            displayName += ` • ${subject}`;
        }
    }

    return displayName;
}

VoxEngine.addEventListener(AppEvents.CallAlerting, function (e) {
    log("Received headers " + JSON.stringify(e.headers));
    customData.redirectId = e.headers["X-redirect_id"];
    customData.sourceUsername = e.callerid;

    log("Get redirect-id: " + customData.redirectId);
    headers = filterHeaders(e.headers);
    log("Filtered headers " + JSON.stringify(headers));
    let displayName = buildDisplayName();

    customData.targetUsername = e.destination

    inCall = e.call;

    addEventAndSend(EVENT_TYPE_TO_ID.START_APP_SCENARIO_EVENT);
    inCall.addEventListener(CallEvents.RecordStarted, (e) => {
        customData.recordUrl = e.url;
        log("Got RecordStarted event, recordUrl=" + customData.recordUrl);
    });
    inCall.addEventListener(CallEvents.RecordStopped, () => {
        log("Got RecordStopped event.");
    });

    inCall.addEventListener(CallEvents.Disconnected, () => {
        addEventAndSend(EVENT_TYPE_TO_ID.IN_CALL_DISCONNECTED_EVENT);
        log("Scenario was terminated due to disconnected source");
        terminateScenario();
    });

    initForwardCall(e.callerid, displayName, e.scheme);
});

function addReinviteListeners(call1, call2) {
    // copied from https://github.com/voximplant/easyprocess/blob/9e66c44b0f097035aeed7fa9871ed89512bf8ac7/easyprocess.js#L55-L77
    call2.addEventListener(CallEvents.ReInviteReceived, function (e) {
        if (call1)
            call1.reInvite(e.headers, e.mimeType, e.body);
    });
    call1.addEventListener(CallEvents.ReInviteReceived, function (e) {
        if (call2)
            call2.reInvite(e.headers, e.mimeType, e.body);
    });

    call2.addEventListener(CallEvents.ReInviteAccepted, function (e) {
        if (call1)
            call1.acceptReInvite(e.headers, e.mimeType, e.body);
    });
    call1.addEventListener(CallEvents.ReInviteAccepted, function (e) {
        if (call2)
            call2.acceptReInvite(e.headers, e.mimeType, e.body);
    });
    call2.addEventListener(CallEvents.ReInviteRejected, function (e) {
        call1.rejectReInvite(e.headers);
    });
    call1.addEventListener(CallEvents.ReInviteRejected, function (e) {
        call2.rejectReInvite(e.headers);
    });
}

function addEventAndSend(eventType, code = undefined) {
    const eventForCustomData = {eventType: eventType, code: code, time: Date.now()}
    const event = {eventType: eventType, code: code, time: new Date().toISOString()}
    customData.events.push(eventForCustomData);
    setVoxEngineCustomData();
    sendEvent(event);
}

function sendEvent(event) {
}

function terminateScenario(shouldPost = true) {
    if (startTalkTime) {
        endTalkTime = Date.now();
        customData.talkDuration = startTalkTime && endTalkTime ? endTalkTime - startTalkTime : 0;
    }
    setVoxEngineCustomData();
    if (shouldPost) {
        addEventAndSend(EVENT_TYPE_TO_ID.APP_SCENARIO_TERMINATED_EVENT);
    }
    log("Scenario was terminated")
    VoxEngine.terminate();
}

function setVoxEngineCustomData() {
    log('Current CustomData: { ' + Object.entries(customData).map(e => `${e[0]}: ${e[1]} `) + '}');
    let {recordUrl, ...minimized} = customData;
    const jsonCustomData = JSON.stringify(minimized);
    VoxEngine.customData(jsonCustomData);
}

function log(message) {
    Logger.write("[Telepony] " + message);
}