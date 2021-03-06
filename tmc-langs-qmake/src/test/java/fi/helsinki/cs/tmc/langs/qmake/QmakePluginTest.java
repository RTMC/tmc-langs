package fi.helsinki.cs.tmc.langs.qmake;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fi.helsinki.cs.tmc.langs.domain.ExerciseDesc;
import fi.helsinki.cs.tmc.langs.domain.RunResult;
import fi.helsinki.cs.tmc.langs.domain.TestDesc;
import fi.helsinki.cs.tmc.langs.utils.TestUtils;

import com.google.common.base.Optional;

import org.junit.Test;

import java.nio.file.Path;

public class QmakePluginTest {

    private final QmakePlugin qmakePlugin;

    public QmakePluginTest() {
        qmakePlugin = new QmakePlugin();
    }

    @Test
    public void testGetLanguageName() {
        assertEquals("qmake", qmakePlugin.getLanguageName());
    }

    @Test
    public void testQtTestsPassingWithNoLib() {
        assertEquals("Tests passing with no library",
                RunResult.Status.PASSED,
                runTests("passing_nolib"));
    }

    @Test
    public void testQtTestsPassingWithNoLibSamePoints() {
        assertEquals("Tests passing with no library and same points",
                RunResult.Status.PASSED,
                runTests("passing_nolib_same_point"));
    }

    @Test
    public void testQtTestsPassingWithMultipleLib() {
        assertEquals("Tests passing with multiple libraries",
                RunResult.Status.PASSED,
                runTests("passing_multiple_lib"));
    }

    @Test
    public void testQtBuildFailingWithSingleLib() {
        assertEquals("Student source not compiling with compiling single lib",
                RunResult.Status.COMPILE_FAILED,
                runTests("failing_compile_single_lib_compiling"));
    }

    @Test
    public void testQtFailingSingleLib() {
        assertEquals("Tests failing with single library",
                RunResult.Status.TESTS_FAILED,
                runTests("failing_single_lib"));
    }

    @Test
    public void testQtLibBuildFailing() {
        assertEquals("Student source compiling with library not compiling ",
                RunResult.Status.COMPILE_FAILED,
                runTests("failing_single_lib_not_compiling"));
    }

    @Test
    public void testQTestsFailingNoLib() {
        assertEquals("Tests fail with no library",
                RunResult.Status.TESTS_FAILED,
                runTests("failing_nolib"));
    }

    @Test
    public void testQtTestsPassingSingleLib() {
        assertEquals("Tests pass with single library",
                RunResult.Status.PASSED,
                runTests("passing_single_lib"));
    }

    @Test
    public void testQtTestsFailingNoLibSamePoints() {
        assertEquals("Failing suite test with no library",
                RunResult.Status.TESTS_FAILED,
                runTests("failing_nolib_same_point"));
    }

    @Test
    public void testQtTestsInvalidProFile() {
        assertEquals("Invalid .pro file does not compile",
                RunResult.Status.COMPILE_FAILED,
                runTests("invalid_pro_file"));
    }

    @Test
    public void testScanInvalidMakefileExercise() {
        Optional<ExerciseDesc> optional = scanExercise("makefile");
        assertFalse(optional.isPresent());
    }

    @Test
    public void testScanExerciseWithPassingSamePoints() {
        Optional<ExerciseDesc> optional = scanExercise("passing_single_lib");
        assertTrue(optional.isPresent());

        ExerciseDesc desc = optional.get();

        assertEquals("passing_single_lib", desc.name);

        assertEquals(2, desc.tests.size());
        TestDesc test1 = desc.tests.get(1);
        TestDesc test2 = desc.tests.get(0);

        assertEquals("test_function_one_here", test1.name);
        assertEquals("test_function_two_here", test2.name);

        assertEquals(1, test1.points.size());
        assertEquals("1", test1.points.get(0));

        assertEquals(1, test2.points.size());
        assertEquals("2", test2.points.get(0));
    }

    @Test
    public void testScanExerciseWithFailingSamePoints() {
        Optional<ExerciseDesc> optional = scanExercise("failing_single_lib_same_point");
        assertTrue(optional.isPresent());

        ExerciseDesc desc = optional.get();

        assertEquals("failing_single_lib_same_point", desc.name);

        assertEquals(3, desc.tests.size());
        TestDesc test2 = desc.tests.get(0);
        TestDesc test21 = desc.tests.get(1);
        TestDesc test1 = desc.tests.get(2);

        assertEquals("test_function_one_here", test1.name);
        assertEquals("test_function_two_here", test2.name);
        assertEquals("test_function_two_here_2", test21.name);

        assertEquals(1, test1.points.size());
        assertEquals(1, test2.points.size());
        assertEquals(1, test21.points.size());

        assertEquals("1", test1.points.get(0));
        assertEquals("2", test2.points.get(0));
        assertEquals("1", test21.points.get(0));
    }

    @Test
    public void testIsExerciseTypeCorrect() {
        assertTrue(isExerciseTypeCorrect("passing_nolib"));
        assertTrue(isExerciseTypeCorrect("passing_nolib_same_point"));
        assertTrue(isExerciseTypeCorrect("passing_single_lib"));
        assertTrue(isExerciseTypeCorrect("passing_single_lib_same_point"));

        assertTrue(isExerciseTypeCorrect("passing_multiple_lib"));
        assertTrue(isExerciseTypeCorrect("passing_multiple_lib_same_point"));

        assertTrue(isExerciseTypeCorrect("failing_nolib"));
        assertTrue(isExerciseTypeCorrect("failing_nolib_same_point"));
        assertTrue(isExerciseTypeCorrect("failing_single_lib"));
        assertTrue(isExerciseTypeCorrect("failing_single_lib_not_compiling"));
        assertTrue(isExerciseTypeCorrect("failing_single_lib_same_point"));
        assertFalse(isExerciseTypeCorrect("makefile"));
        assertFalse(isExerciseTypeCorrect(""));
    }

    @Test
    public void testScanExerciseWithNonexistentProject() {
        Optional<ExerciseDesc> optional = scanExercise("");
        assertFalse(optional.isPresent());
    }

    private RunResult.Status runTests(String testExercise) {
        return qmakePlugin.runTests(TestUtils.getPath(getClass(), testExercise)).status;
    }

    private Optional<ExerciseDesc> scanExercise(String testExercise) {
        return qmakePlugin.scanExercise(TestUtils.getPath(getClass(), testExercise), testExercise);
    }

    private boolean isExerciseTypeCorrect(String exercisePath) {
        Path path = TestUtils.getPath(getClass(), exercisePath);
        return qmakePlugin.isExerciseTypeCorrect(path);
    }
}
