#!groovy​

pipeline {
    agent none

    stages {
        stage('container') {
            agent {
                dockerfile {
                    args '-v ${HOME}/.m2:/home/builder/.m2 -v ${HOME}/bin:${HOME}/bin'
                    additionalBuildArgs '--build-arg BUILDER_UID=$(id -u)'
                }
            }
            stages {
                stage('clean') {
                    steps {
                        sh 'git reset --hard'
                        sh 'git clean -xffd'
                    }
                }
                stage('set_version_build') {
                    when { not { branch "master" } }
                    steps {
                        sh './bumpversion.sh build'
                    }
                }
                stage('set_version_release') {
                    when { branch "master" }
                    steps {
                        withCredentials([usernamePassword(credentialsId: env.GIT_CREDENTIALS_ID, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                            sh './bumpversion.sh release'
                        }
                    }
                }
                stage('package') {
                    steps {
                        sh 'mvn -B clean package'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: '*/target/*.zip,wps-cloudformation-template.yaml,lambda/**', fingerprint: true, onlyIfSuccessful: true
                }
            }
        }
        stage('build_worker') {
            agent { label 'master' }
            environment {
                AWS_REGION = "ap-southeast-2"
                AWS_ACCOUNT_ID = sh(returnStdout: true, script: "aws sts get-caller-identity --query Account --output text").trim()
                ECR_REGISTRY_URL = "https://${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com/javaduck"
            }
            steps {
                script {
                    if (env.BRANCH_NAME == 'angus-testing') {
                        docker.build("javaduck:${env.BUILD_ID}", "aggregation-worker/")
                        withEnv(['PATH+EXTRA=/var/lib/jenkins/bin']) {
                            docker.withRegistry(env.ECR_REGISTRY_URL) {
				dockerImage = docker.image("javaduck:${env.BUILD_ID}")
                                dockerImage.push("latest")
				dockerImage.push("${env.BUILD_NUMBER}")
                            }
                        }
                    } else {
                        docker.build("javaduck-${env.BRANCH_NAME}:${env.BUILD_ID}", "aggregation-worker/")
                    }
                }
            }
        }
    }
}
