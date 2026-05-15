package ifmo.se.lab1app.camunda;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class AviasalesProcessBpmnTest {

    @Test
    void shouldDeclareExecutableCamundaProcessFormsMessagesAndExternalTopics() throws Exception {
        Document document = parseProcess();
        Element process = (Element) document.getElementsByTagNameNS(
                "http://www.omg.org/spec/BPMN/20100524/MODEL",
                "process"
        ).item(0);

        assertThat(process.getAttribute("id")).isEqualTo("aviasales-campaign-launch");
        assertThat(process.getAttribute("isExecutable")).isEqualTo("true");
        assertThat(document.getElementsByTagNameNS("http://camunda.org/schema/1.0/bpmn", "formData").getLength())
                .isGreaterThanOrEqualTo(8);

        Set<String> topics = elements(document, "serviceTask").stream()
                .map(element -> element.getAttributeNS("http://camunda.org/schema/1.0/bpmn", "topic"))
                .filter(topic -> !topic.isBlank())
                .collect(Collectors.toSet());
        assertThat(topics).contains(
                "campaign-create-draft",
                "campaign-configure",
                "creative-schedule-upload",
                "moderation-decision",
                "payment-succeeded",
                "payment-canceled",
                "payment-retry-invoice",
                "campaign-activate",
                "campaign-freeze",
                "campaign-resume",
                "campaign-stop"
        );

        Set<String> messages = elements(document, "message").stream()
                .map(element -> element.getAttribute("name"))
                .collect(Collectors.toSet());
        assertThat(messages).contains(
                "CreativeUploadFinished",
                "PaymentSucceeded",
                "PaymentCanceled",
                "PauseRequested"
        );
    }

    private Document parseProcess() throws Exception {
        Path rootRelative = Path.of("docs", "aviasales-process.bpmn");
        Path moduleRelative = Path.of("..", "docs", "aviasales-process.bpmn");
        return parse(Files.exists(rootRelative) ? rootRelative : moduleRelative);
    }

    private Document parse(Path path) throws Exception {
        assertThat(Files.exists(path)).isTrue();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private java.util.List<Element> elements(Document document, String localName) {
        var nodes = document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", localName);
        java.util.ArrayList<Element> elements = new java.util.ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            elements.add((Element) nodes.item(index));
        }
        return elements;
    }
}
