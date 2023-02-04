package accrualer

import (
	"embed"
	"encoding/json"
	"path"
	"strings"
	"testing"

	"github.com/stretchr/testify/require"
	"github.com/valyala/fastjson"

	aconf "a.yandex-team.ru/billing/hot/accrualer/internal/config"
	"a.yandex-team.ru/billing/hot/accrualer/internal/entities"
	"a.yandex-team.ru/billing/hot/accrualer/internal/mocks"
	"a.yandex-team.ru/billing/hot/accrualer/internal/providers"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
)

//go:embed gotest/errors/*
var errorsFS embed.FS

const (
	errorsPath = "gotest/errors"

	inputYTPrefix  = "yt_input_"
	resultYTPrefix = "yt_result_"
	dataYTPrefix   = "yt_data_"
)

func processEventError(t *testing.T, server *Server, lb *mocks.MockLogBrokerProducer,
	fs embed.FS, filename string, netting bool) entities.NotifierMessage {
	before := len(lb.Messages)
	config := server.config.Notifier
	notifier := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)
	beforeNotifier := len(notifier.Messages)

	if netting {
		sendEventFile(t, server, fs, filename, server.handleNettingMessageBatch)
	} else {
		sendEventFile(t, server, fs, filename, server.handleMessageBatch)
	}
	// check that message to main lb topic was not sent
	require.Len(t, lb.Messages, before, "failed test file: "+filename)

	// check that one message was sent to errors topic
	require.Len(t, notifier.Messages, beforeNotifier+1, "failed test file: "+filename)

	msg := notifier.Messages[len(notifier.Messages)-1]
	var actual entities.NotifierMessage
	require.NoError(t, json.Unmarshal(msg, &actual))
	return actual
}

// TestSendErrors checks that correct errors are being sent to errors topic
func (s *AccrualerTestSuite) TestSendErrors() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	yp := mocks.YTProcessorMock{BatchFunc: func(netting []entities.Netting, fields ...aconf.EventFieldConfig) ([][]entities.YTDataModel, error) {
		require.Fail(s.T(), "why i am here?")
		return [][]entities.YTDataModel{{}}, nil
	}}

	p := server.processors[entities.DefaultNamespace]
	p.yp = &yp
	server.processors[entities.DefaultNamespace] = p

	config := s.logicConf.Namespace[entities.DefaultNamespace].Accruals.Types[aconf.AgencyFull].Topics[providers.DefaultName]
	lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

	dirs, err := errorsFS.ReadDir(errorsPath)
	require.NoError(s.T(), err)
	testCount := 0
	for _, data := range dirs {
		if !strings.HasPrefix(data.Name(), inputPrefix) {
			continue
		}
		testCount += 1
		xlog.Info(*s.ctx, "test file", log.String("name", data.Name()))
		testName := data.Name()[len(inputPrefix):len(data.Name())]
		var actual entities.NotifierMessage

		if strings.HasPrefix(data.Name(), "input_netting") {
			actual = processEventError(s.T(), server, lb, errorsFS, path.Join(errorsPath, data.Name()), true)
		} else {
			actual = processEventError(s.T(), server, lb, errorsFS, path.Join(errorsPath, data.Name()), false)
		}

		resultData, err := errorsFS.ReadFile(path.Join(errorsPath, resultPrefix+testName))
		require.NoError(s.T(), err, "failed test file: "+testName)

		var expected entities.NotifierMessage
		require.NoError(s.T(), json.Unmarshal(resultData, &expected), "failed test file: "+testName)
		require.Equal(s.T(), expected, actual, "failed test file: "+testName)
	}
	// check that we ran at least one test
	require.NotEqual(s.T(), 0, testCount)
}

// YTDataTest is used for mocking YT responses
type YtDataTest struct {
	OriginalData string `json:"original_data_raw"`
	Batch        any    `json:"batch"`
}

// TestSendErrorsProcessYT checks that correct errors are being sent to errors topic
// Tests here have additional input file with data mocking YT responses
func (s *AccrualerTestSuite) TestSendErrorsProcessYT() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	YTDataModel := entities.YTDataModel{
		OriginalData: &fastjson.Value{},
	}

	var YTDataModels [][]entities.YTDataModel
	YTDataModels = append(YTDataModels, []entities.YTDataModel{YTDataModel})

	yp := mocks.YTProcessorMock{
		BatchData: YTDataModels,
	}

	p := server.processors[entities.DefaultNamespace]
	p.yp = &yp
	server.processors[entities.DefaultNamespace] = p

	config := s.logicConf.Namespace[entities.DefaultNamespace].Accruals.Types[aconf.AgencyFull].Topics[providers.DefaultName]
	lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

	dirs, err := errorsFS.ReadDir(errorsPath)
	require.NoError(s.T(), err)
	testCount := 0
	for _, inputData := range dirs {
		if !strings.HasPrefix(inputData.Name(), inputYTPrefix) {
			continue
		}
		testCount += 1
		xlog.Info(*s.ctx, "test file", log.String("name", inputData.Name()))
		testName := inputData.Name()[len(inputYTPrefix):len(inputData.Name())]

		data, err := errorsFS.ReadFile(path.Join(errorsPath, dataYTPrefix+testName))
		require.NoError(s.T(), err, "failed test file: "+testName)

		// mocking YT responses
		var ytDataTest YtDataTest
		require.NoError(s.T(), json.Unmarshal(data, &ytDataTest), "failed test file: "+testName)
		batchBytes, err := json.Marshal(ytDataTest.Batch)
		require.NoError(s.T(), err)
		origData, err := fastjson.ParseBytes(batchBytes)
		require.NoError(s.T(), err)
		server.processors[entities.DefaultNamespace].yp.(*mocks.YTProcessorMock).BatchData[0][0].OriginalData = origData
		server.processors[entities.DefaultNamespace].yp.(*mocks.YTProcessorMock).BatchData[0][0].OriginalDataRaw = []byte(ytDataTest.OriginalData)

		actual := processEventError(s.T(), server, lb, errorsFS, path.Join(errorsPath, inputData.Name()), true)

		resultData, err := errorsFS.ReadFile(path.Join(errorsPath, resultYTPrefix+testName))
		require.NoError(s.T(), err, "failed test file: "+testName)

		var expected entities.NotifierMessage
		require.NoError(s.T(), json.Unmarshal(resultData, &expected), "failed test file: "+testName)
		require.Equal(s.T(), expected, actual, "failed test file: "+testName)
	}
	// check that we ran at least one test
	//require.NotEqual(s.T(), 0, testCount)
}
