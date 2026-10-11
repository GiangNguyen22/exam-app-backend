
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

    //Stage1
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
                echo "===Git branch and Commit ==="
                git branch --show-current
                git log -1 --oneline

                echo "===Repository file==="
                ls -la
                echo "===Dockerfile validation==="
                test -f Dockerfile
                test -f .dockerignore
            '''
            echo "Docker image tag: git$-{IMAGE_TAG}"

        }
    }

    //Stage2
    stage('Build and Test') {
        steps {
            sh '''
                set -eu
                echo "===Java version ===="
                java -version
                echo "===Maven Wrapper version ===="
                ./mvnw -version
                echo "===Build and run tests ==="
                ./mvnw -B clean verify
                echo "===Verify JAR artifact ==="
                test -f "$APP_JAR"
                ls -lh "$APP_JAR"
            '''
        }
    }

    //Stage3: Build docker image
    stage('Build Docker Image') {
        steps{
            sh '''
                set -eu
                echo "===Validate build artifacts ==="
                test -f Dockerfile
                test -f "$APP_JAR"
                echo "===Build Docker image ==="
                docker build --pull \
                    -t "$DOCKERHUB_REPO:test" \
                    -t "$DOCKERHUB_REPO:git-$IMAGE_TAG" \
                    .
                echo "===Verify Docker images ==="
                docker images inspect "$DOCKERHUB_REPO:test"
                docker images inspect "$DOCKERHUB_REPO:git-$IMAGE_TAG"
            '''
        }
    }
    //stage4: Push Images to docker hub
    stage('Push Docker Image') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKERHUB_USER',
                        passwordVariable: 'DOCKERHUB_TOKEN'
                    )
                ]) {
                    sh '''
                        set +x
                        set -eu

                        # Automatically log out after the push stage.
                        trap 'docker logout >/dev/null 2>&1 || true' EXIT

                        echo "=== Login to Docker Hub ==="
                        printf '%s' "$DOCKERHUB_TOKEN" |
                            docker login \
                                --username "$DOCKERHUB_USER" \
                                --password-stdin

                        echo "=== Push versioned image ==="
                        docker push "$DOCKERHUB_REPO:git-$IMAGE_TAG"

                        echo "=== Push test image tag ==="
                        docker push "$DOCKERHUB_REPO:test"

                        echo "=== Docker image push completed ==="
                        echo "Repository: $DOCKERHUB_REPO"
                        echo "Commit tag: git-$IMAGE_TAG"
                        echo "Test tag: test"
                    '''
                }
            }
        }
    
//PIPELINE RESULT
post {
        success {
            echo '''
            CI PIPELINE SUCCESS
            - Source code checked out
            - Maven build and verification completed
            - Docker image built
            - Docker images pushed to Docker Hub
            '''
        }

        failure {
            echo '''
            CI PIPELINE FAILED
            Check the failed stage and console output.
            Later stages are skipped when an earlier stage fails.
            '''
        }

        always {
            echo "Pipeline execution finished."
        }
    }

}

