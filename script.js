/*
constants & global variables
 */
const no_rubrics = "No rubrics, either add one or load saved file (each line is a rubric)";
const pattern_aID = /\d+-\d+_\d+/;
let qID, key_rubrics, key_graded;
let rubrics;
let has_rubrics = false;
let graded = [];

/*
Auto save and load
 */
window.onbeforeunload = function () {
    if (typeof (Storage) !== "undefined") {
        if (exist(rubrics) && !rubrics.has(no_rubrics)) {
            const r_array = Array.from(rubrics);
            localStorage.setItem(key_rubrics, JSON.stringify(r_array));
        }
        if (exist(graded) && graded.length > 0) {
            localStorage.setItem(key_graded, JSON.stringify(graded));
        }
        const ele = document.getElementsByTagName('textarea');
        for (let i = 0; i <= ele.length - 1; i++) {
            const subID = ele[i].id;
            const comment = ele[i].value;
            if (comment !== "") {
                localStorage.setItem(subID, comment);
            }
        }
    }
}

function onLoad() {
    qID = document.getElementById("qID").innerText;
    key_rubrics = qID + "_rubrics";
    key_graded = qID + "_graded";
    if (typeof (Storage) !== "undefined") {
        hideGraded();
        reloadRubrics();
        reloadGradingResults();
    }
}

function hideGraded() {
    const g = localStorage.getItem(key_graded);
    if (exist(g)) {
        graded = JSON.parse(g);
        for (let id of graded) {
            hide(id);
        }
    }
}

function reloadRubrics() {
    const r = localStorage.getItem(key_rubrics);
    rubrics = exist(r) ? new Set(JSON.parse(r))
        : new Set([no_rubrics]);
    rubrics.forEach((rubric) => addOneRubric(rubric, -1));
}

function reloadGradingResults() {
    for (let key in localStorage) {
        if (!key.match(pattern_aID))
            continue
        const comment = localStorage.getItem(key);
        const ele = document.getElementById(key);
        if (comment === null || ele === null)
            continue;
        ele.value = comment.trim();
        localStorage.removeItem(key);
    }
}

/*
Rubrics related.
 */
function addRubric(id) {
    let input = document.getElementById("r_" + id);
    let rubric = input.value;
    if (rubric === "") {
        alert("No input, please enter a rubric.\nFormat: deduction comment or just comment if no deduction");
        return;
    } else if (rubrics.has(rubric)) {
        alert("Rubric exists");
        return;
    }
    input.value = "";
    addOneRubric(rubric, -1)
    applyRubric(id, rubric);
}

function updateRubric(sID) {
    modifyRubric(sID, "update", function (i, original) {
        const updated = prompt("Edit rubric: ", original);
        if (updated === null || updated === original) return null;
        rubrics.delete(original);
        rubrics.add(updated);
        addOneRubric(updated, i);
        return updated;
    })
}

function deleteRubric(sID) {
    modifyRubric(sID, "delete", deleteOneRubric)
}

function applyRubric(id, rubric) {
    if (rubric === "")
        rubric = document.getElementById("sl_" + id).value
    const textarea = document.getElementById(id);
    const original = textarea.value;
    if (rubric !== "" && rubric !== no_rubrics && !original.includes(rubric)) {
        const a = original === "" ? rubric : "\n" + rubric
        textarea.value += a;
    }
}

function addOneRubric(rubric, position) {
    rubrics.add(rubric);
    let select = document.getElementsByTagName('select');
    if (position === -1)
        position = !has_rubrics ? 0 : select[0].options.length;

    if (!has_rubrics && rubric !== no_rubrics) {
        has_rubrics = true;
        rubrics.delete(no_rubrics);
    }

    for (let i = 0; i <= select.length - 1; i++) {
        if (rubric === no_rubrics) {
            select[i].options.length = 0;
        }
        select[i].options[position] = new Option(rubric, rubric);
    }
}

function deleteOneRubric(position, rubric) {
    rubrics.delete(rubric);
    if (position >= 0) {
        let select = document.getElementsByTagName('select');
        for (let s of select) {
            s.remove(position);
        }
    }
    if (rubrics.size === 0)
        reset();
    return "";
}

function clearRubric(ask) {
    if (!has_rubrics) return;
    if (has_rubrics && ask) {
        if (!confirm("Are you sure you want to clear all rubrics? \nMake sure you have a copy saved."))
            return;
    }
    localStorage.removeItem(key_rubrics);
    let position = rubrics.size - 1;
    for (const rubric of rubrics) {
        if (position < 0) break;
        deleteOneRubric(position, rubric);
        position--;
    }
}

function loadRubric() {
    const message = "Has rubrics already\nClick OK to override, Cancel to append";
    if (has_rubrics && confirm(message)) {
        reset();
    }
    let reader = new FileReader();
    reader.readAsText(this.files[0]);
    reader.onload = function () {
        let lines = reader.result.split(/\r\n|\n/);
        for (let i = 0; i < lines.length; i++) {
            if (lines[i] === "") continue;
            addOneRubric(lines[i], -1);
        }
    }
}

function downloadRubrics() {
    if (!has_rubrics) {
        alert("No rubrics to save");
        return;
    }

    const qNum = document.getElementById("qNum").innerText;
    const content = [...rubrics].join('\n');
    download(qNum + "_rubrics", content);
    if (confirm("Rubrics downloaded. Click OK to clear all rubrics on the page or Cancel"))
        clearRubric(false);
}


/*
Grading related
 */
function downloadGrading() {
    graded.length = 0;
    const qNum = document.getElementById("qNum").innerText;
    let data = "";
    let score = document.getElementById("score").innerText;
    const submissions = document.getElementsByClassName("submission");
    for (let sub of submissions) {
        if (sub.style.display === "none") continue;
        graded.push(sub.id);
        const ele = sub.getElementsByTagName('textarea')[0];
        const subID = ele.id;
        const comment = ele.value;
        data += `${subID}\n${comment}\n`;
    }
    data = data.substring(0, data.length - 1);
    const content = `${score}\n${data}`;
    download(qNum, content);
}

function loadSaved() {
    let reader = new FileReader();
    reader.readAsText(this.files[0]);
    reader.onload = function () {
        let lines = reader.result.split(/\r\n|\n/);
        if (lines[0].match(/[0-9]\.[0-9]/) === null
            || lines[1].match(pattern_aID) === null) {
            alert("Seems not a valid result file, please load the correct file. ");
            return;
        }
        let i = 1;
        while (i < lines.length) {
            const current = lines[i];
            if (current.match(pattern_aID)) {
                const sID = current;
                let content = "";
                while (i + 1 !== lines.length && !lines[i + 1].match(pattern_aID)) {
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
                    const student = document.getElementById("stu_" + sID).innerText;
                    if (!confirm(`${student} has grading data\nClick OK to override, Cancel to append`)) {
                        content = element.value + "\n" + content;
                    }
                }
                if (content.trim() !== "")
                    element.value = content.trim();
            }
            i++;
        }
    };
}

/*
Grade later related
 */
function hide(subID) {
    const x = document.getElementById(subID);
    if (x !== null)
        x.style.display = "none";
}

function unhideAll() {
    const submissions = document.getElementsByClassName("submission");
    for (let sub of submissions) {
        if (sub.style.display === "none")
            sub.style.display = "block";
    }
}

/*
Others
 */
function load(id, func) {
    document.getElementById(id).click();
    document.getElementById(id).addEventListener("change", func);
}

function download(filename, data) {
    const a = document.createElement("a");
    const file = new Blob([data], {type: 'text/plain'});
    a.href = URL.createObjectURL(file);
    a.download = filename.replace(".", "_");
    a.click();
}

function reset() {
    rubrics.clear();
    has_rubrics = false;
    addOneRubric(no_rubrics, 0);
}

function clearComment(aID) {
    document.getElementById(aID).value = "";
}

function exist(ele) {
    return ele !== undefined && ele !== null;
}

function modifyRubric(sID, op, func) {
    const rubric = document.getElementById(sID);
    const i = rubric.selectedIndex;
    if (i === -1 || !has_rubrics) {
        alert("Select a rubric first!")
        return;
    }
    const original = rubric[i].innerText;
    const replacement = func(i, original);
    if (replacement === null) return;

    const message = `Click OK to ${op} applied rubrics in all comment boxes, or Cancel if want to ${op} manually. `;
    if (!confirm(message)) {
        return;
    }

    const ele = document.getElementsByTagName('textarea');
    for (let i = 0; i <= ele.length - 1; i++) {
        let comment = ele[i].value;
        let regex = op === "update" ? original :
            new RegExp(original + "\n*");
        comment = comment.replace(regex, replacement);
        ele[i].value = comment;
    }
}