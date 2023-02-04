package bulk_deployment

import (
	"sync"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/deployment/status"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	dmock "github.com/YandexClassifieds/shiva/test/mock/deployment"
	mqMock "github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

func TestJobNotFound(t *testing.T) {
	test.RunUp(t)

	db := test_db.NewSeparatedDb(t)
	statusStorage := status.NewStorage(db, test.NewLogger(t))
	schedulerMock := mock.NewMockScheduler()
	pMock := mqMock.NewProducerMock()
	conf := NewConf()
	srv := newService(t, db, schedulerMock, dmock.NewProducerMock(), conf, pMock)
	srvBranch := ServiceBranch{Service: "svcName"}
	runService(t, db, srv, schedulerMock, statusStorage, srvBranch.Service, srvBranch.Branch, common.Test, sMapYml)

	b, err := srv.BulkDeployment(common.Test, common.Update, t.Name(), config.AdminSource, NewServiceBranchSet(srvBranch))
	require.NoError(t, err)

	jobChan := make(chan *Status, 1)

	worker := NewWorker(test.NewLogger(t), conf, srv, b, jobChan, &Lock{})
	var wg sync.WaitGroup
	wg.Add(1)
	go worker.Run(&wg)

	jobChan <- &Status{}
	close(jobChan)

	wg.Wait()
}
