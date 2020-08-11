package onedio;

import lombok.Getter;

@Getter
class TestOutcome {

    private String key;
    private String title;

    TestOutcome(String key, String title) {
        this.key = key;
        this.title = title;
    }
}
