pipeline {
    agent {
        label 'build-server-01'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 20, unit: 'MINUTES')
    }

    environment {
        DOCKERHUB_REPO = 'doanpv10/exam-app-backend'
        APP_JAR = 'target/exam-app-backend-0.0.1-SNAPSHOT.jar'
    }

    stages {
        // Stage 1: Checkout source code
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

        // Stage 2: Build and test
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

        // Stage 3: Build Docker image
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
                    docker image inspect "$DOCKERHUB_REPO:test"
                    docker image inspect "$DOCKERHUB_REPO:git-$IMAGE_TAG"
                '''
            }
        }

        // Stage 4: Push images to Docker Hub
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

                        echo "=== Login to Docker Hub ==="
                        printf '%s' "$DOCKERHUB_TOKEN" |
                            docker login \
                                --username "$DOCKERHUB_USER" \
                                --password-stdin

                        echo "=== Push versioned image ==="
                        docker push "$DOCKERHUB_REPO:git-$IMAGE_TAG"

                        echo "=== Push test image ==="
                        docker push "$DOCKERHUB_REPO:test"

                        echo "=== Push completed ==="
                        echo "Repository: $DOCKERHUB_REPO"
                        echo "Commit tag: git-$IMAGE_TAG"
                        echo "Test tag: test"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'CI SUCCESS: build, test, Docker build and push completed.'
        }

        failure {
            echo 'CI FAILED: inspect the failed stage in Console Output.'
        }

        always {
            echo 'Pipeline execution finished.'
        }
    }


stage('Test Production SSH') {
    steps {
        withCredentials([
            sshUserPrivateKey(
                credentialsId: 'deploy-server',
                keyFileVariable: 'PROD_SSH_KEY',
                usernameVariable: 'PROD_SSH_USER'
            )
        ]) {
            sh '''
                set -eu

                ssh \
                    -i "$PROD_SSH_KEY" \
                    -o IdentitiesOnly=yes \
                    -o BatchMode=yes \
                    -o StrictHostKeyChecking=yes \
                    -o UserKnownHostsFile=/home/jenkins/.ssh/known_hosts \
                    -o ConnectTimeout=10 \
                    "$PROD_SSH_USER@180.93.43.211" \
                    'whoami && hostname'
            '''
        }
    }
}

}