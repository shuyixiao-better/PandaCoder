pipeline {
  agent {
    node {
      label 'base'
    }
  }

    parameters {
        string(name:'APP_NAME',defaultValue: 'torchv-server',description:'应用名称')
        choice(name:'K8S_NAMESPACE', choices: ['test-ais','dev-ais'],description:'部署空间')
        booleanParam(name: 'DEPLOY', defaultValue: true, description: '是否将服务部署到K8S')
    }

    environment {


        // 获取当前时间：年月日时分（格式：YYYYMMDDHHMM）
        CURRENT_TIME = sh(script: 'date +%Y%m%d%H%M', returnStdout: true).trim()

        // 拼接变量：年月日时分-分支名
        TAG = "${CURRENT_TIME}-${env.BRANCH_NAME}"


        REGISTRY = 'harbor.torchv.com'
        HARBOR_NAMESPACE = 'ais'
		IMAGE_NAME = "${REGISTRY}/${HARBOR_NAMESPACE}/${APP_NAME}:${TAG}"



    }

    stages {
       stage ('拉取代码') {
           steps {
               checkout(scm)
           }
       }

        stage ('maven打包') {
           steps {
                container ('base') {
                   sh 'mvn clean package -Dmaven.test.skip=true -U'
               }
            }
        }

        stage ('构建镜像 & 推送镜像') {
            steps {
                container ('kaniko') {
                sh 'pwd'
                sh 'executor  --dockerfile=./Dockerfile --context=dir://./ --destination=${IMAGE_NAME}'
                script {
                    echo "${REGISTRY}/${HARBOR_NAMESPACE}/${APP_NAME}:${TAG}"

					}
				}
            }
        }



		stage ('K8S部署') {

			when {
				allOf {

                    expression { params.DEPLOY == true }
                    // 限制分支发布
                    anyOf {
                        branch 'dev'
                        branch 'test'
                        branch 'dev-devops-test-fsh'
                        branch 'master'
                    }
                }
			}
           steps {
                container ('base') {
					 script {
                                    def deployDir = "deploy/${BRANCH_NAME}"
                                    echo "当前分支是 ${BRANCH_NAME}，正在部署，目录为：${deployDir} ..."
                                    sh "envsubst < ${deployDir}/devops-sample.yaml | cat -"
                                    sh """
                                      # 删除现有的Deployment
                                      kubectl delete deployment ${APP_NAME} -n ${K8S_NAMESPACE} --ignore-not-found

                                      # 应用更新
                                      envsubst < ${deployDir}/devops-sample.yaml | kubectl apply -f -
                                    """
                                }
               }
            }
        }
   }


}
