package onedio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.util.Pair;
import lombok.Getter;

class Answer {

    @Getter
    private String answerText;
    private List<Integer> data;
    private Map<String, Integer> points;
    private Map<String, TestOutcome> results;

    Answer(String answerId, String answerText, Map<String, TestOutcome> results,
            String questionId) {
        this.results = results;
        this.answerText = answerText;
        points = results.keySet().stream()
                .map(testOutcome -> new Pair<>(testOutcome, 0))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        data = Stream.of(answerId.split("\\."))
                .map(this::parseInt36)
                .collect(Collectors.toList());

        computePointsForQuestion(questionId);
    }

    List<Pair<String, Integer>> givePoints() {
        return points.entrySet().stream()
                .filter(entry -> !entry.getValue().equals(0))
                .map(entry -> new Pair<>(results.get(entry.getKey()).getTitle(),
                        points.get(entry.getKey())))
                .collect(Collectors.toList());
    }

    private void computePointsForQuestion(String questionId) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            s.append((char) (data.get(i) ^ (int) (questionId.charAt(i % questionId.length()))));
        }

        Stream.of(s.toString().replace(" ", "").split(",")).forEach(part -> {
            if (part.indexOf('*') == -1) {
                if (points.containsKey(part)) {
                    points.put(part, points.get(part) + 1);
                }
            } else {
                String[] data2 = part.split("\\*");
                if (points.containsKey(data2[1])) {
                    points.put(data2[1], points.get(data2[1]) + new Integer(data2[0]));
                }
            }
        });
    }

    private int parseInt36(String data) {
        int power = 1;
        int result = 0;
        for (int i = data.length() - 1; i >= 0; i--) {
            int current = (data.charAt(i) <= '9' && data.charAt(i) >= '0') ? (data.charAt(i) - '0')
                    : ((data.charAt(i) - 'a') + 10);
            result += current * power;
            power *= 36;
        }
        return result;
    }
}
