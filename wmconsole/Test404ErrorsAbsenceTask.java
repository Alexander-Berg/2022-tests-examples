package ru.yandex.webmaster3.worker.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.webmaster3.core.checklist.data.SiteProblemContent;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemState;
import ru.yandex.webmaster3.core.checklist.data.SiteProblemTypeEnum;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.worker.task.TaskResult;
import ru.yandex.webmaster3.core.worker.task.Test404ErrorsAbsenceTaskData;
import ru.yandex.webmaster3.core.zora.GoZoraService;
import ru.yandex.webmaster3.core.zora.go_data.request.GoZoraRequest;
import ru.yandex.webmaster3.storage.checklist.data.ProblemSignal;
import ru.yandex.webmaster3.storage.checklist.service.SiteProblemsService;
import ru.yandex.webmaster3.worker.RpsLimitedTask;
import ru.yandex.webmaster3.worker.Task;
import ru.yandex.wmtools.common.util.http.YandexHttpStatus;

/**
 * @author tsyplyaev
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class Test404ErrorsAbsenceTask extends RpsLimitedTask<Test404ErrorsAbsenceTaskData> {
    private static final int CHECK_ROUNDS = 2;
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 30;

    private final GoZoraService goZoraService;
    private final SiteProblemsService siteProblemsService;

    public void init() {
        setTargetRps(5.0f);
    }

    @Override
    public Task.Result run(Test404ErrorsAbsenceTaskData data) throws Exception {
        WebmasterHostId hostId = data.getHostId();

        ProblemSignal problem;
        if (checkIfProblemPresent(hostId)) {
            problem = new ProblemSignal(new SiteProblemContent.No404Errors(), DateTime.now());
        } else {
            problem = new ProblemSignal(SiteProblemTypeEnum.NO_404_ERRORS, SiteProblemState.ABSENT, DateTime.now());
        }
        // Явно запоминаем факт отсутствия проблемы, чтобы не перепроверять ее для этого сайта каждый день
        siteProblemsService.updateRealTimeProblem(hostId, problem, true, false);

        return new Task.Result(TaskResult.SUCCESS);
    }

    protected boolean checkIfProblemPresent(WebmasterHostId hostId) {
        boolean all3XX = true;
        for (int i = 0; i < CHECK_ROUNDS; i++) {
            String randStr = RandomStringUtils.randomAlphanumeric(MIN_LENGTH, MAX_LENGTH);
            int code = request404(IdUtils.hostIdToUrl(hostId) + "/" + randStr);
            if (code >= 400 && code < 500) {
                return false;
            }
            if (!(code >= 300 && code < 400)) {
                all3XX = false;
            }
            YandexHttpStatus httpStatus = YandexHttpStatus.parseCode(code);
            if (!YandexHttpStatus.isStandardHttpCode(httpStatus)) {
                return false;
            }
        }

        // В случае переезда сайта все урлы могут отдавать 3xx - в таком случае проблемы нет
        return !all3XX;
    }

    protected int request404(String url) {
        return goZoraService.executeRequest(GoZoraRequest.of(url)).getHttpCode();
    }

    @Override
    public Class<Test404ErrorsAbsenceTaskData> getDataClass() {
        return Test404ErrorsAbsenceTaskData.class;
    }

}
