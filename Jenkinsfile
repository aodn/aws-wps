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
                stage('set_version_release') {
                    when { branch "master" }
                    steps {
                        withCredentials([usernamePassword(credentialsId: env.GIT_CREDENTIALS_ID, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                            sh './bumpversion.sh'
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
                    docker.build("javaduck:${env.BUILD_TAG}", "aggregation-worker/")
                    if (env.BRANCH_NAME == 'master') {
                        withEnv(['PATH+EXTRA=/var/lib/jenkins/bin']) {
                            docker.withRegistry(env.ECR_REGISTRY_URL) {
                                dockerImage = docker.image("javaduck:${env.BUILD_TAG}")
                                dockerImage.push("${env.BUILD_TAG}")
                                dockerImage.push("latest")
                            }
                        }
                    }
                }
            }
        }
    }
}
