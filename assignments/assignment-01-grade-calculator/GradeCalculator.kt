data class Student(
    val name: String,
    val score1: Double?,
    val score2: Double?,
    val score3: Double?
)

fun main() {
    println("=== Student Grade Calculator ===")

    print("Enter student name: ")
    val name: String? = readLine()

    print("Enter score 1: ")
    val s1: Double? = readLine()?.toDoubleOrNull()

    print("Enter score 2: ")
    val s2: Double? = readLine()?.toDoubleOrNull()

    print("Enter score 3: ")
    val s3: Double? = readLine()?.toDoubleOrNull()

    // Create Student object
    val student = Student(
        name = name ?: "Unknown",
        score1 = s1,
        score2 = s2,
        score3 = s3
    )

    // Elvis operator for null safety
    val score1 = student.score1 ?: 0.0
    val score2 = student.score2 ?: 0.0
    val score3 = student.score3 ?: 0.0

    val average: Double = (score1 + score2 + score3) / 3

    // when expression for grade
    val grade = when {
        average >= 90 -> "A"
        average >= 80 -> "B"
        average >= 70 -> "C"
        average >= 60 -> "D"
        else          -> "F"
    }

    val status = if (average >= 60) "Pass" else "Fail"

    // Output
    println("\nStudent : ${student.name}")
    println("Average : ${"%.2f".format(average)}")
    println("Grade   : $grade")
    println("Status  : $status")
}
