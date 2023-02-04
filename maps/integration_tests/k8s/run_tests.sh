#!/usr/bin/env sh
set -e

RUN_DESCR=$1
TEST_NAME=$2
if [ -z $TEST_NAME ];
then
  export TEST_ARGS="[\"-v\"]"
else
  export TEST_ARGS="[\"-v\", \"${TEST_NAME}\"]"
fi

if [ -z $RUN_DESCR ];
then
    echo "Usage:
  ./run_tests.sh <run_description> (test-name)

  Optional args:
    test-name: Only run such test file/test class/test name.
  
  Examples:
  ./run_tests.sh only-smoke-tests SmokeTest
  ./run_tests.sh one-test CalendarPlanningApikeyTest.test_invalid_apikey
  "
    exit 1
fi

# First switch kubectl to 'testing' cluster.
aws eks update-kubeconfig --name testing

# update secrets.yaml
update_secrets() {
  echo "Updating secrets"
  cd testing
  sh secrets-generator.sh
  kubectl apply -f secrets.yaml
  rm secrets.yaml
  cd -
}
export JOB_NAME=solver-integr-tests-$(arc describe --svn | head -c 8)-$RUN_DESCR

update_secrets
kubectl apply -f ./testing/config-map.yaml

# envsubst replaces envvars with values from env in job.yaml
cat testing/job.yaml | envsubst | kubectl apply -f -
echo "Started executing job: $JOB_NAME"
sleep 5
nohup sh -c "kubectl logs -f job/$JOB_NAME | gzip | aws s3 cp - s3://solver-tests-logs/$JOB_NAME.log.gz" > /dev/null &
echo "\n*****\nTo check logs either execute:
\`kubectl logs -f job/$JOB_NAME\`
or read from S3 bucket:
\`aws s3 cp s3://solver-tests-logs/$JOB_NAME.log.gz - | gunzip -c | less\`
*****\n"
