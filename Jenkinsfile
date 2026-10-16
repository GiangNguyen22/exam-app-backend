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
        PROD_DIR = '/opt/exam-app-backend'
        SSH_KNOWN_HOSTS = '/home/jenkins/.ssh/known_hosts'
        DEPLOY_SCRIPT = '/usr/local/sbin/deploy-exam-app-backend'
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

                    echo "=== Repository files ==="
                    ls -la

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

                    echo "=== Java version ==="
                    java -version

                    echo "=== Maven Wrapper version ==="
                    ./mvnw -version

                    echo "=== Build and run tests ==="
                    ./mvnw -B clean verify

                    echo "=== Verify JAR artifact ==="
                    test -f "$APP_JAR"
                    ls -lh "$APP_JAR"
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

                    echo "=== Validate build artifacts ==="
                    test -f Dockerfile
                    test -f "$APP_JAR"

                    echo "=== Build Docker images ==="
                    docker build --pull \
                        -t "$DOCKERHUB_REPO:test" \
                        -t "$DOCKERHUB_REPO:git-$IMAGE_TAG" \
                        .

                    echo "=== Verify Docker images ==="
                    docker image inspect "$DOCKERHUB_REPO:test" >/dev/null
                    docker image inspect "$DOCKERHUB_REPO:git-$IMAGE_TAG" >/dev/null
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

                        echo "$DOCKERHUB_TOKEN" |
                            docker login \
                                --username "$DOCKERHUB_USER" \
                                --password-stdin

                        docker push "$DOCKERHUB_REPO:git-$IMAGE_TAG"
                        docker push "$DOCKERHUB_REPO:test"

                        echo "Pushed image: $DOCKERHUB_REPO:git-$IMAGE_TAG"
                    '''
                }
            }
        }

        stage('Test Production SSH') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'deploy-production',
                        keyFileVariable: 'PROD_SSH_KEY',
                        usernameVariable: 'CREDENTIAL_USERNAME' // Dùng biến tạm tránh trùng lặp
                    )
                ]) {
                    sh '''
                        set -eu

                        echo "=== Test SSH connection as user: $PROD_USER ==="

                        ssh \
                            -i "$PROD_SSH_KEY" \
                            -o IdentitiesOnly=yes \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=yes \
                            -o UserKnownHostsFile="$SSH_KNOWN_HOSTS" \
                            -o ConnectTimeout=10 \
                            "$PROD_USER@$PROD_HOST" \
                            'whoami && hostname'

                        echo "=== Check restricted sudo permission ==="

                        ssh \
                            -i "$PROD_SSH_KEY" \
                            -o IdentitiesOnly=yes \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=yes \
                            -o UserKnownHostsFile="$SSH_KNOWN_HOSTS" \
                            "$PROD_USER@$PROD_HOST" \
                            "sudo -n -l $DEPLOY_SCRIPT"
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

                        echo "=== Deploy image: $IMAGE_REF ==="
                        echo "=== Production host: $PROD_HOST (User: $PROD_USER) ==="

                        ssh \
                            -i "$PROD_SSH_KEY" \
                            -o IdentitiesOnly=yes \
                            -o BatchMode=yes \
                            -o StrictHostKeyChecking=yes \
                            -o UserKnownHostsFile="$SSH_KNOWN_HOSTS" \
                            -o ConnectTimeout=10 \
                            "$PROD_USER@$PROD_HOST" \
                            "sudo -n $DEPLOY_SCRIPT 'git-$IMAGE_TAG'"

                        echo "=== Deployment script completed ==="
                        echo "Deployed image: $IMAGE_REF"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'CI/CD SUCCESS: build, test, push and deployment completed.'
        }

        failure {
            echo 'CI/CD FAILED: inspect the failed stage in Console Output.'
        }

        always {
            echo "Pipeline result: ${currentBuild.currentResult}"
        }
    }
}