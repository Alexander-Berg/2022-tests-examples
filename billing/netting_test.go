package accrualer

import (
	"context"
	"embed"
	"encoding/json"
	"path"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	aconf "a.yandex-team.ru/billing/hot/accrualer/internal/config"
	"a.yandex-team.ru/billing/hot/accrualer/internal/entities"
	"a.yandex-team.ru/billing/hot/accrualer/internal/mocks"
	"a.yandex-team.ru/billing/hot/accrualer/internal/providers"
	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue"
)

const (
	namespace = "bnpl"
)

type YtRows []entities.RawRow
type DataRows []entities.YTDataModel

func parseYTData(t *testing.T, fs embed.FS, filename string) DataRows {
	var rawRows YtRows
	rawData, err := fs.ReadFile(filename)
	require.NoErrorf(t, err, "failed read test data file: %s", filename)
	require.NoErrorf(t, json.Unmarshal(rawData, &rawRows), "failed parse test data file: %s", filename)

	result := make(DataRows, len(rawRows))
	for idx, row := range rawRows {
		data, _, err := entities.NewYTData(row)
		require.NoError(t, err, "failed prepare new yt data, pos %v, file %s", idx, filename)
		result[idx] = *data
	}
	return result
}

// TestProcessNetting checks minimal work for accruals from marked events
func (s *AccrualerTestSuite) TestProcessNetting() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	yp := mocks.YTProcessorMock{BatchData: [][]entities.YTDataModel{parseYTData(s.T(), nettingFS, path.Join(nettingPath, "yt_data_netting_event.json"))}}
	p := server.processors[namespace]
	p.yp = &yp
	server.processors[namespace] = p

	config := s.logicConf.Namespace[namespace].Accruals.Types[aconf.AgencyDetailed].Topics[providers.DefaultName]
	lb := server.writers.Writers[config].(*mocks.MockLogBrokerProducer)

	nettingConf := s.config.NettingProv
	nettingLB := server.writers.Writers[nettingConf].(*mocks.MockLogBrokerProducer)

	noNetting := len(nettingLB.Messages)
	before := len(lb.Messages)
	sendEventFile(s.T(), server, nettingFS, path.Join(nettingPath, "process_netting_event.json"), server.handleNettingMessageBatch)
	require.Len(s.T(), nettingLB.Messages, noNetting, "correct event with marked events")
	require.Len(s.T(), lb.Messages, before+2, "accrual")

	data, err := nettingFS.ReadFile(path.Join(nettingPath, "result_netting_event.json"))
	require.NoError(s.T(), err, "failed test file: result_netting_event")
	var result []entities.Accrual
	require.NoError(s.T(), json.Unmarshal(data, &result), "failed test file: result_netting_event")

	var accrual entities.Accrual
	require.NoError(s.T(), json.Unmarshal(lb.Messages[before], &accrual))
	checkResultAccrual(s.T(), result[0], accrual, false, "result_netting_event")

	require.NoError(s.T(), json.Unmarshal(lb.Messages[before+1], &accrual))
	checkResultAccrual(s.T(), result[1], accrual, false, "result_netting_event")
}

func (s *AccrualerTestSuite) TestProcessNettingSplit() {
	server := s.newServer()
	require.NoError(s.T(), mockProcProviders(server))

	wasNetting := map[bool]bool{}
	yp := mocks.YTProcessorMock{BatchFunc: func(netting []entities.Netting, fields ...aconf.EventFieldConfig) ([][]entities.YTDataModel, error) {
		require.NotEmpty(s.T(), netting)
		wasNetting[netting[0].Attrs.DryRun] = true
		return make([][]entities.YTDataModel, len(netting)), nil
	}}
	p := server.processors[namespace]
	p.yp = &yp
	server.processors[namespace] = p

	rawData, err := nettingFS.ReadFile(path.Join(nettingPath, "multiple_netting_events.json"))
	require.NoError(s.T(), err, "failed test file: multiple_netting_events.json")
	var data []entities.Netting
	require.NoError(s.T(), json.Unmarshal(rawData, &data), "failed parse file: multiple_netting_events.json")
	b := persqueue.MessageBatch{
		Topic:     "test_unique",
		Partition: uint32(1),
		Messages:  []persqueue.ReadMessage{},
	}
	for _, event := range data {
		bytes, err := json.Marshal(event)
		require.NoError(s.T(), err)
		b.Messages = append(b.Messages, persqueue.ReadMessage{
			Data: bytes,
		})
	}
	err = server.handleNettingMessageBatch(context.Background(), persqueue.ReadMessage{}, b)
	require.NoError(s.T(), err)

	assert.True(s.T(), wasNetting[true])
	assert.True(s.T(), wasNetting[false])
}
