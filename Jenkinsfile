#!groovy​

pipeline {
    agent { label 'master' }

    stages {
        stage('clean') {
            steps {
                sh 'git clean -fdx'
            }
        }
        stage('set_version') {
            steps {
                sh 'bumpversion patch'
            }
        }
        stage('release') {
            when { branch 'master' }
            steps {
                sh 'bumpversion --tag --commit --allow-dirty release'
            }
        }
        stage('package') {
            agent {
                dockerfile {
                    args '-v ${HOME}/.m2:/home/builder/.m2'
                    additionalBuildArgs '--build-arg BUILDER_UID=${JENKINS_UID:-9999}'
                    reuseNode true
                }
            }
            steps {
                sh 'mvn -B clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: '*/target/*.zip,wps-cloudformation-template.yaml,lambda/**', fingerprint: true, onlyIfSuccessful: true
                }
            }
        }

        stage('build_worker') {
            environment {
                AWS_REGION = "ap-southeast-2"
                AWS_ACCOUNT_ID = sh(returnStdout: true, script: "aws sts get-caller-identity --query Account --output text").trim()
                ECR_REGISTRY_URL = "https://${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com/javaduck"
            }
            steps {
                script {
                    if (env.BRANCH_NAME == 'master') {
                        docker.build("javaduck:${env.BUILD_ID}", "aggregation-worker/")
                        docker.withRegistry(env.ECR_REGISTRY_URL) {
                            docker.image("javaduck:${env.BUILD_ID}").push('latest')
                        }
                    } else {
                        docker.build("javaduck-${env.BRANCH_NAME}:${env.BUILD_ID}", "aggregation-worker/")
                    }
                }
            }
        }
    }
}
