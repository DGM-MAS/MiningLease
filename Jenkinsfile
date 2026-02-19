pipeline {
    agent any

    environment {
        // Docker
        DOCKER_REGISTRY = 'amitdockerregistry'
        IMAGE_NAME      = 'mas-backend-mining-lease'
        SPRINGBOOT_IMAGE = "${DOCKER_REGISTRY}/${IMAGE_NAME}"
        IMAGE_TAG       = "${BUILD_NUMBER}"
        DOCKER_CREDENTIALS_ID = 'docker-credentials'

        // Kubernetes
        KUBECONFIG_CREDENTIALS_ID = 'kubeconfig-mas'
        NAMESPACE       = 'mas'
        DEPLOYMENT_NAME = 'mas-backend-mining-lease'   // kubectl get deployments -n mas
        CONTAINER_NAME  = 'mas-backend-mining-lease'        // container name in deployment
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
                        dockerImage.push("latest")
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withKubeConfig([credentialsId: KUBECONFIG_CREDENTIALS_ID]) {
                        sh """
                            echo "Updating image in Kubernetes..."
                            kubectl set image deployment/${DEPLOYMENT_NAME} \
                              ${CONTAINER_NAME}=${SPRINGBOOT_IMAGE}:latest \
                              -n ${NAMESPACE}

                            echo "Waiting for rollout to complete..."
                            kubectl rollout status deployment/${DEPLOYMENT_NAME} \
                              -n ${NAMESPACE} --timeout=5m

                            echo "Current pods:"
                            kubectl get pods -n ${NAMESPACE}
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Deployment successful: ${SPRINGBOOT_IMAGE}:latest"
        }
        failure {
            echo "❌ Deployment failed"
        }
    }
}
