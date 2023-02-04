ENV(AWS_ACCESS_KEY_ID="sqs_configshop_user")
ENV(AWS_SECRET_ACCESS_KEY="unused")
ENV(AWS_SESSION_TOKEN="")

USE_RECIPE(kikimr/public/tools/sqs_recipe/sqs_recipe --sqs-user sqs_configshop_user --use-packages kikimr/public/tools/package/stable)

DEPENDS(
    billing/configshop/tests/recipes/sqs_prepare
    kikimr/public/tools/sqs_recipe
    kikimr/public/tools/package/stable
)
