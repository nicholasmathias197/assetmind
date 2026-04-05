# AssetMind — Before First Deploy

## 1. Create the S3 bucket

```bash
aws s3 mb s3://assetmind-frontend-911784620581 --region us-east-2
```

Or let Terraform create it on the first `terraform apply`.

## 2. Create an EC2 key pair

```bash
aws ec2 create-key-pair --key-name assetmind-key --region us-east-2 --query "KeyMaterial" --output text > assetmind-key.pem
chmod 400 assetmind-key.pem
```

## 3. Replace `sg-PLACEHOLDER` in buildspec files

After the first `terraform apply`, get the security group ID:

```bash
aws ec2 describe-security-groups --filters "Name=group-name,Values=assetmind-sg" --query "SecurityGroups[0].GroupId" --output text --region us-east-2
```

Replace `sg-PLACEHOLDER` in both `buildspec.yml` and `buildspec-deploy.yml` with the actual ID.

## 4. Create CodeBuild projects

Create two CodeBuild projects in the AWS Console (us-east-2):

| Project Name | Buildspec | Purpose |
|---|---|---|
| `AssetMind_Build` | `buildspec.yml` | Main build + deploy |
| `AssetMind_SonarCloud_Analysis` | `buildspec-sonar.yml` | SonarCloud code analysis |

Both should use:
- **Source**: GitHub (CodeConnections) → `nicholasmathias197/assetmind`, branch `main`
- **Environment**: Managed image, Amazon Linux 2023, Standard runtime
- **Service role**: Must have permissions for S3, EC2, SSM, SNS, CloudWatch, Terraform state

## 5. Create the CodePipeline

```bash
aws codepipeline create-pipeline --cli-input-json file://pipeline.json --region us-east-2
```

## 6. Store the SonarCloud token (if using SonarCloud)

```bash
aws ssm put-parameter --name "/sonarcloud/token" --value "YOUR_SONAR_TOKEN" --type SecureString --region us-east-2
```
