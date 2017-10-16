package fi.helsinki.cs.tmc.langs.r;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import fi.helsinki.cs.tmc.langs.domain.RunResult;
import fi.helsinki.cs.tmc.langs.domain.TestDesc;
import fi.helsinki.cs.tmc.langs.domain.TestResult;
import fi.helsinki.cs.tmc.langs.io.StudentFilePolicy;
import fi.helsinki.cs.tmc.langs.utils.TestUtils;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RPluginTest {

    private RPlugin plugin;

    private Path simpleAllTestsPassProject;
    private Path simpleSomeTestsFailProject;
    private Path simpleSourceCodeErrorProject;

    @Before
    public void setUp() {
        plugin = new RPlugin();

        simpleAllTestsPassProject = TestUtils.getPath(getClass(),
                "simple_all_tests_pass");
        simpleSomeTestsFailProject = TestUtils.getPath(getClass(),
                "simple_some_tests_fail");
        simpleSourceCodeErrorProject = TestUtils.getPath(getClass(),
                "simple_source_code_error");
    }

    @After
    public void tearDown() {
        removeAvailablePointsJson(simpleAllTestsPassProject);
        removeResultsJson(simpleAllTestsPassProject);
        removeResultsJson(simpleSomeTestsFailProject);
        removeResultsJson(simpleSourceCodeErrorProject);
    }

    private void removeResultsJson(Path projectPath) {
        try {
            Files.deleteIfExists(projectPath.resolve(".results.json"));
        } catch (IOException e) {
            System.out.println("Something wrong: " + e.getMessage());
        }
    }

    private void removeAvailablePointsJson(Path projectPath) {
        try {
            Files.deleteIfExists(projectPath.resolve(".available_points.json"));
        } catch (IOException e) {
            System.out.println("Something wrong: " + e.getMessage());
        }
    }

    private void testResultAsExpected(TestResult result, boolean successful, String name,
                                      String[] points) {
        assertEquals(successful, result.isSuccessful());
        assertEquals(name, result.getName());
        assertArrayEquals(points, result.points.toArray());
    }

    @Test
    public void testScanExercise() {
        plugin.scanExercise(simpleAllTestsPassProject, "main.R");
        assertTrue(Files.exists(simpleAllTestsPassProject.resolve(".available_points.json")));
    }

    @Test
    public void testScanExerciseInTheWrongPlace() throws IOException {
        plugin.scanExercise(simpleAllTestsPassProject, "ar.R");
        Path availablePointsJson = simpleAllTestsPassProject.resolve(".available_points.json");
        ImmutableList<TestDesc> re = null;
        try {
            re = new RExerciseDescParser(availablePointsJson).parse();
        } catch (IOException e) {
            // Expected outcome
        }
        assertTrue(re == null);
    }

    @Test
    public void runTestsRunResultAsExpectedSimpleAllPass() {
        RunResult result = plugin.runTests(simpleAllTestsPassProject);

        assertEquals(RunResult.Status.PASSED, result.status);

        ImmutableList<TestResult> results = result.testResults;
        testResultAsExpected(results.get(0), true,
                "ret_true works.", new String[]{"r1", "r1.1"});
        testResultAsExpected(results.get(1), true,
                "ret_one works.", new String[]{"r1", "r1.2"});
        testResultAsExpected(results.get(2), true,
                "add works.", new String[]{"r1", "r1.3", "r1.4"});
        testResultAsExpected(results.get(3), true,
                "minus works", new String[]{"r2", "r2.1"});
    }

    @Test
    public void runTestsRunResultAsExpectedSimpleSomeFail() {
        RunResult result = plugin.runTests(simpleSomeTestsFailProject);

        assertEquals(RunResult.Status.TESTS_FAILED, result.status);

        ImmutableList<TestResult> results = result.testResults;
        testResultAsExpected(results.get(0), true,
                "ret_true works.", new String[]{"r1", "r1.1"});
        testResultAsExpected(results.get(1), true,
                "ret_one works.", new String[]{"r1", "r1.2"});
        testResultAsExpected(results.get(2), true,
                "add works.", new String[]{"r1", "r1.3", "r1.4"});
        testResultAsExpected(results.get(3), false,
                "ret_false returns true", new String[]{"r1", "r1.5"});
        testResultAsExpected(results.get(4), true,
                "ret_true works but there are no points.", new String[]{"r1"});
    }

    @Test
    public void runTestsCreatesRunResultWithCorrectStatusWhenSourceCodeHasError() {
        RunResult res = plugin.runTests(simpleSourceCodeErrorProject);

        assertEquals(RunResult.Status.COMPILE_FAILED, res.status);
    }

    @Test
    public void exerciseIsCorrectTypeIfItContainsRFolder() {
        Path testCasesRoot = TestUtils.getPath(getClass(), "recognition_test_cases");
        Path project = testCasesRoot.resolve("R_folder");

        assertTrue(plugin.isExerciseTypeCorrect(project));
    }

    @Test
    public void exerciseIsCorrectTypeIfItContainsTestthatFolder() {
        Path testCasesRoot = TestUtils.getPath(getClass(), "recognition_test_cases");
        Path project = testCasesRoot.resolve("testthat_folder");

        assertTrue(plugin.isExerciseTypeCorrect(project));
    }

    @Test
    public void exerciseIsCorrectTypeIfItContainsTestthatFile() {
        Path testCasesRoot = TestUtils.getPath(getClass(), "recognition_test_cases");
        Path project = testCasesRoot.resolve("testthat_folder")
                                    .resolve("tests");

        assertTrue(Files.exists(project.resolve("testthat.R")));
    }

    @Test
    public void getStudentFilePolicyReturnsRStudentFilePolicy() {
        StudentFilePolicy policy = plugin.getStudentFilePolicy(Paths.get(""));

        assertTrue(policy instanceof RStudentFilePolicy);
    }
}
