import 'package:flutter/material.dart';
import 'dart:math';

void main() {
  runApp(const SysCalcApp());
}

class SysCalcApp extends StatelessWidget {
  const SysCalcApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SYS CALC PW4',
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF1E1E1E),
        colorScheme: const ColorScheme.dark(
          primary: Colors.blueAccent,
          secondary: Colors.tealAccent,
        ),
      ),
      home: const Dashboard(),
    );
  }
}

class Dashboard extends StatefulWidget {
  const Dashboard({super.key});

  @override
  State<Dashboard> createState() => DashboardState();
}

class DashboardState extends State<Dashboard> {
  final tSm = TextEditingController();
  final tUnom = TextEditingController();
  final tIk = TextEditingController();
  final tTf = TextEditingController();
  final tCt = TextEditingController();
  final tJek = TextEditingController();

  final tSk = TextEditingController();
  final tUcn = TextEditingController();
  final tSnomt = TextEditingController();
  final tUk = TextEditingController();

  final tRcn = TextEditingController();
  final tXcn = TextEditingController();
  final tRcmin = TextEditingController();
  final tXcmin = TextEditingController();
  final tUvn = TextEditingController();
  final tUnn = TextEditingController();
  final tSnomt3 = TextEditingController();
  final tUkmax = TextEditingController();

  String consoleOut = "";

  void loadData() {
    setState(() {
      tSm.text = "1300";
      tUnom.text = "10";
      tIk.text = "2500";
      tTf.text = "2.5";
      tCt.text = "92";
      tJek.text = "1.4";

      tSk.text = "200";
      tUcn.text = "10.5";
      tSnomt.text = "6.3";
      tUk.text = "10.5";

      tRcn.text = "10.65";
      tXcn.text = "24.02";
      tRcmin.text = "34.88";
      tXcmin.text = "65.68";
      tUvn.text = "115";
      tUnn.text = "11";
      tSnomt3.text = "6.3";
      tUkmax.text = "11.1";
      consoleOut = "DATASET LOADED. READY.";
    });
  }

  double pVal(String s) {
    return double.tryParse(s) ?? 0.0;
  }

  void executeCalc() {
    double sm = pVal(tSm.text);
    double unom = pVal(tUnom.text);
    double ik = pVal(tIk.text);
    double tf = pVal(tTf.text);
    double ct = pVal(tCt.text);
    double jek = pVal(tJek.text);

    double im = (sm / 2) / (sqrt(3) * unom);
    double impa = 2 * im;
    double sek = im / jek;
    double smin = (ik * sqrt(tf)) / ct;

    double sk = pVal(tSk.text);
    double ucn = pVal(tUcn.text);
    double snomt = pVal(tSnomt.text);
    double uk = pVal(tUk.text);

    double xc = pow(ucn, 2) / sk;
    double xt = (uk / 100) * (pow(ucn, 2) / snomt);
    double xsum = xc + xt;
    double ip0 = ucn / (sqrt(3) * xsum);

    double rcn = pVal(tRcn.text);
    double xcn = pVal(tXcn.text);
    double rcmin = pVal(tRcmin.text);
    double xcmin = pVal(tXcmin.text);
    double uvn = pVal(tUvn.text);
    double unn = pVal(tUnn.text);
    double snomt3 = pVal(tSnomt3.text);
    double ukmax = pVal(tUkmax.text);

    double xt3 = (ukmax * pow(uvn, 2)) / (100 * snomt3);
    double rn = rcn;
    double xn = xcn + xt3;
    double rmin = rcmin;
    double xmin = xcmin + xt3;

    double kpr = pow(unn, 2) / pow(uvn, 2);
    double rshn = rn * kpr;
    double xshn = xn * kpr;
    double zshn = sqrt(pow(rshn, 2) + pow(xshn, 2));

    double rshmin = rmin * kpr;
    double xshmin = xmin * kpr;
    double zshmin = sqrt(pow(rshmin, 2) + pow(xshmin, 2));

    double ishn3 = (unn * 1000) / (sqrt(3) * zshn);
    double ishn2 = ishn3 * (sqrt(3) / 2);
    double ishmin3 = (unn * 1000) / (sqrt(3) * zshmin);
    double ishmin2 = ishmin3 * (sqrt(3) / 2);

    setState(() {
      consoleOut = "MODULE 1 RESULTS:\nIm: ${im.toStringAsFixed(2)} A\nImpa: ${impa.toStringAsFixed(2)} A\nSek: ${sek.toStringAsFixed(2)} mm2\nSmin: ${smin.toStringAsFixed(2)} mm2\n\nMODULE 2 RESULTS:\nXc: ${xc.toStringAsFixed(3)} Ohm\nXt: ${xt.toStringAsFixed(3)} Ohm\nXsum: ${xsum.toStringAsFixed(3)} Ohm\nIp0: ${ip0.toStringAsFixed(3)} kA\n\nMODULE 3 RESULTS:\nZsh.n: ${zshn.toStringAsFixed(3)} Ohm\nI(3)sh.n: ${ishn3.toStringAsFixed(0)} A\nI(2)sh.n: ${ishn2.toStringAsFixed(0)} A\n\nZsh.min: ${zshmin.toStringAsFixed(3)} Ohm\nI(3)sh.min: ${ishmin3.toStringAsFixed(0)} A\nI(2)sh.min: ${ishmin2.toStringAsFixed(0)} A";
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(">> SC NETWORK ANALYSIS", style: TextStyle(fontFamily: "Courier")),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: {
            const Text("MODULE 1 Cable Selection", style: TextStyle(color: Colors.orangeAccent)),
            const SizedBox(height: 10),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: {
                boxField("Sm", tSm),
                boxField("Unom", tUnom),
                boxField("Ik", tIk),
                boxField("tf", tTf),
                boxField("Ct", tCt),
                boxField("jek", tJek),
              }.toList(),
            ),
            const SizedBox(height: 20),
            const Text("MODULE 2 Short Circuit 10kV", style: TextStyle(color: Colors.orangeAccent)),
            const SizedBox(height: 10),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: {
                boxField("Sk", tSk),
                boxField("Ucn", tUcn),
                boxField("SnomT", tSnomt),
                boxField("Uk", tUk),
              }.toList(),
            ),
            const SizedBox(height: 20),
            const Text("MODULE 3 Substation Modes", style: TextStyle(color: Colors.orangeAccent)),
            const SizedBox(height: 10),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: {
                boxField("Rc.n", tRcn),
                boxField("Xc.n", tXcn),
                boxField("Rc.min", tRcmin),
                boxField("Xc.min", tXcmin),
                boxField("Uvn", tUvn),
                boxField("Unn", tUnn),
                boxField("SnomT3", tSnomt3),
                boxField("Ukmax", tUkmax),
              }.toList(),
            ),
            const SizedBox(height: 20),
            Row(
              children: {
                Expanded(
                  child: OutlinedButton(
                    onPressed: loadData,
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 20),
                      side: const BorderSide(color: Colors.grey),
                    ),
                    child: const Text("LOAD DATASET", style: TextStyle(color: Colors.white70)),
                  ),
                ),
                const SizedBox(width: 15),
                Expanded(
                  child: ElevatedButton(
                    onPressed: executeCalc,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blueAccent,
                      padding: const EdgeInsets.symmetric(vertical: 20),
                    ),
                    child: const Text("EXECUTE", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                  ),
                ),
              }.toList(),
            ),
            const SizedBox(height: 20),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(15),
              decoration: BoxDecoration(
                color: Colors.black45,
                border: Border.all(color: Colors.white24),
                borderRadius: BorderRadius.circular(5),
              ),
              child: Text(
                consoleOut.isEmpty ? "AWAITING INPUT..." : consoleOut,
                style: const TextStyle(
                  fontFamily: "Courier",
                  color: Colors.greenAccent,
                  fontSize: 14,
                ),
              ),
            ),
          }.toList(),
        ),
      ),
    );
  }

  Widget boxField(String label, TextEditingController ctrl) {
    return SizedBox(
      width: 150,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: {
          Text(label, style: const TextStyle(color: Colors.blueGrey, fontSize: 12)),
          const SizedBox(height: 5),
          TextField(
            controller: ctrl,
            keyboardType: TextInputType.number,
            style: const TextStyle(color: Colors.white),
            decoration: InputDecoration(
              filled: true,
              fillColor: const Color(0xFF2D2D2D),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(5), borderSide: BorderSide.none),
              contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 15),
            ),
          ),
        }.toList(),
      ),
    );
  }
}