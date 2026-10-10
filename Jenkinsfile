
pipeline {
    agent {
        label 'build-server-01'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm

                sh '''
                    set -eu
                    echo "=== Branch ==="
                    git branch --show-current
                    echo "=== Commit ==="
                    git rev-parse --short HEAD
                    echo "=== Workspace ==="
                    pwd
                '''
            }
        }

        stage('Verify Toolchain') {
            steps {
                sh '''
                    set -eu
                    java -version
                    git --version
                    test -f pom.xml
                    test -x ./mvnw || chmod +x ./mvnw
                '''
            }
        }

        stage('Build and Test') {
            steps {
                sh './mvnw -B clean verify'
            }
            post {
                always {
                    junit(
                        testResults: '**/target/surefire-reports/TEST-*.xml',
                        allowEmptyResults: true
                    )
                }
            }
        }
    }

    post {
        success {
            echo 'CI SUCCESS: Maven verify completed.'
        }
        failure {
            echo 'CI FAILURE: inspect the failed stage and console log.'
        }
    }
}
