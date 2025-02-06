#!/bin/bash
set -xe

#remove all the parameters that should be removed.
command="java --enable-preview -jar target/authoringAssistant-0.1-jar-with-dependencies.jar agent=$1 threshold=0 inContextLearningPath='system-prompt.json' settings=settings.json testPath=testCases  numTestToGenerate=1 numLearningCaseToGenerate=5"

output=$(eval "$command")
