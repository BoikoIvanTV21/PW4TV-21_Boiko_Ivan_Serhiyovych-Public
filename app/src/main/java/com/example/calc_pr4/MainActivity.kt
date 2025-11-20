package com.example.calc_pr4

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.math.max

// MainActivity.kt - оновлений повний код, що відповідає вимогам ПР4
// Функціонал:
// - розрахунок струмів КЗ (Iк(3ф) для режимів: основний/норм/мін/авар; Iк(1ф) по R,X)
// - термічна перевірка (розрахунок S_min) і підбір стандартного перерізу
// - динамічна перевірка (I_dyn та порівняння з механічною витривалістю перерізу)
// - вибір кабелю з таблиці (типові кабелі) та перевірка обраного кабелю
// - кнопка автозаповнення (варіант 6) з реалістичними значеннями
// - зручний вивід результатів по секціях
// - кнопка відкриття локального PDF з завданням (шлях заданий у ресурсі)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PR4App() }
    }
}

data class CableType(
    val name: String,
    val crossSection: Int, // мм²
    val I_continuous: Int  // допустимий тривалий струм, A (прибл.)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PR4App() {
    val ctx = LocalContext.current

    // Вхідні поля (строки, щоб дозволити автозаповнення запитом користувача)
    var U_kV by remember { mutableStateOf("10") } // кВ
    var S_kz_MVA by remember { mutableStateOf("400") } // MVA - приклад ГПП
    var R_line by remember { mutableStateOf("1.25") } // Ом
    var X_line by remember { mutableStateOf("2.6") } // Ом

    var S_kz_norm by remember { mutableStateOf("350") }
    var S_kz_min by remember { mutableStateOf("250") }
    var S_kz_av by remember { mutableStateOf("180") }

    // Перевірки
    var t_k by remember { mutableStateOf("1.0") } // с
    var k_therm by remember { mutableStateOf("115") } // A·s^0.5/mm² (адіабатична)
    var k_dyn by remember { mutableStateOf("1.8") } // коефіцієнт динаміки
    // Механічна константа для оцінки I_allow = k_mech * S
    val k_mech = 60.0 // A/mm², спрощено

    // Список типових кабелів (реалістичні приклади)
    val cableCatalog = listOf(
        CableType("ААШв 3×120 мм²", 120, 240),
        CableType("АПвПу 3×95 мм²", 95, 200),
        CableType("ВВГ 3×70 мм²", 70, 160),
        CableType("ВВГ 3×50 мм²", 50, 120),
        CableType("ВВГ 3×35 мм²", 35, 100),
        CableType("ВВГ 3×25 мм²", 25, 80)
    )

    // Вибраний кабель і опціональний ручний переріз
    var selectedCableIndex by remember { mutableStateOf(-1) }
    var manualS by remember { mutableStateOf("") } // якщо користувач вводить власний переріз

    // Output
    var outputText by remember { mutableStateOf("") }

    // Стандартні перерізи
    val standardSizes = listOf(16, 25, 35, 50, 70, 95, 120, 150, 185, 240)

    // Автозаповнення — варіант 6 (реалістичні значення)
    fun autoFillVariant6() {
        U_kV = "10"
        S_kz_MVA = "400"
        R_line = "1.25"
        X_line = "2.6"
        S_kz_norm = "350"
        S_kz_min = "250"
        S_kz_av = "180"
        t_k = "1.0"
        k_therm = "115"
        k_dyn = "1.8"
        // Виберемо реалістичний кабель з каталогу (95 мм²)
        selectedCableIndex = cableCatalog.indexOfFirst { it.crossSection == 95 }
        manualS = ""
    }

    fun parseDoubleSafe(s: String, default: Double = 0.0): Double {
        return try {
            s.replace(',', '.').toDouble()
        } catch (e: Exception) {
            default
        }
    }

    fun suggestStandardSize(sMin: Double): Int {
        for (s in standardSizes) {
            if (s.toDouble() >= sMin) return s
        }
        return standardSizes.last()
    }

    // Основні розрахунки
    fun calculateAll() {
        val U = parseDoubleSafe(U_kV)
        val Sgp = parseDoubleSafe(S_kz_MVA)
        val R = parseDoubleSafe(R_line)
        val X = parseDoubleSafe(X_line)
        val Snorm = parseDoubleSafe(S_kz_norm)
        val Smin = parseDoubleSafe(S_kz_min)
        val Sav = parseDoubleSafe(S_kz_av)
        val tk = max(parseDoubleSafe(t_k), 1e-6)
        val ktherm = parseDoubleSafe(k_therm)
        val kdyn = parseDoubleSafe(k_dyn)

        // якщо обраний кабель — беремо його переріз, або ручний ввод
        val chosenSmm2: Double? = if (manualS.isNotBlank()) {
            parseDoubleSafe(manualS).takeIf { it > 0.0 }
        } else {
            if (selectedCableIndex in cableCatalog.indices) cableCatalog[selectedCableIndex].crossSection.toDouble() else null
        }

        val sqrt3 = sqrt(3.0)
        fun Ik3_from_S_MVA(Smva: Double, Ukv: Double): Double {
            if (Ukv <= 0.0) return 0.0
            // I = S(MVA)*1e6 / (sqrt(3) * U(kV)*1e3) = S*1000 / (sqrt3 * U)
            return (Smva * 1000.0) / (sqrt3 * Ukv)
        }

        val Ik3_main = Ik3_from_S_MVA(Sgp, U)
        val Ik3_norm = Ik3_from_S_MVA(Snorm, U)
        val Ik3_min = Ik3_from_S_MVA(Smin, U)
        val Ik3_av = Ik3_from_S_MVA(Sav, U)

        // Однофазний струм через імпеданс лінії (фаза-земля): Uф = Uл *1000 / sqrt3
        val U_phase_V = U * 1000.0 / sqrt3
        val Z_line = sqrt(R * R + X * X)
        val Ik1 = if (Z_line <= 0.0) 0.0 else U_phase_V / Z_line

        // Термічна перевірка: S_min = I_k * sqrt(t_k) / k_therm
        fun SminTherm(Ik: Double): Double {
            return if (ktherm <= 0.0) 0.0 else Ik * sqrt(tk) / ktherm
        }

        val Smin_main = SminTherm(Ik3_main)
        val Smin_norm = SminTherm(Ik3_norm)
        val Smin_min = SminTherm(Ik3_min)
        val Smin_av = SminTherm(Ik3_av)

        // Динамічна перевірка: I_dyn = k_dyn * I_k ; I_allow_mech = k_mech * S
        fun dynamicCheck(Ik: Double, Smm2: Double): Pair<Double, Boolean> {
            val I_dyn = kdyn * Ik
            val I_allow = k_mech * Smm2
            return Pair(I_dyn, I_allow >= I_dyn)
        }

        // Формуємо текст результатів
        val sb = StringBuilder()
        sb.append("Результати розрахунку — ПР4\n\n")
        sb.append("Вхідні дані:\n")
        sb.append("U = ${U} кВ\n")
        sb.append("Sкз (ГПП приклад) = ${Sgp} MVA\n")
        sb.append("R лінії = ${R} Ом, X лінії = ${X} Ом\n")
        sb.append("Режими Sкз (MVA): нормальний=${Snorm}, мінімальний=${Smin}, аварійний=${Sav}\n")
        sb.append("t_k = ${tk} с, k_терм = ${ktherm}, k_dyn = ${kdyn}\n\n")

        sb.append("=== Струми КЗ ===\n")
        sb.append("Iк(3ф) (ГПП) = ${"%,.2f".format(Ik3_main)} A\n")
        sb.append("Iк(3ф) нормальний = ${"%,.2f".format(Ik3_norm)} A\n")
        sb.append("Iк(3ф) мінімальний = ${"%,.2f".format(Ik3_min)} A\n")
        sb.append("Iк(3ф) аварійний = ${"%,.2f".format(Ik3_av)} A\n")
        sb.append("Iк(1ф) (фаза-земля) = ${"%,.2f".format(Ik1)} A\n\n")

        sb.append("=== Термічна перевірка (мін. переріз S_min, мм²) ===\n")
        fun appendSminLabel(label: String, Ik: Double, Smin: Double) {
            val recommendedStd = suggestStandardSize(Smin)
            sb.append("$label: Iк = ${"%,.2f".format(Ik)} A → S_min(calc) = ${"%,.2f".format(Smin)} мм² → рекомендований стандарт = ${recommendedStd} мм²\n")
        }
        appendSminLabel("ГПП (приклад)", Ik3_main, Smin_main)
        appendSminLabel("Нормальний режим", Ik3_norm, Smin_norm)
        appendSminLabel("Мінімальний режим", Ik3_min, Smin_min)
        appendSminLabel("Аварійний режим", Ik3_av, Smin_av)
        sb.append("\n")

        // Інформація по вибраному кабелю
        if (chosenSmm2 != null) {
            val chosenName = if (selectedCableIndex in cableCatalog.indices) cableCatalog[selectedCableIndex].name else "Введений вручну"
            sb.append("=== Перевірка обраного кабелю ===\n")
            sb.append("Кабель: $chosenName, S = ${"%.2f".format(chosenSmm2)} мм²\n")
            val okThermMain = chosenSmm2 >= Smin_main
            sb.append("Термічна (за ГПП): S_chosen >= S_min ? ${if (okThermMain) "PASS" else "FAIL"} (S_min=${"%,.2f".format(Smin_main)} мм²)\n")
            val (I_dyn_main, okDynMain) = dynamicCheck(Ik3_main, chosenSmm2)
            sb.append("Динамічна (прибл.): I_dyn = ${"%,.2f".format(I_dyn_main)} A; I_allow = ${"%,.2f".format(k_mech * chosenSmm2)} A → ${if (okDynMain) "PASS" else "FAIL"}\n\n")
        } else {
            sb.append("Обраний переріз кабелю не заданий — виберіть кабель з каталогу або введіть вручну переріз.\n\n")
        }

        sb.append("=== Динамічна інформація по режимах (I_dyn = k_dyn * Iк) ===\n")
        fun appendDynLabel(label: String, Ik: Double) {
            val I_dyn = kdyn * Ik
            sb.append("$label: Iк = ${"%,.2f".format(Ik)} A → I_dyn = ${"%,.2f".format(I_dyn)} A\n")
        }
        appendDynLabel("ГПП (приклад)", Ik3_main)
        appendDynLabel("Нормальний", Ik3_norm)
        appendDynLabel("Мінімальний", Ik3_min)
        appendDynLabel("Аварійний", Ik3_av)

        sb.append("\nПримітки:\n")
        sb.append("1) Формули адаптовані з методички ПР4 (Приклади 7.1, 7.2, 7.4).\n")
        sb.append("2) Константи k_терм, k_dyn, k_mech — типовi; при необхідності змініть згідно методички.\n")
        sb.append("3) Підбір стандартного перерізу виконується як найменший стандарт ≥ S_min.\n")

        outputText = sb.toString()
    }

    // UI
    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text("ПР4 — Калькулятор струмів КЗ") })
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Кнопки дій
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { autoFillVariant6() }) {
                    Text("Автозаповнення (варіант 6)")
                }
                Button(onClick = { calculateAll() }) {
                    Text("Розрахувати")
                }
                Spacer(modifier = Modifier.weight(1f))
                // Кнопка відкриття локального PDF (шлях з історії): /mnt/data/_РПЗМП Практична робота 4 v_2024_11_07.pdf
                Button(onClick = {
                    val pdfPath = "/mnt/data/_РПЗМП Практична робота 4 v_2024_11_07.pdf"
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(Uri.parse("file://$pdfPath"), "application/pdf")
                        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        ctx.startActivity(intent)
                    } catch (e: Exception) {
                        // якщо не можна відкрити локально - нічого не падає
                    }
                }) {
                    Text("Відкрити завдання (ПР4)")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Вхідні дані - блоки
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Вхідні параметри мережі", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = U_kV, onValueChange = { U_kV = it }, label = { Text("U, кВ") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = S_kz_MVA, onValueChange = { S_kz_MVA = it }, label = { Text("Sкз (ГПП), MVA") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = R_line, onValueChange = { R_line = it }, label = { Text("R лінії, Ом") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = X_line, onValueChange = { X_line = it }, label = { Text("X лінії, Ом") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Режими (Sкз), MVA", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = S_kz_norm, onValueChange = { S_kz_norm = it }, label = { Text("Sкз нормальний, MVA") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = S_kz_min, onValueChange = { S_kz_min = it }, label = { Text("Sкз мінімальний, MVA") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = S_kz_av, onValueChange = { S_kz_av = it }, label = { Text("Sкз аварійний, MVA") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Параметри перевірок", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = t_k, onValueChange = { t_k = it }, label = { Text("t_k, с (тривалість КЗ)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = k_therm, onValueChange = { k_therm = it }, label = { Text("k_терм (адіабатична константа)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = k_dyn, onValueChange = { k_dyn = it }, label = { Text("k_dyn (коеф. динаміки)") }, modifier = Modifier.fillMaxWidth())
                    Text("k_mech (механічна константа, використовується в розрахунку динаміки) = ${k_mech} A/mm²", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Вибір кабелю з каталогу
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Каталог типових кабелів", style = MaterialTheme.typography.titleMedium)
                    cableCatalog.forEachIndexed { idx, cable ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedCableIndex == idx),
                                    onClick = { selectedCableIndex = idx; manualS = "" }
                                )
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (selectedCableIndex == idx), onClick = { selectedCableIndex = idx; manualS = "" })
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("${cable.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("S = ${cable.crossSection} мм²; Iдоп ≈ ${cable.I_continuous} A", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text("Або введіть вручну переріз (мм²):", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = manualS, onValueChange = {
                        manualS = it
                        if (it.isNotBlank()) selectedCableIndex = -1
                    }, label = { Text("Обраний переріз, мм² (опційно)") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(12.dp))

            // Результат — в окремих картинах для кращої читабельності
            if (outputText.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Результати", style = MaterialTheme.typography.titleLarge)
                        // Покажемо розділений текст, щоб було зручно
                        val lines = outputText.split("\n")
                        lines.forEach { ln ->
                            Text(ln, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Натисніть 'Розрахувати' для отримання результатів", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
