package edu.illinois.cs.cs125.jeed.core

import io.kotlintest.Matcher
import io.kotlintest.MatcherResult
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNot
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class TestCheckstyle : StringSpec({
    "should check strings without errors" {
        val checkstyleResult = Source.fromSnippet("""
int i = 0;
""".trim()).checkstyle()

        checkstyleResult shouldNot haveCheckstyleErrors()
    }
    "it should identify checkstyle errors in strings" {
        val checkstyleErrors = Source.fromSnippet("""
int i = 0;
int y =1;
""".trim()).checkstyle()

        checkstyleErrors should haveCheckstyleErrors()
        checkstyleErrors should haveCheckstyleErrorAt(line = 2)
    }
    "should identify checkstyle errors in snippet results" {
        val checkstyleErrors = Source.fromSnippet("""
int i = 0;
int y = 1;
int add(int a, int b) {
    return a+ b;
}
add(i, y);
""".trim()).checkstyle()

        checkstyleErrors should haveCheckstyleErrors()
        checkstyleErrors should haveCheckstyleErrorAt(line = 4)
    }
    "should identify checkstyle errors in snippet static results" {
        val checkstyleErrors = Source.fromSnippet("""
int i = 0;
int y = 1;
static int add(int a, int b) {
    return a+ b;
}
add(i, y);
""".trim()).checkstyle()

        checkstyleErrors should haveCheckstyleErrors()
        checkstyleErrors should haveCheckstyleErrorAt(line = 4)
    }
    "should identify checkstyle errors in snippet results with modifiers" {
        val checkstyleErrors = Source.fromSnippet("""
int i = 0;
int y = 1;
public int add(int a, int b) {
    return a+ b;
}
add(i, y);
""".trim()).checkstyle()

        checkstyleErrors should haveCheckstyleErrors()
        checkstyleErrors should haveCheckstyleErrorAt(line = 4)
    }
    "should check all sources by default" {
        val checkstyleErrors = Source(mapOf(
                "First.java" to """
public class First{
}
                """.trim(),
                "Second.java" to """
public class Second {
}
                """.trim()
        )).checkstyle()

        checkstyleErrors should haveCheckstyleErrors()
        checkstyleErrors.errors shouldHaveSize 1
        checkstyleErrors should haveCheckstyleErrorAt(source = "First.java", line = 1)
    }
    "should ignore sources not configured to check" {
        val checkstyleErrors = Source(mapOf(
                "First.java" to """
public class First{
}
                """.trim(),
                "Second.java" to """
public class Second {
}
                """.trim()
        )).checkstyle(CheckstyleArguments(sources = setOf("Second.java")))

        checkstyleErrors shouldNot haveCheckstyleErrors()
    }
    "should check indentation properly" {
        val checkstyleErrors = Source.fromSnippet("""
public int add(int a, int b) {
   return a + b;
 }
""".trim()).checkstyle()
        checkstyleErrors should haveCheckstyleErrors()
        checkstyleErrors.errors shouldHaveSize 2
        checkstyleErrors should haveCheckstyleErrorAt(line = 2)
        checkstyleErrors should haveCheckstyleErrorAt(line = 3)
    }
    "should throw when configured" {
    val checkstyleError = shouldThrow<CheckstyleFailed> {
            Source.fromSnippet("""
public int add(int a,int b) {
    return a+ b;
}
""".trim()).checkstyle(CheckstyleArguments(failOnError = true))
        }
        checkstyleError.errors shouldHaveSize 2
        checkstyleError.errors[0].location.line shouldBe 1
        checkstyleError.errors[1].location.line shouldBe 2
    }
    // TODO: Update if and when checkstyle supports switch expressions
    "!should not fail on new Java features" {
        val checkstyleResult = Source(mapOf(
                "Test.java" to """
public class Test {
    public static String testYieldKeyword(int switchArg) {
        return switch (switchArg) {
            case 1, 2: yield "works";
            case 3: yield "oh boy";
            default: yield "testing";
        };
    }
    public static void main() {
        System.out.println(testYieldKeyword(1));
    }
}
                """.trim()
        )).checkstyle()

        checkstyleResult shouldNot haveCheckstyleErrors()
    }
})

fun haveCheckstyleErrors() = object : Matcher<CheckstyleResults> {
    override fun test(value: CheckstyleResults): MatcherResult {
        return MatcherResult(value.errors.isNotEmpty(),
                "should have checkstyle errors",
                "should not have checkstyle errors")
    }
}
fun haveCheckstyleErrorAt(source: String = SNIPPET_SOURCE, line: Int) = object : Matcher<CheckstyleResults> {
    override fun test(value: CheckstyleResults): MatcherResult {
        return MatcherResult(value.errors.any { it.location.source == source && it.location.line == line },
                "should have checkstyle error on line $line",
                "should not have checkstyle error on line $line")
    }
}
