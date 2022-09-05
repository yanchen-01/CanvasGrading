function deleteRubric(sID) {
    const rubric = document.getElementById(sID);
    const i = rubric.selectedIndex;
    if (i !== -1 && rubric.value !== "No rubrics") {
        let select = document.getElementsByTagName('select');
        for (let s of select) {
            s.remove(i);
            if (s.options.length === 0) {
                s.options[0] = new Option("No rubrics, either add one or load saved file (each line is a rubric)", "No rubrics");
            }
        }
    }
}

function clearRubric() {
    let select = document.getElementsByTagName('select');
    if (select[0].options[0].value !== "No rubrics") {
        if (!confirm("Are you sure you want to clear all rubrics? \nMake sure you have a copy saved."))
            return;
    }
    for (let s of select) {
        s.options.length = 0;
        s.options[0] = new Option("No rubrics, either add one or load saved file (each line is a rubric)", "No rubrics");
    }
}

function clearComment(aID) {
    document.getElementById(aID).value = "";
}

function applyRubric(aID, sID) {
    const rubric = document.getElementById(sID).value;
    if (rubric !== "" && rubric !== "No rubrics") {
        const a = document.getElementById(aID).value === "" ?
            rubric : "\n" + rubric
        document.getElementById(aID).value += a;
    }

}

function addRubric(aID, iID) {
    const rubric = document.getElementById(iID).value;
    if (rubric === "") {
        alert("No input, please enter a rubric.\nFormat: deduction comment or just comment if no deduction");
        return;
    }
    document.getElementById(iID).value = "";
    let select = document.getElementsByTagName('select');
    let position = select[0].options[0].value === "No rubrics" ?
        0 : select[0].options.length;

    for (let i = 0; i <= select.length - 1; i++) {
        select[i].options[position] = new Option(rubric, rubric);
    }
    const a = document.getElementById(aID).value === "" ?
        rubric : "\n" + rubric
    document.getElementById(aID).value += a;
}

function saveRubrics() {
    const qID = document.getElementById("qNum").innerText;
    let select = document.getElementsByTagName('select');
    let content = "";
    for (let i = 0; i <= select[0].options.length - 1; i++) {
        content += select[0].options[i].value + "\n";
    }
    download(qID + "_rubrics", content);
}

function loadRubric() {
    let select = document.getElementsByTagName('select');
    let position = 0;
    if (select[0].options[0].value !== "No rubrics") {
        if (confirm("Has rubrics already\nClick OK to override, Cancel to append"))
            for(let i of select)
                i.options.length = 0;
        else position = select[0].options.length;
    }
    let reader = new FileReader();
    reader.readAsText(this.files[0]);
    reader.onload = function () {
        let lines = reader.result.split(/\r\n|\n/);
        for (let i = 0; i < select.length; i++) {
            let k = position
            for (let j = 0; j < lines.length; j++) {
                select[i].options[k] = new Option(lines[j], lines[j]);
                k++;
            }
        }
    }
}

function submit() {
    const qID = document.getElementById("qNum").innerText;
    let data = "";
    let score = document.getElementById("score").innerText;
    const ele = document.getElementsByTagName('textarea');
    for (let i = 0; i <= ele.length - 1; i++) {
        const subID = ele[i].id;
        const comment = ele[i].value;
        data += `${subID}\n${comment}\n`;
    }
    data = data.substring(0, data.length - 1);
    const content = `${score}\n${data}`;
    download(qID, content);
}

function load(id, func) {
    document.getElementById(id).click();
    document.getElementById(id).addEventListener("change", func);
}

function loadSaved() {
    let reader = new FileReader();
    reader.readAsText(this.files[0]);
    reader.onload = function () {
        const format = /[0-9]+_[0-9]+/;
        let lines = reader.result.split(/\r\n|\n/);
        if (lines[0].match(/[0-9]\.[0-9]/) === null
            || lines[1].match(format) === null) {
            alert("Seems not a valid result file, please load the correct file. ");
            return;
        }
        let i = 1;
        while (i < lines.length) {
            const current = lines[i];
            if (current.match(format)) {
                const sID = current;
                let content = "";
                while (i + 1 !== lines.length && !lines[i + 1].match(format)) {
                    if (lines[i + 1].length !== 0)
                        content += lines[i + 1] + "\n";
                    i++;
                }
                let element = document.getElementById(current);
                if (element === null) {
                    alert(`${sID} not found. Check if you are loading a valid file.`);
                    return;
                }
                if (element.value.length !== 0 && content.trim() !== "") {
                    const student = document.getElementById(sID + "_").innerText;
                    if (!confirm(`${student} has grading data\nClick OK to override, Cancel to append`)) {
                        content = element.value + "\n" + content;
                    }
                }
                if(content.trim() !== "")
                    element.value = content.trim();
            }
            i++;
        }
    };
}

function download(filename, data) {
    const a = document.createElement("a");
    const file = new Blob([data], {type: 'text/plain'});
    a.href = URL.createObjectURL(file);
    a.download = filename;
    a.click();
}