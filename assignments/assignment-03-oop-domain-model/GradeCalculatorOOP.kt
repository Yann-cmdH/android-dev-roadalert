package assignments.oop

// ═══ Interface — définit le contrat de notation ═══
interface Gradable {
    fun calculateAverage(): Double
    fun getLetterGrade(): String
    fun isPassing(): Boolean
}

// ═══ Abstract class — entité de base avec propriétés communes ═══
abstract class Person(
    val id: String,
    val name: String
) {
    abstract fun displayInfo(): String

    override fun toString(): String = "$name (ID: $id)"
}

// ═══ Data class — modèle immuable pour un score ═══
data class Score(
    val subject: String,
    val value: Double,
    val weight: Double = 1.0
) {
    init {
        require(value in 0.0..100.0) { "Score must be between 0 and 100" }
        require(weight > 0) { "Weight must be positive" }
    }

    fun weightedValue(): Double = value * weight
}

// ═══ Sealed class — résultat d'évaluation (exhaustif dans when) ═══
sealed class EvaluationResult {
    data class Passed(val grade: String, val average: Double) : EvaluationResult()
    data class Failed(val average: Double, val deficit: Double) : EvaluationResult()
    data class Incomplete(val missingSubjects: List<String>) : EvaluationResult()

    fun message(): String = when (this) {
        is Passed -> "PASSED with grade $grade (average: ${"%.1f".format(average)})"
        is Failed -> "FAILED (average: ${"%.1f".format(average)}, needs ${"%.1f".format(deficit)} more points)"
        is Incomplete -> "INCOMPLETE — missing: ${missingSubjects.joinToString(", ")}"
    }
}

// ═══ Concrete class — Student hérite de Person, implémente Gradable ═══
class Student(
    id: String,
    name: String,
    private val scores: MutableList<Score> = mutableListOf()
) : Person(id, name), Gradable {

    // Visibility modifier — internal logic
    private fun totalWeight(): Double = scores.sumOf { it.weight }

    fun addScore(score: Score) {
        scores.add(score)
    }

    fun getScores(): List<Score> = scores.toList()  // defensive copy

    // ═══ Interface implementation ═══
    override fun calculateAverage(): Double {
        if (scores.isEmpty()) return 0.0
        val weightedSum = scores.sumOf { it.weightedValue() }
        return weightedSum / totalWeight()
    }

    override fun getLetterGrade(): String = when {
        calculateAverage() >= 90 -> "A"
        calculateAverage() >= 80 -> "B"
        calculateAverage() >= 70 -> "C"
        calculateAverage() >= 60 -> "D"
        else -> "F"
    }

    override fun isPassing(): Boolean = calculateAverage() >= 60.0

    // ═══ Abstract method implementation ═══
    override fun displayInfo(): String = buildString {
        appendLine("Student: $name (ID: $id)")
        appendLine("  Scores: ${scores.joinToString { "${it.subject}: ${it.value}" }}")
        appendLine("  Average: ${"%.1f".format(calculateAverage())}")
        appendLine("  Grade: ${getLetterGrade()} — ${if (isPassing()) "PASS" else "FAIL"}")
    }

    // ═══ Sealed class usage ═══
    fun evaluate(requiredSubjects: List<String>): EvaluationResult {
        val currentSubjects = scores.map { it.subject }
        val missing = requiredSubjects.filter { it !in currentSubjects }
        if (missing.isNotEmpty()) return EvaluationResult.Incomplete(missing)
        return if (isPassing()) {
            EvaluationResult.Passed(getLetterGrade(), calculateAverage())
        } else {
            EvaluationResult.Failed(calculateAverage(), 60.0 - calculateAverage())
        }
    }

    // ═══ Companion object — factory pattern ═══
    companion object {
        private var counter = 0
        fun createWithAutoId(name: String): Student {
            counter++
            return Student("STU-${counter.toString().padStart(3, '0')}", name)
        }
    }
}

// ═══ Extension function — ajoute fonctionnalité à List<Student> ═══
fun List<Student>.classAverage(): Double =
    if (isEmpty()) 0.0 else sumOf { it.calculateAverage() } / size

fun List<Student>.topStudents(n: Int = 3): List<Student> =
    sortedByDescending { it.calculateAverage() }.take(n)

fun List<Student>.passingRate(): Double =
    if (isEmpty()) 0.0 else count { it.isPassing() }.toDouble() / size * 100

// ═══ Higher-order function — traitement flexible ═══
fun processStudents(
    students: List<Student>,
    filter: (Student) -> Boolean = { true },
    action: (Student) -> Unit
) {
    students.filter(filter).forEach(action)
}

// ═══ Main — démonstration complète ═══
fun main() {
    println("═══ Grade Calculator — OOP Domain Model ═══\n")

    // Création via factory (companion object)
    val students = listOf(
        Student.createWithAutoId("Alice Nguemo").apply {
            addScore(Score("Math", 95.0, 2.0))
            addScore(Score("Physics", 88.0, 1.5))
            addScore(Score("English", 92.0))
        },
        Student.createWithAutoId("Bob Kamga").apply {
            addScore(Score("Math", 72.0, 2.0))
            addScore(Score("Physics", 65.0, 1.5))
            addScore(Score("English", 70.0))
        },
        Student.createWithAutoId("Charlie Fotso").apply {
            addScore(Score("Math", 45.0, 2.0))
            addScore(Score("Physics", 55.0, 1.5))
            addScore(Score("English", 50.0))
        },
        Student.createWithAutoId("Diana Mbeki").apply {
            addScore(Score("Math", 85.0, 2.0))
            // Missing Physics and English — incomplete
        }
    )

    // Polymorphisme — toutes les Person implémentent displayInfo()
    println("--- All Students ---")
    students.forEach { println(it.displayInfo()) }

    // Sealed class — évaluation exhaustive
    val required = listOf("Math", "Physics", "English")
    println("--- Evaluation Results ---")
    students.forEach { student ->
        val result = student.evaluate(required)
        println("${student.name}: ${result.message()}")
    }

    // Extension functions — statistiques de classe
    println("\n--- Class Statistics ---")
    println("Class average: ${"%.1f".format(students.classAverage())}")
    println("Passing rate: ${"%.0f".format(students.passingRate())}%")
    println("Top students: ${students.topStudents(2).joinToString { it.name }}")

    // Higher-order function — filtrage flexible
    println("\n--- Passing Students Only ---")
    processStudents(students, filter = { it.isPassing() }) { student ->
        println("  ${student.name}: ${student.getLetterGrade()}")
    }

    // Data class features — copy, destructuring
    println("\n--- Data Class Features ---")
    val originalScore = Score("Math", 85.0)
    val adjustedScore = originalScore.copy(value = 90.0)
    println("Original: $originalScore → Adjusted: $adjustedScore")

    val (subject, value, weight) = adjustedScore
    println("Destructured: subject=$subject, value=$value, weight=$weight")
}
