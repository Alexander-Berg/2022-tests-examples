package branch

import (
	"errors"
	"strconv"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/cleaner/branch/store/branch"
	"github.com/YandexClassifieds/shiva/cmd/cleaner/branch/store/issue"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/pb/shiva/events/event2"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/manifest"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pkg/mq"
	"github.com/YandexClassifieds/shiva/pkg/mq/conf"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/types/known/timestamppb"
	"gorm.io/gorm"
)

var (
	name                           = "test_service"
	version                        = "0.0.11"
	dlayer                         = common.Test
	manifestProductionMirroringOn  = &manifest.Manifest{ProductionMirroring: true}
	manifestProductionMirroringOff = &manifest.Manifest{ProductionMirroring: false}
)

func TestService_Add(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	require.NoError(t, db.GormDb.AutoMigrate(branch.Model{}))

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()
	rec := branch.Model{
		Layer:             common.Test,
		Name:              "svc1",
		Branch:            "b1",
		DeploymentEndTime: time.Now(),
	}
	err := service.Add(rec, []*issue.Model{})
	require.NoError(t, err)
	resultModel := branch.Model{}
	err = db.GormDb.Find(&resultModel, branch.Model{Layer: common.Test, Name: "svc1", Branch: "b1"}).Error
	assert.False(t, errors.Is(err, gorm.ErrRecordNotFound))
	assert.Equal(t, resultModel.DeploymentEndTime.Unix(), rec.DeploymentEndTime.Unix())
}

func TestService_Remove(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()
	db.GormDb.Create(&branch.Model{Layer: common.Test, Name: "svc1", Branch: "b1", DeploymentEndTime: time.Now()})

	err := service.Remove(branch.Model{
		Layer:             common.Test,
		Name:              "svc1",
		Branch:            "b1",
		DeploymentEndTime: time.Now(),
	})
	require.NoError(t, err)
}

func TestService_HandleEvent(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	producer := mq.NewProducer(conf.NewProducerConf(makeTopic(t)), test.NewLogger(t))

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()

	testCases := []struct {
		name  string
		layer common.Layer
		dType common.Type
	}{
		{
			name:  "TestLayer_Run",
			layer: common.Test,
			dType: common.Run,
		},
		{
			name:  "ProdLayer_Run",
			layer: common.Prod,
			dType: common.Run,
		},
		{
			name:  "TestLayer_Update",
			layer: common.Test,
			dType: common.Update,
		},
		{
			name:  "ProdLayer_Update",
			layer: common.Prod,
			dType: common.Update,
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			b := t.Name()

			sendEvent(t, producer, b, tc.layer, tc.dType, time.Now(), time.Now().Add(time.Minute), manifestProductionMirroringOff, []string{})
			test.Wait(t, func() error {
				return waitProcessedState(service, name, b, tc.layer, branch.Started)
			})

			sendEvent(t, producer, b, tc.layer, common.Stop, time.Now().Add(time.Minute), time.Now().Add(2*time.Minute), manifestProductionMirroringOff, []string{})
			test.Wait(t, func() error {
				return waitProcessedState(service, name, b, tc.layer, branch.Finished)
			})
		})
	}
}

// TODO change sleep to wait
func TestDuplicateEvent(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)
	b := t.Name()

	producer := mq.NewProducer(conf.NewProducerConf(makeTopic(t)), test.NewLogger(t))

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()

	startTime := time.Unix(0, time.Now().UnixNano()/1000*1000)

	sendEvent(t, producer, b, dlayer, common.Run, startTime, startTime.Add(time.Minute), manifestProductionMirroringOff, []string{})
	test.Wait(t, func() error {
		return waitProcessedState(service, name, b, dlayer, branch.Started)
	})

	rec1, err := service.branchStorage.Get(dlayer, name, b)
	require.NoError(t, err)

	sendEvent(t, producer, b, dlayer, common.Run, startTime, startTime.Add(time.Minute), manifestProductionMirroringOff, []string{})
	time.Sleep(time.Second)

	rec2, err := service.branchStorage.Get(dlayer, name, b)
	require.NoError(t, err)

	assert.Equal(t, rec1.UpdatedAt, rec2.UpdatedAt)

	sendEvent(t, producer, b, dlayer, common.Stop, startTime.Add(time.Minute), startTime.Add(2*time.Minute), manifestProductionMirroringOff, []string{})
	test.Wait(t, func() error {
		return waitProcessedState(service, name, b, dlayer, branch.Finished)
	})

	rec1, err = service.branchStorage.Get(dlayer, name, b)
	require.NoError(t, err)

	sendEvent(t, producer, b, dlayer, common.Stop, startTime.Add(time.Minute), startTime.Add(2*time.Minute), manifestProductionMirroringOff, []string{})
	time.Sleep(time.Second)

	rec2, err = service.branchStorage.Get(dlayer, name, b)
	require.NoError(t, err)

	assert.Equal(t, rec1.UpdatedAt, rec2.UpdatedAt)
}

func TestInverseOrder(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	layer := common.Test
	b := "test_branch"

	producer := mq.NewProducer(conf.NewProducerConf(makeTopic(t)), test.NewLogger(t))

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()

	startTime := time.Now()

	sendEvent(t, producer, b, layer, common.Stop, startTime.Add(time.Minute), time.Now().Add(2*time.Minute), manifestProductionMirroringOff, []string{})
	test.Wait(t, func() error {
		return waitProcessedState(service, name, b, layer, branch.Finished)
	})

	sendEvent(t, producer, b, layer, common.Run, startTime, startTime.Add(time.Minute), manifestProductionMirroringOff, []string{})
	test.Wait(t, func() error {
		return waitProcessedState(service, name, b, layer, branch.Finished)
	})
}

func TestNotBranchDeployment(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	layer := common.Test
	b := ""

	producer := mq.NewProducer(conf.NewProducerConf(makeTopic(t)), test.NewLogger(t))

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()

	startTime := time.Now()

	sendEvent(t, producer, b, layer, common.Stop, startTime.Add(time.Minute), time.Now().Add(2*time.Minute), manifestProductionMirroringOff, []string{})
	time.Sleep(1 * time.Second)
	_, err := service.branchStorage.Get(layer, name, b)
	assert.True(t, errors.Is(err, common.ErrNotFound))

	sendEvent(t, producer, b, layer, common.Run, startTime, startTime.Add(time.Minute), manifestProductionMirroringOff, []string{})
	time.Sleep(1 * time.Second)
	_, err = service.branchStorage.Get(layer, name, b)
	assert.True(t, errors.Is(err, common.ErrNotFound))
}

func TestProductionBranchMirroringOn(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	layer := common.Test
	b := config.ProductionMirroring

	producer := mq.NewProducer(conf.NewProducerConf(makeTopic(t)), test.NewLogger(t))

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()

	startTime := time.Now()

	sendEvent(t, producer, b, layer, common.Stop, startTime.Add(time.Minute), time.Now().Add(2*time.Minute), manifestProductionMirroringOn, []string{})
	time.Sleep(1 * time.Second)
	_, err := service.branchStorage.Get(layer, name, b)
	assert.True(t, errors.Is(err, common.ErrNotFound))

	sendEvent(t, producer, b, layer, common.Run, startTime, startTime.Add(time.Minute), manifestProductionMirroringOn, []string{})
	time.Sleep(1 * time.Second)
	_, err = service.branchStorage.Get(layer, name, b)
	assert.True(t, errors.Is(err, common.ErrNotFound))
}

func TestProductionBranchMirroringOff(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	b := config.ProductionMirroring

	producer := mq.NewProducer(conf.NewProducerConf(makeTopic(t)), test.NewLogger(t))

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()

	sendEvent(t, producer, b, dlayer, common.Run, time.Now(), time.Now().Add(time.Minute), manifestProductionMirroringOff, []string{})
	test.Wait(t, func() error {
		return waitProcessedState(service, name, b, dlayer, branch.Started)
	})

	sendEvent(t, producer, b, dlayer, common.Stop, time.Now().Add(time.Minute), time.Now().Add(2*time.Minute), manifestProductionMirroringOff, []string{})
	test.Wait(t, func() error {
		return waitProcessedState(service, name, b, dlayer, branch.Finished)
	})
}

func TestDefaultTTLWithoutIssues(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	b := t.Name()

	producer := mq.NewProducer(conf.NewProducerConf(makeTopic(t)), test.NewLogger(t))

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()

	startTime := time.Now()
	endTime := startTime.Add(time.Minute)
	sendEvent(t, producer, b, dlayer, common.Run, startTime, endTime, manifestProductionMirroringOff, []string{})
	test.Wait(t, func() error {
		return waitProcessedState(service, name, b, dlayer, branch.Started)
	})
	res, err := service.branchStorage.Get(dlayer, name, b)
	assert.NoError(t, err)
	assert.Equal(t, endTime.Add(service.conf.Lifetime).Unix(), res.Expires.Time.Unix())
}

func TestEmptyTTLWithIssues(t *testing.T) {
	test.InitTestEnv()

	db := test_db.NewSeparatedDb(t)

	b := t.Name()

	producer := mq.NewProducer(conf.NewProducerConf(makeTopic(t)), test.NewLogger(t))

	service := RunService(db, test.NewLogger(t), newConf(t), nil)
	defer service.Close()

	startTime := time.Now()
	endTime := startTime.Add(time.Minute)
	sendEvent(t, producer, b, dlayer, common.Run, startTime, endTime, manifestProductionMirroringOff, []string{"VOID-1"})
	test.Wait(t, func() error {
		return waitProcessedState(service, name, b, dlayer, branch.Started)
	})
	res, err := service.branchStorage.Get(dlayer, name, b)
	assert.NoError(t, err)
	assert.Equal(t, false, res.Expires.Valid)
}

func makeTopic(t *testing.T) string {
	return "branch_test_" + t.Name()
}

func newConf(t *testing.T) Conf {
	c := NewConf()
	c.DeployTopic = makeTopic(t)
	return c
}

func makeEvent(branch string, layer common.Layer, dtype common.Type, startTime, endTime time.Time, manifest *manifest.Manifest, issues []string) *event2.Event {
	e := &event2.Event{
		Deployment: &deployment.Deployment{
			Id:          strconv.FormatInt(1, 10),
			ServiceName: name,
			Version:     version,
			Branch:      branch,
			Start:       timestamppb.New(startTime),
			End:         timestamppb.New(endTime),
			State:       state.DeploymentState_SUCCESS,
			Issues:      issues,
		},
		Manifest: manifest,
	}

	e.Deployment.SetCommonLayer(layer)
	e.Deployment.SetCommonType(dtype)

	return e
}

func sendEvent(t *testing.T, producer *mq.Producer, branch string, layer common.Layer, dtype common.Type, startTs, finishTs time.Time, manifest *manifest.Manifest, issues []string) {
	e := makeEvent(branch, layer, dtype, startTs, finishTs, manifest, issues)
	b, err := proto.Marshal(e)
	require.NoError(t, err)
	message := mq.NewMessage(name, b, nil)
	require.NoError(t, producer.Push(message))
}

func waitProcessedState(service *Service, name, branch string, layer common.Layer, state branch.State) error {
	res, err := service.branchStorage.Get(layer, name, branch)
	switch {
	case err != nil:
		return err
	case res.State == state:
		return nil
	default:
		return errors.New("branch deployment is not processed")
	}
}
