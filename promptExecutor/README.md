![PromptExecutor](https://github.com/explorable-viz/transparent-text/actions/workflows/prompt-executor.yml/badge.svg)
![PromptExecutorMocked](https://github.com/explorable-viz/transparent-text/actions/workflows/prompt-executor-mocked.yml/badge.svg)

## PromptExecutor
PromptExecutor is a command line tool which allows to execute the prompts into a Large Language Model. At this time the only supported model is llama3, but further versions will offer the possibility to use also other models.

### The Java Application
The folder jar, host the java application. It is possible to run the app through the command

`java -jar jar/prompt-executor.jar <agent> <prompt> <settings> <queries> [expected]`

where:

- `agent` is the agent class which correspond to the model will be executed
- `prompt` is the path of the JSON prompt file
- `settings` is the path of the JSON settings file
- `queries` is the path of the queries file
- `expected`, which is not mandatory, is the path of the expected result for the validation process.

### List of available agents

| Class                      | Model                  | Token Needed |
|----------------------------|------------------------|--------------|
| ClaudeSonnetEvaluatorAgent | claude-sonnet-20241022 | yes          |
| GeminiPro15EvaluationAgent | gemini-pro             | yes          |
| Gpt4oEvaluationAgent       | gpt-4o                 | yes          |
| Gpt35EvaluationAgent       | gpt-3.5                | yes          |
| Llama3EvaluatorAgent       | llama3:8b              | no           |
| Llama31EvaluatorAgent      | llama3.1:8b            | no           |


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

### Webtools

There are two web-based tools which allow to edit the prompt base-knowledge and the input queries. These tools are in the folder `./webapp`

| Tool          | Description                           | Link                                                           |
|---------------|---------------------------------------|----------------------------------------------------------------|
| Prompt Editor | Tool for the editing of the prompt    | [./webapp/prompt-editor.html](./webapp/prompt-editor.html)     |
| Query Editor  | Tool for the editing of the queries | [./webapp/queries-editor.html](./webapp/queries-editor.html) |

## Websites

Paths are relative to folder containing these instructions. `$WEBSITE_NAME` refers to any of the website
folders under `website`.

### Bundling/Testing Websites
To bundle a website, run the command `yarn fluid publish -w $WEBSITE_NAME -l`.
This will create a folder in `dist` with a Lisp-cased version of `$WEBSITE_NAME`.

To test the website in the browser:
1. run `npx http-serve dist/$WEBSITE_NAME_LISP_CASE -a 127.0.0.1 -c-1`
2. Open your browser at localhost

To run your website tests:
1. Run the command `yarn website-test $WEBSITE_NAME_LISP_CASE`
