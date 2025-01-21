let promptCount = 0;

// Function to clear all prompts and reset the interface
function clearAll() {
    document.getElementById('system-prompt').value = '';
    document.getElementById('prompt-list').innerHTML = '<p class="text-muted">No prompts added yet.</p>';
    promptCount = 0;
    document.getElementById('prompt-count').textContent = `(0 prompts)`;
}

function addDataset(element) {
    const dataset = document.createElement('div');
    dataset.classList.add("data-row");
    dataset.classList.add("row");
    dataset.innerHTML = `
          <div class="col">
            <input type="text" name="key" class="form-control" placeholder="key" />
          </div>
          <div class="col">
            <input type="text" name="file" class="form-control" placeholder="path"/>
          </div>
    `;
    element.parentElement.appendChild(dataset)
}

// Function to add a new prompt
function addPrompt(userText = '', assistantResponse = '', userData = [], userCode = '', imports = '') {
    promptCount++;

    // Create new prompt elements
    const row = document.createElement('div');
    row.className = 'row prompt-row g-3 align-items-start mb-3';

    // User Prompt Column
    const userCol = document.createElement('div');
    userCol.className = 'col-md-6';
    let dataset_html = "";
    dataset_html += ""
    if(Array.isArray(userData)) {
        userData.forEach(item => {
            dataset_html += `
        <div class="data-row row">
          <div class="col">
            <input type="text" name="key" class="form-control" value="${item.var}" />
          </div>
          <div class="col">
            <input type="text" name="file" class="form-control" value="${item.file}" />
          </div>
        </div>
      `;
        });
    }

    userCol.innerHTML = `
                <label class="form-label">User Prompt #${promptCount}</label>
                <div class="mb-3 prompt-datasets">
                    <p class="form-label">Datasets</p>
                    <button style='margin-top: 5px' class=\"btn btn-secondary\" onclick=\"addDataset(this)\">Add Dataset</button>
                    <div class="dataset-list">
                        ${dataset_html}
                    </div>
                </div>
                <div class="mb-3">
                    <label class="form-label">Fluid Imports</label>
                    <textarea class="form-control prompt-imports" rows="5" placeholder="Enter imports (one for line)...">${imports}</textarea>
                </div>
                <div class="mb-3">
                    <label class="form-label">Fluid Code</label>
                    <textarea class="form-control prompt-code" rows="5" placeholder="Enter Fluid code...">${userCode}</textarea>
                </div>
                <div class="mb-3">
                    <label class="form-label">Text</label>
                    <textarea class="form-control prompt-text" rows="5" placeholder="Enter text...">${userText}</textarea>
                </div>
            `;

    // Assistant Column
    const assistantCol = document.createElement('div');
    assistantCol.className = 'col-md-6';
    assistantCol.innerHTML = `
                <label class="form-label">Assistant Response #${promptCount}</label>
                <textarea class="form-control prompt-assistant" rows="15" placeholder="Enter assistant response...">${assistantResponse}</textarea>
            `;

    // Append columns to the row
    row.appendChild(userCol);
    row.appendChild(assistantCol);

    // Add a separator (line) before each new prompt
    const separator = document.createElement('hr');

    // Add the new row and separator to the prompt list
    const promptList = document.getElementById('prompt-list');
    const placeholder = promptList.querySelector('p.text-muted');
    if (placeholder) placeholder.remove(); // Remove placeholder if it exists
    promptList.appendChild(separator);
    promptList.appendChild(row);

    // Update prompt count
    document.getElementById('prompt-count').textContent = `(${promptCount} prompts)`;
}

// Function to import JSON and populate prompts
function importJson() {
    const jsonInput = document.getElementById('json-input').value;

    try {
        const parsedData = JSON.parse(jsonInput);

        if (!parsedData || typeof parsedData !== 'object') {
            throw new Error('Invalid JSON structure.');
        }

        // Clear existing data
        clearAll();

        // Populate variables
        const variables = parsedData.variables || {};
        const variablesList = document.getElementById('variables-list');
        variablesList.innerHTML = ''; // Clear current variables

        Object.entries(variables).forEach(([key, value]) => {
            const row = document.createElement('div');
            row.className = 'row g-3 align-items-center mb-2 var-row';
            row.innerHTML = `
                <div class="col-md-5">
                    <input type="text" class="form-control" placeholder="Variable name" value="${key}">
                </div>
                <div class="col-md-5">
                    <input type="text" class="form-control" placeholder="Value" value="${value}">
                </div>
                <div class="col-md-2 text-end">
                    <button class="btn btn-danger btn-sm" onclick="this.parentElement.parentElement.remove()">Remove</button>
                </div>
            `;
            variablesList.appendChild(row);
        });

        // Handle system prompt
        const systemPromptData = parsedData.prompts.find(prompt => prompt.role === 'system');
        if (systemPromptData) {
            const systemPromptElement = document.getElementById('system-prompt');
            const systemPromptContent = systemPromptData.raw_content || systemPromptData.content || '';
            systemPromptElement.value = systemPromptContent;
        }

        // Handle user and assistant prompts
        parsedData.prompts.forEach((prompt) => {
            if (prompt.role === 'user') {
                const userData = prompt.datasets || '';
                const userCode = prompt.code || '';
                const userText = prompt.text || '';
                const imports = prompt.imports || '';
                addPrompt(userText, '', userData, userCode, imports);
            } else if (prompt.role === 'assistant') {
                const assistantContent = prompt.raw_content || prompt.content || '';
                const lastRowAssistant = document.querySelectorAll('#prompt-list .row:last-child .col-md-6:nth-child(2) textarea');
                if (lastRowAssistant.length > 0) {
                    lastRowAssistant[0].value = assistantContent;
                }
            }
        });

        // Close the modal
        const importModal = bootstrap.Modal.getInstance(document.getElementById('importModal'));
        if (importModal) importModal.hide();

    } catch (error) {
        alert('Errore durante l\'importazione del JSON: ' + error.message);
    }
}


function exportJson(mode = 0) {
    const systemPromptElement = document.getElementById('system-prompt');
    const systemPrompt = systemPromptElement.value.trim();

    // Gather all variables
    const variables = {};
    const variableRows = document.querySelectorAll('#variables-list .row');
    variableRows.forEach((row) => {
        const key = row.querySelector('input[type="text"]').value.trim();
        const value = row.querySelectorAll('input[type="text"]')[1].value.trim();
        if (key) variables[key] = value;

    });


    // Initialize prompts array
    const prompts = [];

    if (systemPrompt) {
        prompts.push({
            role: 'system',
            raw_content: systemPrompt       // Raw system prompt with variables
        });
    }

    // Process user and assistant prompts
    const promptList = document.querySelectorAll('#prompt-list .prompt-row');
    promptList.forEach((row, index) => {
        const userData = row.querySelector('.prompt-datasets .dataset-list');

        const userCode = row.querySelector('textarea.prompt-code').value.trim();
        const imports = row.querySelector('textarea.prompt-imports').value.trim();
        const userText = row.querySelector('textarea.prompt-text').value.trim();
        const assistantResponse = row.querySelector('textarea.prompt-assistant').value.trim();

        let datasets = []
        userData.querySelectorAll(".data-row").forEach(item => {
            datasets.push({
                "key": item.querySelector("[name=key]").value.trim() ,
                "file": item.querySelector("[name=file]").value.trim()
            })
        })
        // Add user content
        if (datasets || userCode || userText) {
            prompts.push({
                role: 'user',
                datasets: datasets,        // Unprocessed data with variables
                imports: imports === "" ? [] : imports.split("\n"),
                code: userCode,        // Unprocessed code with variables
                text: userText,  // Unprocessed text with variables
            });
        }

        // Add assistant response
        if (assistantResponse) {
            prompts.push({
                role: 'assistant',
                raw_content: assistantResponse,
            });
        }
    });

    // Create final JSON
    let jsonOutput;
    jsonOutput = JSON.stringify({variables, prompts}, null, 2);

    const blob = new Blob([jsonOutput], {type: 'application/json'});
    const url = URL.createObjectURL(blob);

    // Trigger download
    const link = document.createElement('a');
    link.href = url;
    link.download = 'prompts.json';
    link.click();

    URL.revokeObjectURL(url);
}


function getRandomString(length = 8) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

function addVariable(key = '', value = '') {
    const variablesList = document.getElementById('variables-list');
    const placeholder = variablesList.querySelector('p.text-muted');
    if (placeholder) placeholder.remove(); // Remove placeholder if it exists

    // Create a row for the variable
    const row = document.createElement('div');
    row.className = 'row var-row g-2 align-items-center mb-2';

    // Key Input
    const keyCol = document.createElement('div');
    keyCol.className = 'col-md-5';
    keyCol.innerHTML = `
        <label class="form-label">Key</label>
        <input type="text" class="form-control" placeholder="Enter key..." value="${key}">
    `;

    // Value Input
    const valueCol = document.createElement('div');
    valueCol.className = 'col-md-5';
    valueCol.innerHTML = `
        <label class="form-label">Value</label>
        <input type="text" class="form-control" placeholder="Enter value..." value="${value}">
    `;

    // Delete Button
    const deleteCol = document.createElement('div');
    deleteCol.className = 'col-md-2 text-end';
    deleteCol.innerHTML = `
        <button class="btn btn-danger btn-sm mt-4" onclick="removeVariable(this)">Delete</button>
    `;

    // Append columns to the row
    row.appendChild(keyCol);
    row.appendChild(valueCol);
    row.appendChild(deleteCol);

    // Append the row to the list
    variablesList.appendChild(row);
}

function removeVariable(button) {
    const row = button.parentElement.parentElement;
    row.remove();

    // Check if the list is empty and show placeholder
    const variablesList = document.getElementById('variables-list');
    if (variablesList.children.length === 0) {
        variablesList.innerHTML = '<p class="text-muted">No variables defined yet.</p>';
    }
}

function shufflePrompts() {
    const promptList = document.getElementById('prompt-list');
    const rows = Array.from(promptList.querySelectorAll('.row')); // Get all rows
    const separators = Array.from(promptList.querySelectorAll('hr')); // Get all separators

    // Combine rows and separators
    const combined = rows.map((row, index) => ({
        row,
        separator: separators[index] || null
    }));

    // Shuffle combined rows and separators
    for (let i = combined.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [combined[i], combined[j]] = [combined[j], combined[i]];
    }

    // Clear prompt list and re-append shuffled items
    promptList.innerHTML = '';
    combined.forEach(item => {
        if (item.separator) promptList.appendChild(item.separator);
        promptList.appendChild(item.row);
    });

    alert('Prompts shuffled!');
}
