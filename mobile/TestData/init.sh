cd $(dirname "$0")
rm -rf rep_1
git clone http://localhost:7990/bitbucket/scm/project_1/rep_1.git
cd rep_1
git checkout -b test-config-1
echo "- add_file/ci.yaml" >> ci.yaml
echo "- test_ui/ci.yaml" >> ci.yaml
cp ../../common/src/test/resources/ru/yandex/bitbucket/plugin/configprocessor/configProcessingTest_1.yaml add_file/ci.yaml
mkdir test_ui
cp ../ci.yaml test_ui/ci.yaml
git add .
git commit -am "test config"
git push --set-upstream origin test-config-1
open "http://localhost:7990/bitbucket/projects/PROJECT_1/repos/rep_1/compare/commits?sourceBranch=refs/heads/test-config-1"
