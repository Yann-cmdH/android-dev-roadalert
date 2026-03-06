// Data class (objet principal)
data class Student(val name: String, val score: Double) {

    // Fonction 1 : vérifier si la note est valide
    fun isValidScore(): Boolean {
        return score >= 0 && score <= 100
    }

    // Fonction 2 : calculer la lettre de grade
    fun gradeLetter(): String {
        return when {
            score >= 90 -> "A"
            score >= 80 -> "B"
            score >= 70 -> "C"
            score >= 60 -> "D"
            else -> "F"
        }
    }
}

// Fonction d'ordre supérieur (reçoit une fonction)
fun processStudents(
    students: List<Student>,
    operation: (Student) -> Unit
) {
    for (student in students) {
        operation(student)
    }
}

fun main() {

    // Collection d'objets Student
    val students = listOf(
        Student("Alice", 95.0),
        Student("Bob", 72.0),
        Student("Charlie", 58.0),
        Student("David", 84.0)
    )

    // Utilisation d'une collection operation (filter)
    val passedStudents = students.filter { it.score >= 60 }

    println("Students who passed:")
    passedStudents.forEach {
        println("${it.name} - Grade: ${it.gradeLetter()}")
    }

    println()

    //  Lambda passée à une fonction d'ordre supérieur
    println("Processing students:")
    processStudents(students) { student ->
        println("${student.name} : ${student.score} -> ${student.gradeLetter()}")
    }
}
