package gradecalculator

import androidx.compose.animation.*
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

// ═══════════════════════════════════════════════════════════
// DATA MODELS — Week 1 & 2 concepts (data class, null safety, when)
// ═══════════════════════════════════════════════════════════

data class Score(
    val subject: String,
    val value: Double,
    val weight: Double = 1.0
) {
    fun weightedValue(): Double = value * weight
}

data class Student(
    val id: String,
    val name: String,
    val scores: List<Score> = emptyList()
) {
    fun calculateAverage(): Double {
        if (scores.isEmpty()) return 0.0
        val weightedSum = scores.sumOf { it.weightedValue() }
        val totalWeight = scores.sumOf { it.weight }
        return weightedSum / totalWeight
    }

    fun getLetterGrade(): String = when {
        calculateAverage() >= 90 -> "A"
        calculateAverage() >= 80 -> "B"
        calculateAverage() >= 70 -> "C"
        calculateAverage() >= 60 -> "D"
        else -> "F"
    }

    fun isPassing(): Boolean = calculateAverage() >= 60.0

    fun initials(): String = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
}

// ═══ Sealed class — Week 2.2 concept (état de navigation) ═══
sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    object AddStudent : Screen()
    object StudentList : Screen()
    data class StudentDetail(val studentId: String) : Screen()
}

// ═══ Sealed class — état d'évaluation (Week 2.2) ═══
sealed class EvaluationResult {
    data class Passed(val grade: String, val average: Double) : EvaluationResult()
    data class Failed(val average: Double, val deficit: Double) : EvaluationResult()
    data class Incomplete(val missing: List<String>) : EvaluationResult()

    fun message(): String = when (this) {
        is Passed -> "PASSED with grade $grade (average: ${"%.1f".format(average)})"
        is Failed -> "FAILED (average: ${"%.1f".format(average)}, needs ${"%.1f".format(deficit)} more)"
        is Incomplete -> "INCOMPLETE — missing: ${missing.joinToString(", ")}"
    }
}

// ═══ Extension functions — Week 2.1 concepts ═══
fun List<Student>.classAverage(): Double =
    if (isEmpty()) 0.0 else sumOf { it.calculateAverage() } / size

fun List<Student>.passingRate(): Double =
    if (isEmpty()) 0.0 else count { it.isPassing() }.toDouble() / size * 100

fun List<Student>.topStudents(n: Int = 3): List<Student> =
    sortedByDescending { it.calculateAverage() }.take(n)

// ═══════════════════════════════════════════════════════════
// VIEWMODEL — Week 3 concept (state management)
// ═══════════════════════════════════════════════════════════

class GradeViewModel {
    var currentScreen by mutableStateOf<Screen>(Screen.Login)
        private set

    var students by mutableStateOf(
        listOf(
            Student("STU-001", "Alice Nguemo", listOf(
                Score("Math", 95.0, 2.0), Score("Physics", 88.0, 1.5), Score("English", 92.0)
            )),
            Student("STU-002", "Bob Kamga", listOf(
                Score("Math", 72.0, 2.0), Score("Physics", 65.0, 1.5), Score("English", 70.0)
            )),
            Student("STU-003", "Charlie Fotso", listOf(
                Score("Math", 45.0, 2.0), Score("Physics", 55.0, 1.5), Score("English", 50.0)
            )),
            Student("STU-004", "Diana Mbeki", listOf(
                Score("Math", 85.0, 2.0), Score("Physics", 82.0, 1.5), Score("English", 88.0)
            ))
        )
    )
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    private var studentCounter = 4

    fun login(email: String, password: String): Boolean {
        if (email.isNotBlank() && password.isNotBlank()) {
            isLoggedIn = true
            currentScreen = Screen.Home
            return true
        }
        return false
    }

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun addStudent(name: String, mathScore: Double?, physicsScore: Double?, englishScore: Double?) {
        studentCounter++
        val id = "STU-${studentCounter.toString().padStart(3, '0')}"
        val scores = mutableListOf<Score>()
        mathScore?.let { scores.add(Score("Math", it, 2.0)) }
        physicsScore?.let { scores.add(Score("Physics", it, 1.5)) }
        englishScore?.let { scores.add(Score("English", it, 1.0)) }
        students = students + Student(id, name, scores)
        currentScreen = Screen.Home
    }

    fun deleteStudent(studentId: String) {
        students = students.filter { it.id != studentId }
        currentScreen = Screen.StudentList
    }

    fun getStudent(id: String): Student? = students.find { it.id == id }

    fun evaluate(student: Student): EvaluationResult {
        val required = listOf("Math", "Physics", "English")
        val current = student.scores.map { it.subject }
        val missing = required.filter { it !in current }
        if (missing.isNotEmpty()) return EvaluationResult.Incomplete(missing)
        return if (student.isPassing()) {
            EvaluationResult.Passed(student.getLetterGrade(), student.calculateAverage())
        } else {
            EvaluationResult.Failed(student.calculateAverage(), 60.0 - student.calculateAverage())
        }
    }
}

// ═══════════════════════════════════════════════════════════
// THEME COLORS
// ═══════════════════════════════════════════════════════════

val PrimaryBlue = Color(0xFF2E86C1)
val DarkText = Color(0xFF2C3E50)
val GrayText = Color(0xFF7F8C8D)
val LightBg = Color(0xFFF5F5F5)
val GradeA = Color(0xFF1E8449)
val GradeB = Color(0xFF2471A3)
val GradeC = Color(0xFFB7950B)
val GradeF = Color(0xFFC0392B)

fun gradeColor(grade: String): Color = when (grade) {
    "A" -> GradeA; "B" -> GradeB; "C" -> GradeC; "D" -> GradeC; else -> GradeF
}

fun gradeBgColor(grade: String): Color = when (grade) {
    "A" -> Color(0xFFD5F5E3); "B" -> Color(0xFFD6EAF8)
    "C" -> Color(0xFFFEF9E7); "D" -> Color(0xFFFEF9E7); else -> Color(0xFFFDEDEC)
}

// ═══════════════════════════════════════════════════════════
// SCREEN 1 — LOGIN
// ═══════════════════════════════════════════════════════════

@Composable
fun LoginScreen(viewModel: GradeViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Grade Calculator", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        Text("SE 3242 — ICT University", fontSize = 14.sp, color = GrayText)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it; error = "" },
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth(0.8f),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it; error = "" },
            label = { Text("Password") }, modifier = Modifier.fillMaxWidth(0.8f),
            singleLine = true, visualTransformation = PasswordVisualTransformation()
        )

        if (error.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(error, color = GradeF, fontSize = 12.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (!viewModel.login(email, password)) {
                    error = "Please enter email and password"
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) { Text("Sign In", modifier = Modifier.padding(vertical = 4.dp)) }

        Spacer(Modifier.height(16.dp))
        Text("Demo: any non-empty email/password", fontSize = 12.sp, color = Color(0xFFBDC3C7))
    }
}

// ═══════════════════════════════════════════════════════════
// SCREEN 2 — HOME DASHBOARD
// ═══════════════════════════════════════════════════════════

@Composable
fun HomeScreen(viewModel: GradeViewModel) {
    val students = viewModel.students

    Column(modifier = Modifier.fillMaxSize().background(LightBg)) {
        // Top bar
        Surface(color = PrimaryBlue, modifier = Modifier.fillMaxWidth()) {
            Text("Dashboard", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp))
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            // Stats cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Students", "${students.size}", Modifier.weight(1f))
                StatCard("Class Avg", "${"%.1f".format(students.classAverage())}", Modifier.weight(1f))
                StatCard("Pass Rate", "${"%.0f".format(students.passingRate())}%", Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Button(onClick = { viewModel.navigateTo(Screen.AddStudent) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("+ Add Student", modifier = Modifier.padding(vertical = 4.dp)) }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.navigateTo(Screen.StudentList) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("View All Students") }

            Spacer(Modifier.height(16.dp))

            // Top students
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Top Students", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)
                    Spacer(Modifier.height(8.dp))
                    students.topStudents(3).forEach { student ->
                        StudentRow(student) { viewModel.navigateTo(Screen.StudentDetail(student.id)) }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FB))) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
            Text(label, fontSize = 11.sp, color = GrayText)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SCREEN 3 — ADD STUDENT FORM (enrollment + validation)
// ═══════════════════════════════════════════════════════════

@Composable
fun AddStudentScreen(viewModel: GradeViewModel) {
    var name by remember { mutableStateOf("") }
    var mathStr by remember { mutableStateOf("") }
    var physicsStr by remember { mutableStateOf("") }
    var englishStr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // Live preview — null safety with ?.toDoubleOrNull()
    val mathScore = mathStr.toDoubleOrNull()
    val physicsScore = physicsStr.toDoubleOrNull()
    val englishScore = englishStr.toDoubleOrNull()

    val previewStudent = if (name.isNotBlank() && (mathScore != null || physicsScore != null || englishScore != null)) {
        val scores = mutableListOf<Score>()
        mathScore?.let { scores.add(Score("Math", it, 2.0)) }
        physicsScore?.let { scores.add(Score("Physics", it, 1.5)) }
        englishScore?.let { scores.add(Score("English", it, 1.0)) }
        Student("preview", name, scores)
    } else null

    Column(modifier = Modifier.fillMaxSize().background(LightBg)) {
        Surface(color = PrimaryBlue, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                TextButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Text("< Back", color = Color.White)
                }
                Text("Add Student", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Student Info", fontWeight = FontWeight.Bold, color = DarkText)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it; error = "" },
                        label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Spacer(Modifier.height(12.dp))
                    Text("Scores (0-100)", fontWeight = FontWeight.Bold, color = DarkText)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(value = mathStr, onValueChange = { mathStr = it },
                        label = { Text("Math (weight x2.0)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = physicsStr, onValueChange = { physicsStr = it },
                        label = { Text("Physics (weight x1.5)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = englishStr, onValueChange = { englishStr = it },
                        label = { Text("English (weight x1.0)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    // Live preview — demonstrates null safety
                    previewStudent?.let { student ->
                        Spacer(Modifier.height(12.dp))
                        val color = if (student.isPassing()) GradeA else GradeF
                        Text(
                            "Average: ${"%.1f".format(student.calculateAverage())} — Grade: ${student.getLetterGrade()} — ${if (student.isPassing()) "PASS" else "FAIL"}",
                            color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                    }

                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = GradeF, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            when {
                                name.isBlank() -> error = "Name is required"
                                mathScore == null && physicsScore == null && englishScore == null -> error = "At least one score is required"
                                (mathScore != null && mathScore !in 0.0..100.0) ||
                                (physicsScore != null && physicsScore !in 0.0..100.0) ||
                                (englishScore != null && englishScore !in 0.0..100.0) -> error = "Scores must be between 0 and 100"
                                else -> viewModel.addStudent(name, mathScore, physicsScore, englishScore)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) { Text("Save Student", modifier = Modifier.padding(vertical = 4.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SCREEN 4 — STUDENT LIST (LazyColumn + filter + navigation)
// ═══════════════════════════════════════════════════════════

@Composable
fun StudentListScreen(viewModel: GradeViewModel) {
    var searchQuery by remember { mutableStateOf("") }

    // filter — Week 2.1 concept
    val filtered = viewModel.students.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(LightBg)) {
        Surface(color = PrimaryBlue, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                TextButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                    Text("< Back", color = Color.White)
                }
                Text("Students (${viewModel.students.size})", color = Color.White,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                label = { Text("Search students...") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(12.dp)) {
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No students found", color = GrayText)
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(filtered) { student ->
                            StudentRow(student) { viewModel.navigateTo(Screen.StudentDetail(student.id)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentRow(student: Student, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with initials
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(gradeColor(student.getLetterGrade())),
            contentAlignment = Alignment.Center
        ) { Text(student.initials(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(student.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
            Text("Avg: ${"%.1f".format(student.calculateAverage())}", fontSize = 11.sp, color = GrayText)
        }

        // Grade badge
        Box(
            modifier = Modifier.background(gradeBgColor(student.getLetterGrade()), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(student.getLetterGrade(), fontWeight = FontWeight.Bold,
                color = gradeColor(student.getLetterGrade()), fontSize = 13.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SCREEN 5 — STUDENT DETAIL (navigation args + sealed class)
// ═══════════════════════════════════════════════════════════

@Composable
fun StudentDetailScreen(viewModel: GradeViewModel, studentId: String) {
    val student = viewModel.getStudent(studentId)

    if (student == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Student not found", color = GradeF)
        }
        return
    }

    val result = viewModel.evaluate(student)

    Column(modifier = Modifier.fillMaxSize().background(LightBg)) {
        Surface(color = PrimaryBlue, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                TextButton(onClick = { viewModel.navigateTo(Screen.StudentList) }) {
                    Text("< Back", color = Color.White)
                }
                Text(student.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            // Profile card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape)
                        .background(gradeColor(student.getLetterGrade())),
                        contentAlignment = Alignment.Center) {
                        Text(student.initials(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(student.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
                    Text("ID: ${student.id}", fontSize = 12.sp, color = GrayText)
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.background(gradeBgColor(student.getLetterGrade()), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("${student.getLetterGrade()} — ${"%.1f".format(student.calculateAverage())}%",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            color = gradeColor(student.getLetterGrade()))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Scores card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Scores", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)
                    Spacer(Modifier.height(8.dp))
                    student.scores.forEach { score ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("${score.subject} (x${score.weight})", color = GrayText, fontSize = 13.sp,
                                modifier = Modifier.weight(1f))
                            Text("${score.value}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                        }
                        Divider(color = Color(0xFFF0F0F0))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Evaluation result — sealed class usage
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val resultColor = when (result) {
                        is EvaluationResult.Passed -> GradeA
                        is EvaluationResult.Failed -> GradeF
                        is EvaluationResult.Incomplete -> Color(0xFFB7950B)
                    }
                    Text(result.message(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = resultColor)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Delete button
            Button(onClick = { viewModel.deleteStudent(student.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GradeF)
            ) { Text("Delete Student", modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// MAIN — App entry point + Navigation router
// ═══════════════════════════════════════════════════════════

@Composable
fun App() {
    val viewModel = remember { GradeViewModel() }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryBlue,
            onPrimary = Color.White,
            surface = Color.White,
            background = LightBg
        )
    ) {
        // Navigation — sealed class Screen drives which composable shows
        when (val screen = viewModel.currentScreen) {
            is Screen.Login -> LoginScreen(viewModel)
            is Screen.Home -> HomeScreen(viewModel)
            is Screen.AddStudent -> AddStudentScreen(viewModel)
            is Screen.StudentList -> StudentListScreen(viewModel)
            is Screen.StudentDetail -> StudentDetailScreen(viewModel, screen.studentId)
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Grade Calculator — SE 3242",
        state = rememberWindowState(width = 420.dp, height = 750.dp)
    ) {
        App()
    }
}
