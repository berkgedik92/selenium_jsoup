package onedio;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.util.Pair;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class MyTest {

    private List<List<Integer>> computeAllAnswerCombinations(
            List<List<WebElement>> answerSectionsForQuestions) {
        List<List<Integer>> answerCombinations = new ArrayList<>();
        ArrayDeque<List<Integer>> stack = new ArrayDeque<>();
        // List of possible answer combinations where each combination specifies
        // answers given for each question
        stack.push(new ArrayList<>());

        while (stack.size() > 0) {
            List<Integer> currentCombination = stack.pop();
            if (currentCombination.size() == answerSectionsForQuestions.size()) {
                answerCombinations.add(currentCombination);
                continue;
            }
            int index = currentCombination.size();
            for (int i = 0; i < answerSectionsForQuestions.get(index).size(); i++) {
                List<Integer> temp = new ArrayList<>(currentCombination);
                temp.add(i);
                stack.push(temp);
            }
        }
        return answerCombinations;
    }

    // Use -Dwebdriver.chrome.driver="/Users/berkgedik/chromedriver"
    @Test
    public void whenSelectingRandomAnswersTheOutcomeShouldBeEqualToExpectedOutcome()
            throws InterruptedException {

        String urlForTest = "https://onedio.com/haber/universitede-seni-mutlu-edecek-bolumu-soyluyoruz-911998";
        int amountOfTest = 30;

        // these options are to execute Chrome in invisible mode
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200",
                "--ignore-certificate-errors");
        WebDriver webDriver = new ChromeDriver(options);
        JavascriptExecutor executor = ((JavascriptExecutor) webDriver);

        webDriver.get(urlForTest);

        List<WebElement> questionSections = webDriver
                .findElements(By.cssSelector("section.entry.question"));
        List<WebElement> outcomeSections = webDriver
                .findElements(By.cssSelector("section[data-question-result]"));
        List<List<WebElement>> answerSectionsForQuestions = questionSections.stream()
                .map(question -> question.findElements(By.cssSelector("div[data-key]")))
                .collect(Collectors.toList());

        List<List<Integer>> allAnswerCombinations = computeAllAnswerCombinations(
                answerSectionsForQuestions);

        // Pick "amountOfTest" random combinations of answers
        Collections.shuffle(allAnswerCombinations);
        allAnswerCombinations = allAnswerCombinations.subList(0, amountOfTest);

        List<Question> givenQuestions = new Runner().runner(urlForTest);

        // Question -> Answer -> For each answer, how much point this answer will provide for each category
        List<List<Map<String, Integer>>> pointTable = givenQuestions.stream()
                .map(question -> question.answers.stream()
                        .map(answer -> answer.givePoints().stream()
                                .collect(Collectors.toMap(Pair::getKey, Pair::getValue)))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        for (int index = 0; index < amountOfTest; index++) {
            List<Integer> combinationToTest = allAnswerCombinations.get(index);
            Map<String, Integer> pointsForEachCandidateOutput = new HashMap<>();

            // Hide all outcome sections if they are visible
            for (WebElement outcomeSection : outcomeSections) {
                String candidateTestOutcome = outcomeSection.findElement(By.cssSelector("h2"))
                        .getAttribute("innerHTML");
                pointsForEachCandidateOutput.put(candidateTestOutcome, 0);
                executor.executeScript("arguments[0].setAttribute('style', '')", outcomeSection);
            }

            // For each question, click to the answer that is determined by "combinationToTest"
            for (int questionIndex = 0; questionIndex < combinationToTest.size(); questionIndex++) {
                executor.executeScript("arguments[0].click();",
                        answerSectionsForQuestions.get(questionIndex)
                                .get(combinationToTest.get(questionIndex)));

                Map<String, Integer> pointsForCandidateOutputs = pointTable.get(questionIndex)
                        .get(combinationToTest.get(questionIndex));
                for (String candidateOutput : pointsForCandidateOutputs.keySet()) {
                    pointsForEachCandidateOutput
                            .put(candidateOutput, pointsForEachCandidateOutput.get(candidateOutput)
                                    + pointsForCandidateOutputs.get(candidateOutput));
                }
            }

            Set<String> outputsWithHighestPoint = new HashSet<>();
            int maximumPoint = -1;

            for (String candidateOutput : pointsForEachCandidateOutput.keySet()) {
                if (pointsForEachCandidateOutput.get(candidateOutput) == maximumPoint) {
                    outputsWithHighestPoint.add(candidateOutput);
                } else if (pointsForEachCandidateOutput.get(candidateOutput) > maximumPoint) {
                    outputsWithHighestPoint = new HashSet<>();
                    outputsWithHighestPoint.add(candidateOutput);
                    maximumPoint = pointsForEachCandidateOutput.get(candidateOutput);
                }
            }

            while (true) {
                Thread.sleep(500);
                // Test outcome given by the site after we answer all questions
                Set<String> outcome = new HashSet<>();
                for (WebElement outcomeSection : outcomeSections) {
                    if (outcomeSection.getAttribute("style").replace(" ", "")
                            .equals("display:block;")) {
                        outcome.add(outcomeSection.findElement(By.cssSelector("h2"))
                                .getText());
                    }
                }

                // Intersection between the outcomes given by the site and our algorithm
                Set<String> intersection = new HashSet<>(outputsWithHighestPoint);
                intersection.retainAll(outcome);
                if (intersection.size() > 0) {
                    // We passed the test
                    break;
                }
            }
            System.out.println("Test " + (index + 1) + "/30");
        }
    }
}
