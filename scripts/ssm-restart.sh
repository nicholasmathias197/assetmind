#!/bin/bash

echo "Step 1: looking up instance ID"
INSTANCE_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=assetmind-backend" "Name=instance-state-name,Values=running" \
  --query "Reservations[0].Instances[0].InstanceId" \
  --output text --region us-east-2)
echo "Instance ID: $INSTANCE_ID"

echo "Step 2: waiting for SSM"
for i in $(seq 1 20); do
  STATUS=$(aws ssm describe-instance-information \
    --filters "Key=InstanceIds,Values=$INSTANCE_ID" \
    --query "InstanceInformationList[0].PingStatus" \
    --output text --region us-east-2 2>/dev/null || echo "QUERY_FAILED")
  echo "SSM status $i/20: $STATUS"
  [ "$STATUS" = "Online" ] && break
  sleep 15
done

echo "Step 3: sending SSM command"
COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters 'commands=["aws s3 cp s3://assetmind-frontend-911784620581/app/assetmind-application.jar /opt/assetmind/assetmind-application.jar && systemctl restart assetmind"]' \
  --region us-east-2 \
  --query "Command.CommandId" --output text)
echo "Command ID: $COMMAND_ID"

echo "Step 4: polling for result"
for i in $(seq 1 20); do
  STATUS=$(aws ssm get-command-invocation \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID" \
    --region us-east-2 \
    --query "Status" --output text 2>/dev/null || echo "POLL_FAILED")
  echo "Command status $i/20: $STATUS"
  [ "$STATUS" = "Success" ] && echo "Done" && exit 0
  [ "$STATUS" = "Failed" ] && echo "Command failed" && exit 1
  sleep 10
done

echo "Timed out"
exit 0
