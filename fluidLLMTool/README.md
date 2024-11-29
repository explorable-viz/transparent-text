## PromptExecutor
PromptExecutor is a command line tool which allows to execute the prompts into an LLM. At this time the only supported model is llama3, but further versions will offer the possibility to use also other models.

### The Java Application
The folder jar, host the java application. It is possible to run the app through the command
'-jar jar/prompt-executor.jar <param1> <param2> [param3]'
where:

- `param1` is the path of the JSON prompt file
- `param2` is the path of the sentence file
- `param3`, which is not mandatory, is the path of the expected result for the validation process.

### Scripts
There are four main bash scripts, placed in the script folder, for the installation and the execution of the Prompt Executor
1. **ollama-install.sh**. This script provides the installation of the ollama platform.
2. **model-pull.sh** This script allows us to pull the models into ollama. Require 1 parameter, which is the model name. For example: ./model-pull.sh llama3
3. **ollama-serve.sh** This script starts the ollama platform (generally on port 11434).
4. **prompt-executor.sh**. This script executes the Java application. It requires the parameters described in the section "The Java Application"

### yarn integration

All the scripts are integrated with yarn scripts.

- `yarn ollama-install` provides to install ollama
- `yarn pull-llama3` provides to pull the llama3 model
- `yarn ollama-serve` provides to start the ollama server
- `yarn test` provides to launch the test with the file in the input folder.