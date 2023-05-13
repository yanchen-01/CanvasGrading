let graded = [];
window.onbeforeunload = function () {
    if (typeof (Storage) !== "undefined") {
        if (graded !== null && graded.length > 0) {
            localStorage.setItem("graded", JSON.stringify(graded));
        }
        const ele = document.getElementsByTagName('textarea');
        for (let i = 0; i <= ele.length - 1; i++) {
            const subID = ele[i].id;
            const comment = ele[i].value;
            if (comment !== "") {
                localStorage.setItem(subID, comment);
            }
        }
        const qID = document.getElementById("qNum").innerText;
        const rubrics = saveRubrics();
        localStorage.setItem(qID, rubrics);
    }
}

function saveRubrics() {
    let select = document.getElementsByTagName('select');
    let content = "";
    for (let i = 0; i <= select[0].options.length - 1; i++) {
        const rubric = select[0].options[i].value;
        if (rubric !== "") {
            content += select[0].options[i].value + "\n";
        }
    }
    return content;
}

function onLoad() {
    const qID = document.getElementById("qNum").innerText;
    if (typeof (Storage) !== "undefined" && localStorage.length !== 0) {
        for (let key in localStorage) {
            if (key === "graded") {
                unhideGraded(key);
                continue;
            } else if (key === qID) {
                const rubrics = localStorage.getItem(qID);
                reloadRubrics(rubrics, 0);
                continue;
            }
            const comment = localStorage.getItem(key);
            const ele = document.getElementById(key);
            if (comment === null || ele === null)
                continue;
            ele.value = comment.trim();
            localStorage.removeItem(key);
        }
    }
}

function reloadRubrics(rubrics, initialPosition) {
    let select = document.getElementsByTagName('select');
    let lines = rubrics.split(/\r\n|\n/);
    for (let i = 0; i < select.length; i++) {
        let k = initialPosition;
        for (let j = 0; j < lines.length; j++) {
            if (lines[j] === "") continue;
            const text = lines[j] === "No rubrics"?
                "No rubrics, either add one or load saved file (each line is a rubric)" : lines[j];
            select[i].options[k] = new Option(text, lines[j]);
            k++;
        }
    }
}

function unhideGraded(key) {
    graded = JSON.parse(localStorage.getItem(key));
    if (graded !== null) {
        for (let id of graded) {
            hide(id);
        }
    }
}

function hide(sID) {
    const x = document.getElementById(sID);
    x.style.display = "none";
}

function unhide(sID) {
    const x = document.getElementById(sID);
    x.style.display = "block";
}

function unhideAll() {
    const submissions = document.getElementsByClassName("submission");
    for (let sub of submissions) {
        if (sub.style.display === "none")
            sub.style.display = "block";
    }
}

function updateRubric(sID) {
    const rubric = document.getElementById(sID);
    const i = rubric.selectedIndex;
    if (i === -1 || rubric.value === "No rubrics") {
        alert("Select a rubric first!")
        return;
    }
    const original = rubric[i].innerText;
    const updated = prompt("Edit rubric: ", original);
    if (updated === null) return;
    let select = document.getElementsByTagName('select');
    for (let s of select) {
        s.options[i] = new Option(updated, updated);
        if (s.options.length === 0) {
            s.options[0] = new Option("No rubrics, either add one or load saved file (each line is a rubric)", "No rubrics");
        }
    }
    if (!confirm("Click OK to update applied rubrics in all comment boxes, or Cancel if want to update manually. ")) {
        return;
    }
    const ele = document.getElementsByTagName('textarea');
    for (let i = 0; i <= ele.length - 1; i++) {
        let comment = ele[i].value;
        comment = comment.replace(original, updated);
        ele[i].value = comment;
    }
}

function deleteRubric(sID) {
    const rubric = document.getElementById(sID);
    const i = rubric.selectedIndex;
    if (i === -1 || rubric.value === "No rubrics") {
        alert("Select a rubric first!")
        return;
    }
    const original = rubric[i].innerText;
    let select = document.getElementsByTagName('select');
    for (let s of select) {
        s.remove(i);
        if (s.options.length === 0) {
            s.options[0] = new Option("No rubrics, either add one or load saved file (each line is a rubric)", "No rubrics");
        }
    }
    if (!confirm("Click OK to delete applied rubrics in all comment boxes, or Cancel if want to delete manually. ")) {
        return;
    }
    const ele = document.getElementsByTagName('textarea');
    for (let i = 0; i <= ele.length - 1; i++) {
        let comment = ele[i].value;
        let regex = new RegExp(original + "\n*");
        comment = comment.replace(regex, "");
        ele[i].value = comment;
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
    removeSavedRubrics();
}

function removeSavedRubrics() {
    const qID = document.getElementById("qNum").innerText;
    localStorage.removeItem(qID);
}

function clearComment(aID) {
    document.getElementById(aID).value = "";
}

function applyRubric(aID, sID) {
    const rubric = document.getElementById(sID).value;
    const original = document.getElementById(aID).value;
    if (rubric !== "" && rubric !== "No rubrics" && !original.includes(rubric)) {
        const a = original === "" ? rubric : "\n" + rubric
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

function downloadRubrics() {
    const qID = document.getElementById("qNum").innerText;
    const content = saveRubrics();
    removeSavedRubrics();
    download(qID + "_rubrics", content);
}

function loadRubric() {
    let select = document.getElementsByTagName('select');
    let position = 0;
    if (select[0].options[0].value !== "No rubrics") {
        if (confirm("Has rubrics already\nClick OK to override, Cancel to append"))
            for (let i of select)
                i.options.length = 0;
        else position = select[0].options.length;
    }
    let reader = new FileReader();
    reader.readAsText(this.files[0]);
    reader.onload = function () {
        reloadRubrics(reader.result, position);
    }
}

function downloadGrading() {
    localStorage.removeItem("graded");
    const qID = document.getElementById("qNum").innerText;
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
                let submission = document.getElementById("u_" + current);
                if (submission.style.display === "none")
                    submission.style.display = "block";
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
                if (content.trim() !== "")
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
    a.download = filename.replace(".", "_");
    a.click();
}