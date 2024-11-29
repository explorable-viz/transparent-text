let promptCount = 0;

// Function to clear all prompts and reset the interface
function clearAll() {
    document.getElementById('system-prompt').value = '';
    document.getElementById('prompt-list').innerHTML = '<p class="text-muted">No prompts added yet.</p>';
    promptCount = 0;
    document.getElementById('prompt-count').textContent = `(0 prompts)`;
}

// Function to add a new prompt
function addPrompt(userCaption = '', assistantResponse = '', userData = '', userCode = '') {
    promptCount++;

    // Create new prompt elements
    const row = document.createElement('div');
    row.className = 'row g-3 align-items-start mb-3';

    // User Prompt Column
    const userCol = document.createElement('div');
    userCol.className = 'col-md-6';
    userCol.innerHTML = `
                <label class="form-label">User Prompt #${promptCount}</label>
                <div class="mb-3">
                    <label class="form-label">Data</label>
                    <textarea class="form-control prompt-data" rows="5" placeholder="Enter data...">${userData}</textarea>
                </div>
                <div class="mb-3">
                    <label class="form-label">BOB Code</label>
                    <textarea class="form-control prompt-code" rows="5" placeholder="Enter BOB code...">${userCode}</textarea>
                </div>
                <div class="mb-3">
                    <label class="form-label">Caption</label>
                    <textarea class="form-control prompt-caption" rows="5" placeholder="Enter caption...">${userCaption}</textarea>
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
            row.className = 'row g-3 align-items-center mb-2';
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
                const userData = prompt.data || '';
                const userCode = prompt.code || '';
                const userCaption = prompt.caption || '';
                addPrompt(userCaption, '', userData, userCode);
            } else if (prompt.role === 'assistant') {
                const assistantContent = prompt.content || '';
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


function exportJson() {
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

    // Process the system prompt
    let processedSystemPrompt = systemPrompt;
    for (const [key, value] of Object.entries(variables)) {
        const variablePlaceholder = `$${key}$`;
        processedSystemPrompt = processedSystemPrompt.replaceAll(variablePlaceholder, value);
    }

    // Initialize prompts array
    const prompts = [];

    if (systemPrompt) {
        prompts.push({
            role: 'system',
            content: processedSystemPrompt, // Processed system prompt
            raw_content: systemPrompt       // Raw system prompt with variables
        });
    }

    // Process user and assistant prompts
    const promptList = document.querySelectorAll('#prompt-list .row');
    promptList.forEach((row, index) => {
        const userData = row.querySelector('textarea.prompt-data').value.trim();
        const userCode = row.querySelector('textarea.prompt-code').value.trim();
        const userCaption = row.querySelector('textarea.prompt-caption').value.trim();
        const assistantResponse = row.querySelector('textarea.prompt-assistant').value.trim();

        // Replace variables in content (concatenated fields)
        let processedContent = [userData, userCode, userCaption].join('\n');
        for (const [key, value] of Object.entries(variables)) {
            const variablePlaceholder = `$${key}$`;
            processedContent = processedContent.replaceAll(variablePlaceholder, value);
        }

        // Add user content
        if (userData || userCode || userCaption) {
            prompts.push({
                role: 'user',
                data: userData,        // Unprocessed data with variables
                code: userCode,        // Unprocessed code with variables
                caption: userCaption,  // Unprocessed caption with variables
                content: processedContent // Processed content with variables replaced
            });
        }

        // Replace variables in assistant response
        const processedAssistantResponse = assistantResponse.replaceAll(/\$([A-Za-z0-9_]+)\$/g, (match, varName) => {
            return variables[varName] || match; // Replace or leave as-is if no match
        });

        // Add assistant response
        if (assistantResponse) {
            prompts.push({
                role: 'assistant',
                content: processedAssistantResponse
            });
        }
    });

    // Create final JSON
    const jsonOutput = JSON.stringify({ variables, prompts }, null, 2);
    const blob = new Blob([jsonOutput], { type: 'application/json' });
    const url = URL.createObjectURL(blob);

    // Trigger download
    const link = document.createElement('a');
    link.href = url;
    link.download = 'prompts.json';
    link.click();

    URL.revokeObjectURL(url);
}



function addVariable(key = '', value = '') {
    const variablesList = document.getElementById('variables-list');
    const placeholder = variablesList.querySelector('p.text-muted');
    if (placeholder) placeholder.remove(); // Remove placeholder if it exists

    // Create a row for the variable
    const row = document.createElement('div');
    row.className = 'row g-2 align-items-center mb-2';

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
