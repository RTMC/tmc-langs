package fi.helsinki.cs.tmc.langs.r;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fi.helsinki.cs.tmc.langs.domain.RunResult;
import fi.helsinki.cs.tmc.langs.domain.TestDesc;
import fi.helsinki.cs.tmc.langs.domain.TestResult;
import fi.helsinki.cs.tmc.langs.io.StudentFilePolicy;
import fi.helsinki.cs.tmc.langs.utils.TestUtils;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
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
        File resultsJson = new File(projectPath.toAbsolutePath().toString()
                + "/.results.json");
        resultsJson.delete();
    }

    private void removeAvailablePointsJson(Path projectPath) {
        File availablePointsJson = new File(projectPath.toAbsolutePath().toString()
                + "/.available_points.json");
        availablePointsJson.delete();
    }

    private void testResultAsExpected(TestResult result, boolean successful, String name,
                                      String[] points) {
        assertEquals(successful, result.isSuccessful());
        assertEquals(name, result.getName());
        assertArrayEquals(points, result.points.toArray());
    }

    @Test
    public void testGetTestCommand() {
        String[] command = new String[]{"Rscript"};
        String[] args;
        if (SystemUtils.IS_OS_WINDOWS) {
            args = new String[]{"-e", "\"library('tmcRtestrunner');run_tests()\""};
        } else {
            args = new String[]{"-e", "library(tmcRtestrunner);run_tests()"};
        }
        String[] expectedCommand = ArrayUtils.addAll(command, args);
        Assert.assertArrayEquals(expectedCommand, plugin.getTestCommand());
    }

    @Test
    public void testGetAvailablePointsCommand() {
        String[] command = new String[]{"Rscript"};
        String[] args;
        if (SystemUtils.IS_OS_WINDOWS) {
            args = new String[]{"-e", "\"library('tmcRtestrunner');run_available_points()\""};
        } else {
            args = new String[]{"-e", "library(tmcRtestrunner);run_available_points()"};
        }
        String[] expectedCommand = ArrayUtils.addAll(command, args);
        Assert.assertArrayEquals(expectedCommand, plugin.getAvailablePointsCommand());
    }

    @Test
    public void testGetPluginName() {
        assertEquals("r", plugin.getLanguageName());
    }

    @Test
    public void testScanExercise() {
        plugin.scanExercise(simpleAllTestsPassProject, "main.R");
        File availablePointsJson = new File(simpleAllTestsPassProject.toAbsolutePath().toString()
                + "/.available_points.json");

        assertTrue(availablePointsJson.exists());
    }

    @Test
    public void testScanExerciseInTheWrongPlace() {
        plugin.scanExercise(simpleAllTestsPassProject, "ar.R");
        Path availablePointsJson = simpleAllTestsPassProject.resolve(".available_points.json");
        ImmutableList<TestDesc> re = null;
        try {
            re = new RExerciseDescParser(availablePointsJson).parse();
        } catch (IOException e) {
            System.out.println("Something wrong: " + e.getMessage());
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

        File testThatR = new File(project.toAbsolutePath().toString() + "/testthat.R");
        assertTrue(testThatR.exists());
    }


    @Test
    public void getStudentFilePolicyReturnsRStudentFilePolicy() {
        StudentFilePolicy policy = plugin.getStudentFilePolicy(Paths.get(""));

        assertTrue(policy instanceof RStudentFilePolicy);
    }
}