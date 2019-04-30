#!groovy​

pipeline {
    agent { label 'master' }

    environment {
        AWS_REGION = "ap-southeast-2"
        AWS_ACCOUNT_ID = sh(returnStdout: true, script: "aws sts get-caller-identity --query Account --output text").trim()
        ECR_REGISTRY_URL = "https://${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com/javaduck"
    }

    stages {
        stage('package') {
            agent {
                dockerfile {
                    reuseNode true
                    args '-v ${HOME}/.m2:/var/maven/.m2'
                }
            }
            environment {
                JAVA_TOOL_OPTIONS = '-Duser.home=/var/maven'
            }
            steps {
                sh 'mvn -B clean package'
            }
        }
        stage('docker_build_master') {
            when { branch 'master' }
            steps {
                script {
                    docker.build("javaduck:${env.BUILD_ID}", "aggregation-worker/")
                    docker.withRegistry(env.ECR_REGISTRY_URL) {
                        docker.image("javaduck:${env.BUILD_ID}").push('latest')
                    }
                }
            }
        }
        stage('docker_build_branch') {
            when { not { branch 'master' } }
            steps {
                script {
                    docker.build("javaduck-${env.BRANCH_NAME}:${env.BUILD_ID}", "aggregation-worker/")
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: '*/target/*.zip,wps-cloudformation-template.yaml,lambda/**', fingerprint: true, onlyIfSuccessful: true
        }
    }
}
