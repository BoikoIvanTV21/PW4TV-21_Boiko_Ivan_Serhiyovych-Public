package main

import (
	"fmt"
	"html/template"
	"math"
	"net/http"
	"strconv"
)

type CalcResult struct {
	Out1 string
	Out2 string
	Out3 string
	InputMap map[string]string
}

func main() {
	http.HandleFunc("/", renderUI)
	http.HandleFunc("/calculate", handleCalculation)
	http.ListenAndServe(":9999", nil)
}

func renderUI(w http.ResponseWriter, r *http.Request) {
	tmpl, _ := template.New("ui").Parse(htmlTemplate)
	tmpl.Execute(w, CalcResult{InputMap: make(map[string]string)})
}

func handleCalculation(w http.ResponseWriter, r *http.Request) {
	r.ParseForm()
	inputs := make(map[string]string)
	for k, v := range r.Form {
		inputs[k] = v[0]
	}

	sm := parse(r.FormValue("sm"))
	unom := parse(r.FormValue("unom"))
	ik := parse(r.FormValue("ik"))
	tf := parse(r.FormValue("tf"))
	ct := parse(r.FormValue("ct"))
	jek := parse(r.FormValue("jek"))

	im := (sm / 2) / (math.Sqrt(3) * unom)
	impa := 2 * im
	sek := im / jek
	smin := (ik * math.Sqrt(tf)) / ct

	out1 := fmt.Sprintf("Im: %.2f A\nImpa: %.2f A\nSek: %.2f mm2\nSmin: %.2f mm2", im, impa, sek, smin)

	sk := parse(r.FormValue("sk"))
	ucn := parse(r.FormValue("ucn"))
	snomt := parse(r.FormValue("snomt"))
	uk := parse(r.FormValue("uk"))

	xc := math.Pow(ucn, 2) / sk
	xt := (uk / 100) * (math.Pow(ucn, 2) / snomt)
	xsum := xc + xt
	ip0 := ucn / (math.Sqrt(3) * xsum)

	out2 := fmt.Sprintf("Xc: %.3f Ohm\nXt: %.3f Ohm\nXsum: %.3f Ohm\nIp0: %.3f kA", xc, xt, xsum, ip0)

	rcn := parse(r.FormValue("rcn"))
	xcn := parse(r.FormValue("xcn"))
	rcmin := parse(r.FormValue("rcmin"))
	xcmin := parse(r.FormValue("xcmin"))
	uvn := parse(r.FormValue("uvn"))
	unn := parse(r.FormValue("unn"))
	snomt3 := parse(r.FormValue("snomt3"))
	ukmax := parse(r.FormValue("ukmax"))

	xt3 := (ukmax * math.Pow(uvn, 2)) / (100 * snomt3)
	
	rn := rcn
	xn := xcn + xt3
	rmin := rcmin
	xmin := xcmin + xt3

	kpr := math.Pow(unn, 2) / math.Pow(uvn, 2)
	
	rshn := rn * kpr
	xshn := xn * kpr
	zshn := math.Sqrt(math.Pow(rshn, 2) + math.Pow(xshn, 2))

	rshmin := rmin * kpr
	xshmin := xmin * kpr
	zshmin := math.Sqrt(math.Pow(rshmin, 2) + math.Pow(xshmin, 2))

	ishn3 := (unn * 1000) / (math.Sqrt(3) * zshn)
	ishn2 := ishn3 * (math.Sqrt(3) / 2)
	
	ishmin3 := (unn * 1000) / (math.Sqrt(3) * zshmin)
	ishmin2 := ishmin3 * (math.Sqrt(3) / 2)

	out3 := fmt.Sprintf("Zsh.n: %.3f Ohm\nI(3)sh.n: %.0f A\nI(2)sh.n: %.0f A\n\nZsh.min: %.3f Ohm\nI(3)sh.min: %.0f A\nI(2)sh.min: %.0f A", zshn, ishn3, ishn2, zshmin, ishmin3, ishmin2)

	res := CalcResult{
		Out1: out1,
		Out2: out2,
		Out3: out3,
		InputMap: inputs,
	}

	tmpl, _ := template.New("ui").Parse(htmlTemplate)
	tmpl.Execute(w, res)
}

func parse(s string) float64 {
	v, _ := strconv.ParseFloat(s, 64)
	return v
}

const htmlTemplate = `
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>SYS CALC PW4</title>
<style>
body {
background-color: #1e1e1e;
color: #d4d4d4;
font-family: 'Consolas', 'Monaco', monospace;
display: flex;
justify-content: center;
padding-top: 40px;
}
.main-container {
width: 900px;
background-color: #252526;
padding: 25px;
border: 1px solid #3e3e42;
box-shadow: 0 10px 20px rgba(0,0,0,0.5);
}
h2 { color: #4ec9b0; border-bottom: 2px solid #3e3e42; padding-bottom: 10px; margin-top: 0; }
h3 { color: #ce9178; font-size: 1.1em; }
.input-group {
display: grid;
grid-template-columns: repeat(4, 1fr);
gap: 15px;
margin-bottom: 20px;
}
.field-box { background-color: #333333; padding: 10px; border-left: 3px solid #007acc; }
label { display: block; font-size: 0.8em; color: #9cdcfe; margin-bottom: 5px; }
input {
width: 90%;
background-color: #1e1e1e;
border: 1px solid #3e3e42;
color: #ce9178;
padding: 5px;
font-family: inherit;
}
input:focus { outline: none; border-color: #007acc; }
.btn-panel { display: flex; gap: 15px; margin-top: 20px; }
button {
flex: 1;
padding: 12px;
border: none;
font-family: inherit;
font-weight: bold;
cursor: pointer;
transition: background 0.2s;
}
.btn-calc { background-color: #007acc; color: white; }
.btn-calc:hover { background-color: #005a9e; }
.btn-auto { background-color: #3e3e42; color: #dcdcaa; border: 1px solid #555; }
.btn-auto:hover { background-color: #4e4e52; }
.console-output {
margin-top: 20px;
background-color: #101010;
border: 1px solid #444;
padding: 15px;
font-size: 0.9em;
white-space: pre-wrap;
color: #b5cea8;
}
.highlight { color: #569cd6; }
</style>
<script>
function setDefaults() {
document.getElementById('sm').value = "1300";
document.getElementById('unom').value = "10";
document.getElementById('ik').value = "2500";
document.getElementById('tf').value = "2.5";
document.getElementById('ct').value = "92";
document.getElementById('jek').value = "1.4";

document.getElementById('sk').value = "200";
document.getElementById('ucn').value = "10.5";
document.getElementById('snomt').value = "6.3";
document.getElementById('uk').value = "10.5";

document.getElementById('rcn').value = "10.65";
document.getElementById('xcn').value = "24.02";
document.getElementById('rcmin').value = "34.88";
document.getElementById('xcmin').value = "65.68";
document.getElementById('uvn').value = "115";
document.getElementById('unn').value = "11";
document.getElementById('snomt3').value = "6.3";
document.getElementById('ukmax').value = "11.1";
}
</script>
</head>
<body>
<div class="main-container">
<h2>>> SC NETWORK ANALYSIS</h2>
<form action="/calculate" method="POST">

<h3>MODULE 1 Cable Selection</h3>
<div class="input-group">
<div class="field-box"><label>Sm</label><input type="text" id="sm" name="sm" value="{{.InputMap.sm}}"></div>
<div class="field-box"><label>Unom</label><input type="text" id="unom" name="unom" value="{{.InputMap.unom}}"></div>
<div class="field-box"><label>Ik</label><input type="text" id="ik" name="ik" value="{{.InputMap.ik}}"></div>
<div class="field-box"><label>tf</label><input type="text" id="tf" name="tf" value="{{.InputMap.tf}}"></div>
<div class="field-box"><label>Ct</label><input type="text" id="ct" name="ct" value="{{.InputMap.ct}}"></div>
<div class="field-box"><label>jek</label><input type="text" id="jek" name="jek" value="{{.InputMap.jek}}"></div>
</div>

<h3>MODULE 2 Short Circuit 10kV</h3>
<div class="input-group">
<div class="field-box"><label>Sk</label><input type="text" id="sk" name="sk" value="{{.InputMap.sk}}"></div>
<div class="field-box"><label>Ucn</label><input type="text" id="ucn" name="ucn" value="{{.InputMap.ucn}}"></div>
<div class="field-box"><label>SnomT</label><input type="text" id="snomt" name="snomt" value="{{.InputMap.snomt}}"></div>
<div class="field-box"><label>Uk</label><input type="text" id="uk" name="uk" value="{{.InputMap.uk}}"></div>
</div>

<h3>MODULE 3 Substation Modes</h3>
<div class="input-group">
<div class="field-box"><label>Rc.n</label><input type="text" id="rcn" name="rcn" value="{{.InputMap.rcn}}"></div>
<div class="field-box"><label>Xc.n</label><input type="text" id="xcn" name="xcn" value="{{.InputMap.xcn}}"></div>
<div class="field-box"><label>Rc.min</label><input type="text" id="rcmin" name="rcmin" value="{{.InputMap.rcmin}}"></div>
<div class="field-box"><label>Xc.min</label><input type="text" id="xcmin" name="xcmin" value="{{.InputMap.xcmin}}"></div>
<div class="field-box"><label>Uvn</label><input type="text" id="uvn" name="uvn" value="{{.InputMap.uvn}}"></div>
<div class="field-box"><label>Unn</label><input type="text" id="unn" name="unn" value="{{.InputMap.unn}}"></div>
<div class="field-box"><label>SnomT</label><input type="text" id="snomt3" name="snomt3" value="{{.InputMap.snomt3}}"></div>
<div class="field-box"><label>Uk.max</label><input type="text" id="ukmax" name="ukmax" value="{{.InputMap.ukmax}}"></div>
</div>

<div class="btn-panel">
<button type="button" class="btn-auto" onclick="setDefaults()">LOAD DATASET</button>
<button type="submit" class="btn-calc">EXECUTE</button>
</div>
</form>

{{if .Out1}}
<div class="console-output">
> MODULE 1 RESULTS:
<span class="highlight">{{.Out1}}</span>

> MODULE 2 RESULTS:
<span class="highlight">{{.Out2}}</span>

> MODULE 3 RESULTS:
<span class="highlight">{{.Out3}}</span>
</div>
{{end}}
</div>
</body>
</html>
`