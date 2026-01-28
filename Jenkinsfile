pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'amitdockerregistry'  // CHANGE THIS
        SPRINGBOOT_IMAGE = "${DOCKER_REGISTRY}/mas-springboot-app"
        IMAGE_TAG = "${BUILD_NUMBER}"
        DOCKER_CREDENTIALS_ID = 'docker-credentials'
        KUBECONFIG_CREDENTIALS_ID = 'kubeconfig-mas'
        NAMESPACE = 'mas'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Spring Boot') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    dockerImage = docker.build("${SPRINGBOOT_IMAGE}:${IMAGE_TAG}")
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('', DOCKER_CREDENTIALS_ID) {
                        dockerImage.push("${IMAGE_TAG}")
                        dockerImage.push('latest')
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withKubeConfig([credentialsId: KUBECONFIG_CREDENTIALS_ID]) {
                        sh """
                            kubectl set image deployment/springboot-app \
                                springboot=${SPRINGBOOT_IMAGE}:${IMAGE_TAG} \
                                -n ${NAMESPACE} --record
                            kubectl rollout status deployment/springboot-app -n ${NAMESPACE} --timeout=5m
                            kubectl get pods -n ${NAMESPACE}
                        """
                    }
                }
            }
        }
    }
}
