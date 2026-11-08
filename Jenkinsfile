pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 40, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    environment {
        MAVEN_REPO      = '/var/jenkins_home/.m2/repository'
        SONAR_URL       = 'http://host.docker.internal:9000'
        NEXUS_URL       = 'http://host.docker.internal:8081/repository/maven-snapshots/'
        APP_URL         = 'http://host.docker.internal:8082'
        IMAGE_NAME      = 'rbm-app'
        COMPOSE_PROJECT = 'rbm'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --pretty="Commit %h : %s"'
            }
        }

        stage('Build Maven et tests') {
            steps {
                dir('backend') {
                    sh 'sh mvnw -B -Dmaven.repo.local=$MAVEN_REPO clean verify'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                }
                success {
                    archiveArtifacts artifacts: 'backend/target/rbm.jar', fingerprint: true
                }
            }
        }

        stage('Analyse SonarQube') {
            steps {
                dir('backend') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh '''sh mvnw -B -Dmaven.repo.local=$MAVEN_REPO \
                              org.sonarsource.scanner.maven:sonar-maven-plugin:3.10.0.2594:sonar \
                              -Dsonar.host.url=$SONAR_URL \
                              -Dsonar.token=$SONAR_TOKEN \
                              -Dsonar.projectKey=rbm \
                              -Dsonar.projectName="Room and Event Booking"'''
                    }
                }
            }
        }

        stage('Publication Nexus') {
            steps {
                dir('backend') {
                    withCredentials([usernamePassword(credentialsId: 'nexus-credentials',
                                                      usernameVariable: 'NEXUS_USER',
                                                      passwordVariable: 'NEXUS_PASS')]) {
                        sh '''sh mvnw -B -s ci/settings.xml -Dmaven.repo.local=$MAVEN_REPO \
                              deploy -DskipTests \
                              -DaltDeploymentRepository=nexus::$NEXUS_URL'''
                    }
                }
            }
        }

        stage('Build Angular') {
            steps {
                dir('frontend') {
                    sh 'node -v && npm -v'
                    sh 'npm ci --legacy-peer-deps --cache /var/jenkins_home/.npm'
                    sh 'npx ng build --configuration production'
                }
            }
        }

        stage('Build image Docker') {
            steps {
                sh 'docker build -t $IMAGE_NAME:$BUILD_NUMBER -t $IMAGE_NAME:latest .'
            }
        }

        stage('Déploiement') {
            steps {
                withCredentials([file(credentialsId: 'rbm-env', variable: 'ENV_FILE')]) {
                    sh '''cp "$ENV_FILE" .env
                          docker compose -p $COMPOSE_PROJECT up -d --no-build'''
                }
                sh '''code=000
                      for i in $(seq 1 30); do
                        code=$(curl -s -o /dev/null -w "%{http_code}" $APP_URL/index.html || true)
                        [ "$code" = "200" ] && break
                        sleep 5
                      done
                      [ "$code" = "200" ] || { echo "Application injoignable (code $code)"; exit 1; }
                      api=$(curl -s -o /dev/null -w "%{http_code}" $APP_URL/api/salles)
                      [ "$api" = "401" ] || { echo "API : 401 attendu, obtenu $api"; exit 1; }
                      echo "Application en ligne, API protégée."'''
            }
            post {
                always {
                    sh 'rm -f .env'
                }
            }
        }
    }

    post {
        success { echo 'Pipeline terminé avec succès.' }
        failure { echo 'Pipeline en échec : consultez la sortie de la console.' }
    }
}