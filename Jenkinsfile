pipeline {
    agent {
        node {
            label 'miplci3.jpl.nasa.gov'
        }
    }

    stages {

        stage('Start Stages') {
            steps {
                echo "--Stages Started--"
                pwd

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

        stage('Run CI Script: run_ci_test.sh') {
            steps {
                sh """
                pwd
                """

                sh """
                cp ../../run_ci_test.sh .
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
                ./stop_cws.sh
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