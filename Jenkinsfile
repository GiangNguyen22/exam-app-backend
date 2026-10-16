pipeline {
    agent { label 'build-server-01' }

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 20, unit: 'MINUTES')
    }

    environment {
        DOCKERHUB_REPO = 'doanpv10/exam-app-backend'
        APP_JAR = 'target/exam-app-backend-0.0.1-SNAPSHOT.jar'
        PROD_HOST = '180.93.43.211'
        PROD_USER = 'deploy' // Tài khoản SSH vào server Production
        CONTAINER_NAME = 'exam-app-backend'
        CONTAINER_PORT = '8080' // Đổi port nếu ứng dụng Spring Boot của bạn dùng port khác
        SSH_KNOWN_HOSTS = '/home/jenkins/.ssh/known_hosts'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm

                script {
                    env.IMAGE_TAG = sh(
                        script: 'git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim()
                }

                sh '''
                    set -eu
                    echo "=== Git branch and commit ==="
                    git branch --show-current
                    git log -1 --oneline
                    echo "=== Validate Docker files ==="
                    test -f Dockerfile
                    test -f .dockerignore
                '''

                echo "Docker image tag: git-${IMAGE_TAG}"
            }
        }

        stage('Build and Test') {
            steps {
                sh '''
                    set -eu
                    chmod +x mvnw
                    echo "=== Build and run tests ==="
                    ./mvnw -B clean verify
                    echo "=== Verify JAR artifact ==="
                    test -f "$APP_JAR"
                '''
            }

            post {
                always {
                    junit(
                        testResults: '**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml',
                        allowEmptyResults: true
                    )
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eu
                    echo "=== Build Docker images ==="
                    docker build --pull \
                        -t "$DOCKERHUB_REPO:test" \
                        -t "$DOCKERHUB_REPO:git-$IMAGE_TAG" \
                        .
                '''
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'jenkin-dockerhub',
                        usernameVariable: 'DOCKERHUB_USER',
                        passwordVariable: 'DOCKERHUB_TOKEN'
                    )
                ]) {
                    sh '''
                        set +x
                        set -eu
                        trap 'docker logout >/dev/null 2>&1 || true' EXIT

                        echo "$DOCKERHUB_TOKEN" | docker login --username "$DOCKERHUB_USER" --password-stdin
                        docker push "$DOCKERHUB_REPO:git-$IMAGE_TAG"
                        docker push "$DOCKERHUB_REPO:test"
                    '''
                }
            }
        }

        stage('Deploy to Production') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'deploy-production',
                        keyFileVariable: 'PROD_SSH_KEY',
                        usernameVariable: 'CREDENTIAL_USERNAME'
                    )
                ]) {
                    sh '''
                        set -eu

                        IMAGE_REF="$DOCKERHUB_REPO:git-$IMAGE_TAG"
                        echo "=== Connecting to Production ($PROD_USER@$PROD_HOST) to deploy $IMAGE_REF ==="

                        ssh \
                            -i "$PROD_SSH_KEY" \
                            -o IdentitiesOnly=yes \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=yes \
                            -o UserKnownHostsFile="$SSH_KNOWN_HOSTS" \
                            -o ConnectTimeout=10 \
                            "$PROD_USER@$PROD_HOST" \
                            "docker pull $IMAGE_REF && \
                             (docker stop $CONTAINER_NAME || true) && \
                             (docker rm $CONTAINER_NAME || true) && \
                             docker run -d --name $CONTAINER_NAME --restart always -p $CONTAINER_PORT:$CONTAINER_PORT -e SPRING_PROFILES_ACTIVE=prod $IMAGE_REF && \
                             docker image prune -f"

                        echo "=== Deployment Finished Successfully ==="
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'CI/CD SUCCESS: build, test, push and automated deployment completed.'
        }
        failure {
            echo 'CI/CD FAILED: inspect the failed stage in Console Output.'
        }
    }
}