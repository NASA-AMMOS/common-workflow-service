pipeline {

    agent any

    stages {

        stage('Start Stages') {
            steps {
                echo "--Stages Started--"

                sh """
                pwd
                """

                sh """
                ../.././start.sh
                """
            }
        }

        stage('Branches Check') {
            when {
                branch "*"
            }
            steps {
                echo 'BRANCH NAME: ' + env.BRANCH_NAME
            }
        }

        stage('PR Branches Check') {
            when {
                branch "PR-*"
            }
            steps {
                echo 'PR BRANCH NAME: ' + env.BRANCH_NAME
            }
        }

        stage('Run CI Script: run_ci.sh') {
            steps {
                sh """
                pwd
                """

                sh """
                cp ../../run_ci_test.sh .
                """

                sh """
                chmod u+x run_ci_test.sh
                """

                sh """
                ./run_ci_test.sh
                """
            }
        }


        stage('CI Completed') {
            steps {
                sh """
                pwd
                """

                sh """
                rm run_ci_test.sh
                """

                sh """
                ./stop_dev.sh
                """
            }
        }


        stage('Integration & Unit Tests') {
            steps {
                sh """
                pwd
                """

            }
        }


        stage('End of Pipeline') {
            steps {
                sh """
                pwd
                """

            }
        }

    }
}

