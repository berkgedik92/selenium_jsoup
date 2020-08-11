package onedio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Element;

class Question {

    String questionText;
    List<Answer> answers;

    Question(Element element, Map<String, TestOutcome> results) {
        this.answers = new ArrayList<>();

        String questionId = element.attr("data-id");

        questionText = element.select("h2").first().text().replace("&quot;", "")
                .replace("&nbsp;", "");

        element.select("div[data-key]").forEach(e -> {
            String key = e.attr("data-key");
            String name = e.select("h5").first().text();
            answers.add(new Answer(key, name, results, questionId));
        });
    }
}
