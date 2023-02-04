package main

import (
	"context"
	"errors"
	"time"

	"github.com/YandexClassifieds/go-common/retry"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/logger"
	consul_kv "github.com/YandexClassifieds/shiva/pkg/consul/kv"
	"github.com/YandexClassifieds/shiva/pkg/i/kv"
	"github.com/YandexClassifieds/shiva/test"

	"github.com/YandexClassifieds/shiva/common/config"
)

type CIHelper struct {
	kv  kv.KV
	log logger.Logger
}

func NewCIHelper(log logger.Logger) *CIHelper {
	return &CIHelper{
		log: log.NewContext("", "ci", logger.External),
		kv:  consul_kv.NewEphemeralKV(log, consul_kv.NewConf(test.CINamespace), &test.TestInfo{}),
	}
}

// SaveTestInfo - save test info with one retry
func (ci *CIHelper) SaveTestInfo(testInfo *test.TestInfo) {
	if err := ci.kv.Save(test.TestInfoKey, testInfo); err != nil {
		ci.log.WithError(err).Warn("Fail save test info")
		if err := ci.kv.Save(test.TestInfoKey, testInfo); err != nil {
			ci.log.WithError(err).Fatalf("Fail save test info")
		}
	}
}

// GetTestInfo - load test info with retry
func (ci *CIHelper) GetTestInfo() *test.TestInfo {

	var result *test.TestInfo
	err := retry.UntilSuccess(context.Background(), func(ctx context.Context) (retry bool, err error) {
		value, err := ci.kv.Get(test.TestInfoKey)
		switch {
		case errors.Is(err, common.ErrNotFound):
			result = &test.TestInfo{}
			return false, nil
		case err != nil:
			return true, err
		default:
			testInfo, ok := value.(*test.TestInfo)
			if !ok {
				ci.log.WithError(err).Fatal("Fail get test info. No testInfo type")
			}
			result = testInfo
			return false, nil
		}
	}, retry.BackoffLinearWithJitter(time.Second, 0.1))
	if err != nil {
		ci.log.WithError(err).Fatal("Fail get test info")
	}
	return result
}

func (ci *CIHelper) OOMEnable() bool {
	return ci.GetTestInfo().OOM
}

func (ci *CIHelper) rescheduleOnDemand() {
	if stableOnAttempt := config.SafeInt("STABLE_ON_ATTEMPT"); stableOnAttempt != 0 {
		testInfo := ci.GetTestInfo()
		if len(testInfo.Hosts) < stableOnAttempt {
			currentHost := config.Str("_DEPLOY_HOSTNAME")
			currentHostExist := false
			for _, host := range testInfo.Hosts {
				if host == currentHost {
					currentHostExist = true
					break
				}
			}
			if !currentHostExist {
				testInfo.Hosts = append(testInfo.Hosts, currentHost)
				ci.SaveTestInfo(testInfo)
			}
			makeOOM()
		}
	} else if numStableInstances := config.SafeInt("NUM_STABLE_INSTANCES"); numStableInstances != 0 {
		testInfo := ci.GetTestInfo()

		if len(testInfo.Hosts) == numStableInstances {
			makeOOM()
		}

		currentHost := config.Str("_DEPLOY_HOSTNAME")
		testInfo.Hosts = append(testInfo.Hosts, currentHost)
		ci.SaveTestInfo(testInfo)
	}
}
