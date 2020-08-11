package onedio;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javafx.util.Pair;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Runner {

    List<Question> runner(String address) {
        Document doc;
        try {
            doc = Jsoup.connect(address).get();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Siteye baglanamadi, web adresinin dogru oldugunu kontrol edin.", e);
        }

        Elements questionSections = doc.select("section.entry.question");
        Elements outcomeSections = doc.select("section[data-question-result]");

        Map<String, TestOutcome> possibleOutcomes = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        outcomeSections.iterator(),
                        Spliterator.CONCURRENT), true)
                .map(section -> {
                    String key = section.attr("data-question-result");
                    String title = section.select("h2").first().text();
                    return new Pair<>(key, new TestOutcome(key,
                            title.replace("&quot;", "").replace("&nbsp;", "")));
                }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        questionSections.iterator(),
                        Spliterator.ORDERED), false)
                .map(section -> new Question(section, possibleOutcomes))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("s", "site", true, "Testin oldugu web sitesinin adresi");
        CommandLine parsedArgs = new DefaultParser().parse(options, args);
        if (!parsedArgs.hasOption("s")) {
            throw new IllegalArgumentException(
                    "Testin oldugu web sitesinin adresi girilmemis, lutfen adresi girin");
        }

        List<Question> questions = new Runner().runner(parsedArgs.getOptionValue("s"));

        StringBuilder s = new StringBuilder();

        for (Question question : questions) {
            s.append("Soru ").append(question.questionText).append("\r\n");
            for (int j = 0; j < question.answers.size(); j++) {
                s.append("\tCevap : ").append(question.answers.get(j).getAnswerText()).append(":")
                        .append("\r\n");

                List<Pair<String, Integer>> r = question.answers.get(j)
                        .givePoints();

                for (Pair<String, Integer> data : r) {
                    s.append("\t\t").append(data.getKey()).append("\t")
                            .append((data.getValue() > 0) ? "+"
                                    : "").append(data.getValue().toString()).append("\r\n");
                }
            }
        }
        System.out.println(s.toString());
    }
}
