const fs = require('fs');
const path = require('path');

const seedrandom = require('seedrandom');
let generator;

// Funzione per caricare il JSON da un file
function loadJson(filePath) {
    try {
        const rawData = file_get_content(filePath)
        return JSON.parse(rawData);
    } catch (error) {
        console.error(`Error during the loading of the JSON file: ${error.message}`);
        process.exit(1);
    }
}

function file_get_content(filePath) {
    return fs.readFileSync(filePath, 'utf8');
}

function saveJson(filePath, data) {
    try {
        fs.mkdir(path.dirname(filePath), {recursive: true}, function (err) {
            if (err) return cb(err);
            fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf8');
        });
        console.log(`JSON training data saved in: ${filePath}`);
    } catch (error) {
        console.error(`Errore during the generation of training data: ${error.message}`);
        process.exit(1);
    }
}

function transformJson(inputData) {
    const variables = inputData.variables;
    //Generating and replacing value variables
    return inputData.prompts.map(prompt => {


        let content = "";
        if (prompt.role === "user") {
            prompt.datasets.forEach(item => {
                item.file = file_get_content("fluid/" + item.file + ".fld");
            })
            prompt.imports.forEach(item => {
                item = file_get_content("fluid/" + item + ".fld");
            })
            content = JSON.stringify({datasets: prompt.datasets, imports: prompt.imports, code: prompt.code, text: prompt.text});
        } else {
            content = prompt.raw_content;
        }
        for (const [key, value] of Object.entries(variables)) {
            let variablePlaceholder = `$${key}$`;
            let v = value;
            if (value === 'RANDOM_INT') {
                v = Math.floor(generator() * (10));
            } else if (value === 'RANDOM_FLOAT') {
                const rand = generator() * (10);
                v = parseFloat(rand.toFixed(6));
            } else if (value === 'RANDOM_STRING') {
                v = getRandomString(8)
            }
            content = content.replaceAll(variablePlaceholder, v);
        }
        return {
            role: prompt.role,
            content: content
        }
    })

}

function main() {

    const args = process.argv.slice(2);
    if (args.length < 3) {
        console.error('Usage: node output-training-data.js <inputFilePath> <outputFilePath> <modality (seed|random) >');
        process.exit(1);
    }

    generator = args[2] !== "seed" ? seedrandom() : seedrandom("fluid-seed")

    const inputFilePath = path.resolve(args[0]);
    const outputFilePath = path.resolve(args[1]);

    const inputData = loadJson(inputFilePath);
    const outputData = transformJson(inputData);

    saveJson(outputFilePath, outputData);
}

function getRandomString(length = 8) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(generator() * chars.length));
    }
    return result;
}


main();
