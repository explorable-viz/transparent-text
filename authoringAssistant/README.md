![AuthoringAssistant](https://github.com/explorable-viz/transparent-text/actions/workflows/prompt-executor.yml/badge.svg)
![AuthoringAssistantMocked](https://github.com/explorable-viz/transparent-text/actions/workflows/prompt-executor-mocked.yml/badge.svg)

## AuthoringAssistant

AuthoringAssistant is a command line tool which allows to execute the prompts into a Large Language Model. At this time the only supported model is llama3, but further versions will offer the possibility to use also other models.

### Prerequisites

Before setting up the project, ensure you have the following dependencies installed:

- **Java Development Kit (JDK) 22** (or a more recent version)
- **Apache Maven** (latest stable version recommended)

####  Automated Installation

To simplify the installation process, you can run the provided script:

```sh
./install-dependencies.sh
```

This script will install the required dependencies automatically.

#### Manual Configuration

After installation, you need to create a `settings.xml` file in your Maven configuration directory:

```sh
${USER_HOME}/.m2/settings.xml
```
Copy and paste the following content into the `settings.xml` file:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>github</id>
            <username>{GITHUB_USERNAME}</username>
            <password>{GITHUB_PERSONAL_ACCESS_TOKEN}</password>
        </server>
    </servers>
</settings>
```
The `{GITHUB_TOKEN}` must be granted the `read:packages` permission.

### The Java Application

The folder jar, host the java application. It is possible to run the app through the command

`java -jar jar/prompt-executor.jar agent=string threshold=float systemPromptPath=string settings=string testPath=string  numTestToGenerate=int numLearningCaseToGenerate=int numQueryToExecute=int`

where:

- `agent` is the agent class which correspond to the model will be executed
- `threshold` represents the accuracy threshold value for which the tests will be considered passed (float between [0-1])
- `systemPromptPath` is the path of the JSON which contains the system-prompt
- `settings` is the path of the JSON settings file
- `testPath` is the path of the test cases file
- `numTestToGenerate` is the number of test cases to generate from a single test case with variables
- `numLearningCaseToGenerate` is the number of learning cases to generate from a single learning case with variables
- `numQueryToExecute` is the maximum number of query which will be executed

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

### Yarn integration

All the scripts are integrated with yarn scripts.

- `yarn ollama-install` to install ollama
- `yarn pull-llama3` to pull llama3 model
- `yarn ollama-serve` to start ollama server
- `yarn test` to launch test with file in input folder

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

To bundle a website:
1. Run `yarn fluid publish -w $WEBSITE_NAME -l`

This will create a folder in `dist` with a Lisp-cased version of `$WEBSITE_NAME`.

To test the website in the browser:
1. run `npx http-serve dist/$WEBSITE_NAME_LISP_CASE -a 127.0.0.1 -c-1`
2. Open browser at localhost

To run your website tests:
1. Run `yarn website-test $WEBSITE_NAME_LISP_CASE`
